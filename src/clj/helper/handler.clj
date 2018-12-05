(ns helper.handler
  "Namespace for handling routes"
  (:require
   [compojure.route :as r]
   [compojure.core :refer [routes GET POST ANY]]
   [clj-time.core :as time]
   [medley.core :refer [filter-vals map-vals]]
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
  (map-vals (partial sort-by :sequence)
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
                                                               goalid))}))

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
   (GET "/" [iteration-id]
        (render/index config
                      (db/all db :goal)
                      (db/done-goal-ids db (if iteration-id
                                             (util/parse-int iteration-id)
                                             (:id (db/current-iteration db))))))
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
   (POST "/add/:kind" [kind desc deadline goalid url]
         (case kind
           :book (db/add db :book {:title desc
                                   :done false})
           :goal (db/add db :goal {:description desc
                                   :deadline (util/->sqldate deadline)})
           :actionitem (db/add db :actionitem {:goalid (util/parse-int goalid)
                                               :description desc}))
         (if url
           (redirect url)
           (redirect "/")))
   (POST "/add-task/:kind" [kind desc current target unit goalid iterationid actionitemid url]
         (let [extras (filter-vals some? {:unit unit
                                          :target (util/parse-int target)
                                          :done false
                                          :current (util/parse-int current)})
               commons {:goalid (util/parse-int goalid)
                        :actionitemid (util/parse-int actionitemid)
                        :iterationid (util/parse-int iterationid)
                        :description desc}]
           (db/add db
                   (keyword kind)
                   (merge commons extras)))
         (redirect url))
   (POST "/increment-task" [id goalid]
         (db/increment db :incrementaltask :current (util/parse-int id))
         (redirect (str "/goal?id=" goalid)))
   (POST "/toggle-done/:table" [table id goalid url]
         (db/toggle-done db (keyword table) (util/parse-int id))
         (redirect url))
   (POST "/tweak-sequence/:op/:table" [op table id goalid]
         (db/tweak-sequence db (keyword table) (util/parse-int id) (keyword op))
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
