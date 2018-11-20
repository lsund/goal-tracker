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

(defn ensure-current-iteration [handler db]
  (fn [req]
    (when-not (db/current-iteration db)
      (let [now (time/now)
            first-day (time/first-day-of-the-month now)
            last-day (time/last-day-of-the-month now)]
        (db/add db :iteration {:startdate (util/->sqldate first-day)
                               :enddate (util/->sqldate last-day)})))
    (handler req)))

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
   (POST "/add-incremental-task" [desc target unit goalid iterationid actionitemid]
         (db/add db :incrementaltask {:goalid (util/parse-int goalid)
                                      :actionitemid (util/parse-int actionitemid)
                                      :iterationid (util/parse-int iterationid)
                                      :description desc
                                      :current 0
                                      :target (util/parse-int target)
                                      :unit unit})
         (redirect (str "/goal?id=" goalid)))
   (POST "/add-checked-task" [desc goalid iterationid actionitemid]
         (db/add db :checkedtask {:goalid (util/parse-int goalid)
                                  :actionitemid (util/parse-int actionitemid)
                                  :iterationid (util/parse-int iterationid)
                                  :done false
                                  :description desc})
         (redirect (str "/goal?id=" goalid)))
   #_(POST "/add-reading-task" [desc goalid iterationid actionitemid]
         (db/add db :checkedtask {:goalid (util/parse-int goalid)
                                  :actionitemid (util/parse-int actionitemid)
                                  :iterationid (util/parse-int iterationid)
                                  :done false
                                  :description desc})
         (redirect (str "/goal?id=" goalid)))
   (POST "/increment-incremental-task" [id goalid]
         (db/increment db :incrementaltask :current (util/parse-int id))
         (redirect (str "/goal?id=" goalid)))
   (POST "/check-checked-task" [id goalid]
         (db/update db :checkedtask {:done true} (util/parse-int id))
         (redirect (str "/goal?id=" goalid)))
   #_(POST "/update-reading-task" [id goalid]
         (db/update db :checkedtask {:done true} (util/parse-int id))
         (redirect (str "/goal?id=" goalid)))
   (r/resources "/")
   (r/not-found render/not-found)))

(defn new-handler
  [config]
  (-> (app-routes config)
      (ensure-current-iteration (:db config))
      (wrap-keyword-params)
      (wrap-params)
      (wrap-defaults
       (-> site-defaults (assoc-in [:security :anti-forgery] false)))))
