(ns helper.handler
  "Namespace for handling routes"
  (:require
   [compojure.route :as r]
   [compojure.core :refer [routes GET POST]]
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
   [helper.db.delete :as delete]
   [helper.util :as util]
   [helper.render :as render]))

(defn ensure-current-iteration [handler db]
  (fn [req]
    (when-not (read/iteration db)
      (let [now (time/now)
            first-day (time/first-day-of-the-month now)
            last-day (time/last-day-of-the-month now)]
        (create/row db :iteration {:startdate (util/->sqldate first-day)
                                   :enddate (util/->sqldate last-day)})))
    (handler req)))

(defn- all-tasks [db iterationid goalid]
  (map-vals (partial sort-by :sequence)
            {:tasks (read/all-incremental-tasks db goalid iterationid)}))

(defn- goal-handler [{:keys [db] :as config} id iterationid]
  (let [current-iteration (if iterationid
                            (read/row db :iteration (util/parse-int iterationid))
                            (read/iteration db))
        goalid (util/parse-int id)]
    (render/goal config
                 (merge (all-tasks db (:id current-iteration) goalid)
                        {:task-log (read/task-log db (:id current-iteration) goalid)}
                        {:goal (read/row db :goal (util/parse-int id))
                         :iterations (read/all db :iteration)
                         :current-iteration current-iteration
                         :total-estimate (read/total-estimate db
                                                              (util/parse-int id)
                                                              (util/parse-int iterationid))
                         :actionitems (read/all-where db
                                                      :actionitem
                                                      (str "goalid=" goalid))
                         :subgoals (read/all-where db
                                                   :subgoal
                                                   (str "goalid=" goalid))
                         :books (read/all db :book)}))))

(defn- app-routes
  [{:keys [db] :as config}]
  (routes
   (GET "/" [iterationid]
        (let [iteration (if iterationid
                          (read/row db :iteration (util/parse-int iterationid))
                          (read/iteration db))]
          (render/index config
                        {:iterations (read/all db :iteration)
                         :iteration iteration
                         :goals (read/goals-with-estimates db (util/parse-int iterationid))
                         :done-goal-ids (read/done-goal-ids db (:id iteration))})))
   (GET "/goal" [id iterationid iterationid]
        (goal-handler config id iterationid))
   (GET "/books" [iterationid]
        (render/books config
                      (read/all db :iteration)
                      iterationid
                      (sort-by :done (read/all db :book))))
   (POST "/add/:kind" [kind desc deadline goalid url thisiteration]
         (case (keyword kind)
           :book (create/row db :book {:title desc
                                       :done false})
           :goal (create/row db :goal {:description desc
                                       :deadline (util/->sqldate deadline)})
           :actionitem (create/row db :actionitem {:goalid (util/parse-int goalid)
                                                   :description desc})
           :subgoal (create/row db :subgoal {:goalid (util/parse-int goalid)
                                             :description desc
                                             :thisiteration (util/checked->bool thisiteration)}))
         (if url
           (redirect url)
           (redirect "/")))
   (POST "/remove/:kind" [kind id url]
         (delete/by-id db (keyword kind) (util/parse-int id))
         (if url
           (redirect url)
           (redirect "/")))
   (POST "/add-task" [estimate desc current target unit goalid iterationid actionitemid url]
         (let [extras (filter-vals some? {:unit unit
                                          :target (util/parse-int target)
                                          :current (util/parse-int current)})
               commons {:goalid (util/parse-int goalid)
                        :actionitemid (util/parse-int actionitemid)
                        :iterationid (util/parse-int iterationid)
                        :description desc
                        :timeestimate estimate}]
           (create/row db :task (merge commons extras)))
         (redirect url))
   (POST "/nudge/at/:table" [table id url]
         (case (keyword table)
           :task (update/increment db :task :current (util/parse-int id))
           :book (update/toggle-done-book db (util/parse-int id)))
         (redirect url))
   (POST "/sort/:op/:table" [op table id goalid]
         (update/tweak-sequence db (keyword table) (util/parse-int id) (keyword op))
         (redirect (str "/goal?id=" goalid)))
   (POST "/prioritize/:op/:table" [op table id goalid]
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
