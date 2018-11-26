(ns helper.handler
  "Namespace for handling routes"
  (:require
   [compojure.route :as r]
   [compojure.core :refer [routes GET POST ANY]]
   [clj-time.core :as time]
   [medley.core :refer [filter-vals]]
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

(defn- all-tasks [db iterationid goalid]
  {:incremental-tasks (db/all-where db
                                    :incrementaltask
                                    (str "iterationid="
                                         iterationid
                                         " and goalid="
                                         goalid))
   :checked-tasks (db/all-where db
                                :checkedtask
                                (str "iterationid="
                                     iterationid
                                     " and goalid="
                                     goalid))
   :reading-tasks (db/all-where db :readingtask (str "iterationid="
                                                     iterationid
                                                     " and goalid="
                                                     goalid))})

(defn- all-logs [db iterationid goalid]
  (let [params {:db db
                :goalid goalid
                :iterationid iterationid}]
    {:incremental-task-log (db/task-log (assoc params :kind :incremental))
     :checked-task-log (db/task-log (assoc params :kind :checked))
     :reading-task-log (db/task-log (assoc params :kind :reading))}))

(defn- app-routes
  [{:keys [db] :as config}]
  (routes
   (GET "/" []
        (render/index config
                      (db/all db :goal)
                      (db/done-goal-ids db (:id (db/current-iteration db)))))
   (GET "/goal" [id]
        (let [current-iteration (db/current-iteration db)
              goalid (util/parse-int id)]
          (render/goal config
                       (merge (all-tasks db (:id current-iteration) goalid)
                              (all-logs db (:id current-iteration) goalid)
                              {:goal (db/element db :goal (util/parse-int id))
                               :current-iteration current-iteration
                               :actionitems (db/all-where db
                                                          :actionitem
                                                          (str "goalid=" goalid))
                               :books (db/all db :book)}))))
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
   (POST "/add-task/:kind" [kind desc current target unit goalid iterationid actionitemid page]
         (let [extras (filter-vals some? {:unit unit
                                          :target (util/parse-int target)
                                          :done false
                                          :page (util/parse-int page)
                                          :current (util/parse-int current)})
               commons {:goalid (util/parse-int goalid)
                        :actionitemid (util/parse-int actionitemid)
                        :iterationid (util/parse-int iterationid)
                        :description desc}]
           (db/add db
                   (keyword kind)
                   (merge commons extras)))
         (redirect (str "/goal?id=" goalid)))
   (POST "/increment-task" [id goalid]
         (db/increment db :incrementaltask :current (util/parse-int id))
         (redirect (str "/goal?id=" goalid)))
   (POST "/mark-as-done/:table" [table id goalid]
         (db/mark-as-done db (keyword table) (util/parse-int id))
         (redirect (str "/goal?id=" goalid)))
   (POST "/tweak-priority/:op/:table" [op table id goalid]
         (db/tweak-priority db (keyword table) (util/parse-int id) (keyword op))
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
