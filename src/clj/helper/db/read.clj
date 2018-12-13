(ns helper.db.read
  (:require [clojure.java.jdbc :as j]
            [clj-time.core :as time]
            [helper.util :as util]
            [helper.db.core :as db]
            [slingshot.slingshot :refer [throw+]]))

(defn row [db table id]
  (first (j/query db [(str "SELECT * FROM " (name table) " WHERE id=?") id])))

(defn value [db table column id]
  (-> db
      (j/query [(str "SELECT " (name column) " from " (name table) " where id = ?") id])
      first
      column))

(defn all [db table]
  (j/query db [(str "SELECT * FROM " (name table))]))

(defn all-where [db table clause]
  (j/query db [(str "SELECT * FROM " (name table) " WHERE " clause)]))

(defn current-iteration [db]
  (let [now (util/->sqldate (time/now))]
    (if-let [iteration
             (first (j/query db ["select * from iteration where id = 17"])) ;; TODO remove
             #_(first (j/query db
                               [(str "SELECT * FROM iteration where ? >= startdate and ? <= enddate") now now]))]
      iteration
      (throw+ {:type ::no-current-iteration}))))

(defn all-reading-tasks [db goalid iterationid]
   (j/query db [ "SELECT readingtask.*, book.title as description
                  FROM readingtask
                  INNER JOIN book
                  ON book.id = readingtask.bookid
                  WHERE iterationid = ? AND goalid = ?" iterationid goalid]))

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
