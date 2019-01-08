(ns helper.db.create
  (:require [clojure.java.jdbc :as j]
            [helper.util :as util]
            [clj-time.core :as time]))

(defn row [db table row]
  (j/insert! db table row))

(defn done-task-entry [db table id]
  (row db :donetaskentry {:taskid id
                          :tasktype 1
                          :day (util/->sqldate (time/now))}))
