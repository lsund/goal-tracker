(ns helper.db.create
  (:require [clojure.java.jdbc :as j]
            [helper.util :as util]
            [clj-time.core :as time]))

(def ^:private taskname->tasktype
  {:incrementaltask 1
   :checkedtask 2
   :readingtask 3})

(defn row [db table row]
  (j/insert! db table row))

(defn done-task-entry [db table id]
  (row db :donetaskentry {:taskid id
                          :tasktype (taskname->tasktype table)
                          :day (util/->sqldate (time/now))}))
