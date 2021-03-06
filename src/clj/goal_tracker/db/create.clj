(ns goal-tracker.db.create
  (:require [clojure.java.jdbc :as j]
            [goal-tracker.util :as util]
            [clj-time.core :as time]))

(defn row [db table row]
  (j/insert! db table row))

(defn done-task-entry [db id]
  (row db :donetaskentry {:taskid id
                          :tasktype 1
                          :day (util/->sqldate (time/now))}))
