(ns helper.core
  "Namespace that defines the system of components."
  (:require
   [com.stuartsierra.component :as c]
   [helper.app :as app]
   [helper.server :as server]
   [helper.db.core :as db]))

(defn new-system
  [config]
  (c/system-map :server (c/using (server/new-server (:server config))
                                 [:app])
                :app (c/using (app/new-app (:app config))
                              [:db])
                :db (c/using (db/new-db (:db config))
                             [])))
