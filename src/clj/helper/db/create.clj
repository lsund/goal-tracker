(ns helper.db.create
  (:require [clojure.java.jdbc :as j]
            [helper.util :as util]
            [clj-time.core :as time]))

(def taskname->tasktype
  {:incrementaltask 1
   :checkedtask 2
   :readingtask 3})

(defn row [db table row]
  (j/insert! db table row))

(defn taskupdate [db table id]
  (row db :taskupdate {:taskid id
                       :tasktype (taskname->tasktype table)
                       :day (util/->sqldate (time/now))}))
