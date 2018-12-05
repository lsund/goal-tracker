(ns helper.db
  "Namespace for database interfacing"
  (:require [clojure.java.jdbc :as j]
            [clj-time.core :as time]
            [com.stuartsierra.component :as c]
            [taoensso.timbre :as timbre]
            [helper.util :as util]
            [helper.config :as config]
            [slingshot.slingshot :refer [throw+]]))


(defn pg-db [config]
  {:dbtype "postgresql"
   :dbname (:name config)
   :user "postgres"})

(def pg-db-val (pg-db {:name "helper"}))

(defrecord Db [db db-config]
  c/Lifecycle

  (start [component]
    (println ";; [Db] Starting database")
    (assoc component :db (pg-db db-config)))

  (stop [component]
    (println ";; [Db] Stopping database")
    component))

(defn new-db
  [config]
  (map->Db {:db-config config}))

(defn add [db table row]
  (j/insert! db table row))

(defn all [db table]
  (j/query db [(str "SELECT * FROM " (name table))]))

(defn update [db table update-map id]
  (j/update! db table update-map ["id=?" id]))

(defn element [db table id]
  (first (j/query db [(str "SELECT * FROM " (name table) " WHERE id=?") id])))

(defn all [db table]
  (j/query db [(str "SELECT * FROM " (name table))]))

(defn all-where [db table clause]
  (j/query db [(str "SELECT * FROM " (name table) " WHERE " clause)]))

(defn increment [db table column id]
  (j/execute! db
              [(str "UPDATE "
                    (name table)
                    " SET "
                    (name column)
                    " = "
                    (name column)
                    " + 1 WHERE id=?") id])
  (add db :taskupdate {:taskid id :tasktype 1 :day (util/->sqldate (time/now))}))

(defn toggle-done [db table id]
  (update db (keyword table) {:done true} id)
  (let [tasktype (case table :checkedtask 2 :readingtask 3)]
    (add db :taskupdate {:taskid id
                         :tasktype tasktype
                         :day (util/->sqldate (time/now))})))

(defn current-iteration [db]
  (let [now (util/->sqldate (time/now))]
    (if-let [iteration
             (first (j/query db ["select * from iteration where id = 17"])) ;; TODO remove
             #_(first (j/query db
                             [(str "SELECT * FROM iteration where ? >= startdate and ? <= enddate") now now]))]
      iteration
      (throw+ {:type ::no-current-iteration}))))

(defn done-goal-ids [db iterationid]
  (map :id (j/query db
                    ["SELECT id FROM goal WHERE id NOT IN (
                     (SELECT distinct(goalid)
                      FROM checkedtask
                      WHERE done = false AND iterationid = ?)
                     UNION
                     (SELECT distinct(goalid)
                      FROM incrementaltask
                      WHERE current < target AND iterationid = ?)
                     UNION
                     (SELECT distinct(goalid)
                      FROM readingtask
                      WHERE done = false AND iterationid = ?));"
                     iterationid
                     iterationid
                     iterationid])))

(defn tweak-priority [db table id op]
  (let [f (if (= op :up) util/pred util/succ)
        nxt (some-> db
                    (j/query [(str "SELECT priority from " (name table) " where id = ?") id])
                    first
                    :priority
                    first
                    f)]
    (if-not nxt
      (case op
        :up (update db table {:priority "A"} id)
        :down (update db table {:priority "C"} id))
      (when (and nxt (apply <= (map int [\A nxt \C])))
        (update db table {:priority (str nxt)} id)))))

(defn tweak-sequence [db table id op]
  (let [f (if (= op :up) util/pred util/succ)
        nxt (some-> db
                    (j/query [(str "SELECT sequence from " (name table) " where id = ?") id])
                    first
                    :sequence
                    f)]
    (when (and nxt (< 0 nxt))
      (update db table {:sequence nxt} id))))

(defmulti task-log
  (fn [params] (:kind params)))

(defmethod task-log :incremental [{:keys [db iterationid goalid]}]
  (j/query db
           ["SELECT taskupdate.day, incrementaltask.description
             FROM taskupdate
             INNER JOIN incrementaltask ON taskupdate.taskid = incrementaltask.id
             WHERE taskupdate.tasktype = 1
             AND taskupdate.taskid IN
               (SELECT id FROM incrementaltask WHERE iterationid = ? AND goalid = ?);"
            iterationid
            goalid]))

(defmethod task-log :checked [{:keys [db goalid iterationid]}]
  (j/query db
           ["SELECT taskupdate.day, checkedtask.description
             FROM taskupdate
             INNER JOIN checkedtask ON taskupdate.taskid = checkedtask.id
             WHERE taskupdate.tasktype = 2
             AND taskupdate.taskid IN
               (SELECT id FROM checkedtask WHERE iterationid = ? AND goalid = ?);"
            iterationid
            goalid]))

(defmethod task-log :reading [{:keys [db goalid iterationid]}]
  (j/query db
           ["SELECT taskupdate.day, book.title
             FROM taskupdate
             INNER JOIN readingtask
             ON taskupdate.taskid = readingtask.id
             INNER JOIN book on book.id = readingtask.bookid
             WHERE taskupdate.tasktype = 3
             AND taskupdate.taskid IN
              (SELECT id FROM readingtask WHERE iterationid = ? AND goalid = ?)"
            iterationid
            goalid]))
