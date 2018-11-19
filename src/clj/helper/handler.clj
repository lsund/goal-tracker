(ns helper.handler
  "Namespace for handling routes"
  (:require
   [compojure.route :as r]
   [compojure.core :refer [routes GET POST ANY]]


   [ring.util.response :refer [redirect]]
   [ring.middleware
    [defaults :refer [site-defaults wrap-defaults]]
    [keyword-params :refer [wrap-keyword-params]]
    [params :refer [wrap-params]]]

   ;; Logging
   [taoensso.timbre :as logging]
   [taoensso.timbre.appenders.core :as appenders]

   [helper.db :as db]
   [helper.util :as util]
   [helper.render :as render]))


(defn- app-routes
  [{:keys [db] :as config}]
  (routes
   (GET "/" []
        (render/index config))
   (GET "/goal" [id]
        (render/goal config (db/element db :goal (util/parse-int id))))
   (POST "/add-goal" [desc deadline]
         (db/add :goal  {:description desc
                         :deadline (util/->sqldate deadline)})
         (redirect "/"))
   (POST "/add-action-item" [desc goalid]
         (db/add db :actionitem {:goalid (util/parse-int goalid)
                                 :description desc})
         (redirect (str "/goal?id=" goalid)))
   (POST "/add-timed-task" [desc goal unit]
         (db/add db :timedtask {:current 0
                                :goal (util/parse-int goal)
                                :description desc
                                :unit unit})
         (redirect "/"))
   (POST "/increment-timed-task" [id]
         (db/increment db :timedtask :current (util/parse-int id))
         (redirect "/"))
   (r/resources "/")
   (r/not-found render/not-found)))

(defn new-handler
  [config]
  (-> (app-routes config)
      (wrap-keyword-params)
      (wrap-params)
      (wrap-defaults
       (-> site-defaults (assoc-in [:security :anti-forgery] false)))))
