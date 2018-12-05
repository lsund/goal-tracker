(ns helper.db.core
  "Namespace for database interfacing"
  (:require [com.stuartsierra.component :as c]
            [helper.config :as config]))

(defn pg-db [config]
  {:dbtype "postgresql"
   :dbname (:name config)
   :user "postgres"})

(def pg-db-val (pg-db {:name "helper"}))

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
