(ns goal-tracker.db.delete
  (:require [clojure.java.jdbc :as j]
            [goal-tracker.db.read :as read]))

(defn by-id [db table id]
  (j/delete! db table ["id=?" id]))

(defn done-task-entry [db taskid]
  (by-id db :taskid (-> (read/all-where db :donetaskentry (str "taskid=" taskid))
                        first
                        :id)))
