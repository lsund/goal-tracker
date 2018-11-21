(ns helper.handler
  "Namespace for handling routes"
  (:require
   [compojure.route :as r]
   [compojure.core :refer [routes GET POST ANY]]
   [clj-time.core :as time]
   [ring.util.response :refer [redirect]]
   [ring.middleware
    [defaults :refer [site-defaults wrap-defaults]]
    [keyword-params :refer [wrap-keyword-params]]
    [params :refer [wrap-params]]]
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
        (render/index config (db/all db :goal) (db/done-goal-ids db (:id (db/current-iteration db)))))
   (GET "/goal" [id]
        (let [current-iteration (db/current-iteration db)
              goalid (util/parse-int id)]
          (render/goal config
                       {:goal (db/element db :goal (util/parse-int id))
                        :current-iteration current-iteration
                        :actionitems (db/all-where db
                                                   :actionitem
                                                   (str "goalid=" goalid))
                        :books (db/all db :book)
                        :incremental-tasks (db/all-where db
                                                         :incrementaltask
                                                         (str "iterationid="
                                                              (:id current-iteration)
                                                              " and goalid="
                                                              goalid))
                        :checked-tasks (db/all-where db
                                                     :checkedtask
                                                     (str "iterationid="
                                                          (:id current-iteration)
                                                          " and goalid="
                                                          goalid))
                        :reading-tasks (db/all-where db :readingtask (str "iterationid="
                                                                          (:id current-iteration)
                                                                          " and goalid="
                                                                          goalid))})))
   (GET "/books" []
        (render/books config (db/all db :book)))
   (POST "/add-goal" [desc deadline]
         (db/add db :goal {:description desc
                           :deadline (util/->sqldate deadline)})
         (redirect "/"))
   (POST "/add-book" [title]
         (db/add db :book {:title title
                           :done false})
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
   (POST "/add-reading-task" [bookid goalid iterationid actionitemid page]
         (db/add db :readingtask {:goalid (util/parse-int goalid)
                                  :actionitemid (util/parse-int actionitemid)
                                  :iterationid (util/parse-int iterationid)
                                  :bookid (util/parse-int bookid)
                                  :page (util/parse-int page)
                                  :done false})
         (redirect (str "/goal?id=" goalid)))
   (POST "/increment-incremental-task" [id goalid]
         (db/increment db :incrementaltask :current (util/parse-int id))
         (redirect (str "/goal?id=" goalid)))
   (POST "/mark-as-done/:table" [table id goalid]
         (db/update db (keyword table) {:done true} (util/parse-int id))
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
