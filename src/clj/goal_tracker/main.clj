(ns goal-tracker.main
  "Namespace for running the program once"
  (:require
   [goal-tracker.config :as config]
   [org.httpkit.server :refer [run-server]]
   [compojure.handler :refer [site]]
   [goal-tracker.handler :refer [new-handler]])
  (:gen-class))

(defn -main [& args]
  (let [config (config/load)]
    (run-server (handler/new-handler (:app config)) {:port (get-in config [:server :port])})
    (println "Server up and running")))
