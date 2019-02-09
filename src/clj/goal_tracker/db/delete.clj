(ns goal-tracker.db.delete
  (:require [clojure.java.jdbc :as j]
            [goal-tracker.db.read :as read]))

(defn by-id [db table id]
  (j/delete! db table ["id=?" id]))

(defn by-column-id [db table column id]
  (j/delete! db table [(str (name column) "=" id)]))
