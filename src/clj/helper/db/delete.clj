(ns helper.db.delete
  (:require [clojure.java.jdbc :as j]))

(defn by-id [db table id]
  (j/delete! db table ["id=?" id]))
