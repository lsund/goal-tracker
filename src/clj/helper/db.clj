(ns helper.db
  "Namespace for database interfacing"
  (:require [clojure.java.jdbc :as j]
            [clj-time.core :as time]
            [com.stuartsierra.component :as c]
            [taoensso.timbre :as timbre]
            [helper.util :as util]
            [helper.config :as config]))


(defn pg-db [config]
  {:dbtype "postgresql"
   :dbname (:name config)
   :user "postgres"})

(defrecord Db [db db-config]
  c/Lifecycle

  (start [component]
    (println ";; [Db] Starting database")
    (assoc component :db (pg-db db-config)))

  (stop [component]
    (println ";; [Db] Stopping database")
    component))

(defn new-db
  [config]
  (map->Db {:db-config config}))

(defn add [db table row]
  (j/insert! db table row))

(defn all [db table]
  (j/query db [(str "SELECT * FROM " (name table))]))

(defn element [db table id]
  (first (j/query db [(str "SELECT * FROM " (name table) " WHERE id=?") id])))

(defn all [db table]
  (j/query db [(str "SELECT * FROM " (name table))]))

(defn all-where [db table clause]
  (j/query db [(str "SELECT * FROM " (name table) " WHERE " clause)]))

(defn increment [db table column id]
  (j/execute! db [(str "UPDATE " (name table) " SET " (name column) " = " (name column) " + 1 WHERE id=?") id]))

(defn current-iteration [db]
  (let [now (util/->sqldate (time/now))]
    (first (j/query db
                     [(str "SELECT * FROM iteration where ? > startdate and ? < enddate") now now]))))
