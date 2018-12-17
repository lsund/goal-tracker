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

(defn iteration
  ([db]
   (iteration db 0))
  ([db n]
   (let [now (util/->sqldate (.plusMonths (time/now) n))]
     (when-let [iteration (first (j/query db
                                        [(str "SELECT *
                                              FROM iteration
                                              WHERE ? >= startdate and ? <= enddate")
                                         now
                                         now]))]
       iteration))))

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
           ["SELECT doneTaskEntry.day, incrementaltask.description
             FROM doneTaskEntry
             INNER JOIN incrementaltask ON doneTaskEntry.taskid = incrementaltask.id
             WHERE doneTaskEntry.tasktype = 1
             AND doneTaskEntry.taskid IN
               (SELECT id FROM incrementaltask WHERE iterationid = ? AND goalid = ?);"
            iterationid
            goalid]))

(defmethod task-log :checked [{:keys [db goalid iterationid]}]
  (j/query db
           ["SELECT doneTaskEntry.day, checkedtask.description
             FROM doneTaskEntry
             INNER JOIN checkedtask ON doneTaskEntry.taskid = checkedtask.id
             WHERE doneTaskEntry.tasktype = 2
             AND doneTaskEntry.taskid IN
               (SELECT id FROM checkedtask WHERE iterationid = ? AND goalid = ?);"
            iterationid
            goalid]))

(defmethod task-log :reading [{:keys [db goalid iterationid]}]
  (j/query db
           ["SELECT doneTaskEntry.day, book.title
             FROM doneTaskEntry
             INNER JOIN readingtask
             ON doneTaskEntry.taskid = readingtask.id
             INNER JOIN book on book.id = readingtask.bookid
             WHERE doneTaskEntry.tasktype = 3
             AND doneTaskEntry.taskid IN
              (SELECT id FROM readingtask WHERE iterationid = ? AND goalid = ?)"
            iterationid
            goalid]))
