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
   [helper.db.read :as read]
   [helper.db.update :as update]
   [helper.db.create :as create]
   [helper.util :as util]
   [helper.render :as render]))

(defn ensure-current-iteration [handler db]
  (fn [req]
    (when-not (read/current-iteration db)
      (let [now (time/now)
            first-day (time/first-day-of-the-month now)
            last-day (time/last-day-of-the-month now)]
        (create/row db :iteration {:startdate (util/->sqldate first-day)
                                   :enddate (util/->sqldate last-day)})))
    (handler req)))

(defn- all-tasks [db iterationid goalid]
  (map-vals (partial sort-by :sequence)
            {:incremental-tasks (read/all-where db
                                                 :incrementaltask
                                                 (str "iterationid="
                                                      iterationid
                                                      " and goalid="
                                                      goalid))
             :checked-tasks (read/all-where db
                                             :checkedtask
                                             (str "iterationid="
                                                  iterationid
                                                  " and goalid="
                                                  goalid))
             :reading-tasks (read/all-where db :readingtask (str "iterationid="
                                                                  iterationid
                                                                  " and goalid="
                                                                  goalid))}))

(defn- all-logs [db iterationid goalid]
  (let [params {:db db
                :goalid goalid
                :iterationid iterationid}]
    {:incremental-task-log (read/task-log (assoc params :kind :incremental))
     :checked-task-log (read/task-log (assoc params :kind :checked))
     :reading-task-log (read/task-log (assoc params :kind :reading))}))

(defn- app-routes
  [{:keys [db] :as config}]
  (routes
   (GET "/" [iteration-id]
        (render/index config
                      (read/all db :goal)
                      (read/done-goal-ids db (if iteration-id
                                                (util/parse-int iteration-id)
                                                (:id (read/current-iteration db))))))
   (GET "/goal" [id]
        (let [current-iteration (read/current-iteration db)
              goalid (util/parse-int id)]
          (render/goal config
                       (merge (all-tasks db (:id current-iteration) goalid)
                              (all-logs db (:id current-iteration) goalid)
                              {:goal (read/row db :goal (util/parse-int id))
                               :current-iteration current-iteration
                               :actionitems (read/all-where db
                                                             :actionitem
                                                             (str "goalid=" goalid))
                               :books (read/all db :book)}))))
   (GET "/books" []
        (render/books config (read/all db :book)))
   (POST "/add/:kind" [kind desc deadline goalid url]
         (case (keyword kind)
           :book (create/row db :book {:title desc
                                       :done false})
           :goal (create/row db :goal {:description desc
                                       :deadline (util/->sqldate deadline)})
           :actionitem (create/row db :actionitem {:goalid (util/parse-int goalid)
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
           (create/row db
                       (keyword kind)
                       (merge commons extras)))
         (redirect url))
   (POST "/increment-task" [id goalid]
         (update/increment db :incrementaltask :current (util/parse-int id))
         (redirect (str "/goal?id=" goalid)))
   (POST "/toggle-done/:table" [table id url]
         (update/toggle-done db (keyword table) (util/parse-int id))
         (redirect url))
   (POST "/sort/:op/:table" [op table id goalid]
         (update/tweak-sequence db (keyword table) (util/parse-int id) (keyword op))
         (redirect (str "/goal?id=" goalid)))
   (POST "/prioritise/:op/:table" [op table id goalid]
         (update/tweak-priority db (keyword table) (util/parse-int id) (keyword op))
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
