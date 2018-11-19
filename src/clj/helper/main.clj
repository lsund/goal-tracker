(ns helper.main
  "Namespace for running the program once"
  (:require
   [helper.config :as config]
   [org.httpkit.server :refer [run-server]]
   [compojure.handler :refer [site]]
   [helper.handler :refer [new-handler]])
  (:gen-class))

(defn -main [& args]
  (let [config (config/load)]
    (run-server (handler/new-handler (:app config)) {:port (get-in config [:server :port])})
    (println "Server up and running")))
