(ns helper.db.delete
  (:require [clojure.java.jdbc :as j]
            [helper.db.read :as read]))

(defn by-id [db table id]
  (j/delete! db table ["id=?" id]))

(defn done-task-entry [db taskid]
  (let [taskupdate-id (-> (read/all-where db :taskupdate (str "taskid=" taskid))
                          first
                          :id)]
    (by-id db :taskid taskupdate-id)))
