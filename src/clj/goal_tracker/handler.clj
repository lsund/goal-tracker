(ns goal-tracker.handler
  "Namespace for handling routes"
  (:require [clj-time.core :as time]
            [compojure.core :refer [GET POST routes]]
            [compojure.route :as route]
            [goal-tracker.db.create :as create]
            [goal-tracker.db.delete :as delete]
            [goal-tracker.db.read :as read]
            [goal-tracker.db.update :as update]
            [goal-tracker.render :as render]
            [goal-tracker.render.goal :as render.goal]
            [goal-tracker.render.books :as render.books]
            [goal-tracker.util :as util]
            [medley.core :refer [filter-vals map-vals]]
            [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.util.response :refer [redirect]]))

(defn- all-tasks [db iterationid goalid]
  (map-vals (partial sort-by :sequence)
            {:tasks (read/all-incremental-tasks db goalid iterationid)}))

(defn- goal-handler [{:keys [db] :as config} id iterationid]
  (let [current-iteration (if iterationid
                            (read/row db :iteration (util/parse-int iterationid))
                            (read/iteration db))
        goalid (util/parse-int id)]
    (render.goal/layout config
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

(defn nudge-at [db table id url]
  (case (keyword table)
    :task (update/increment db :task :current (util/parse-int id))
    :book (update/toggle-done db :book (util/parse-int id))
    :subgoal (update/toggle-done db :subgoal (util/parse-int id)))
  (redirect url))

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
                         :goals (read/goals-with-estimates db (util/parse-int (:id iteration)))
                         :subgoals (read/subgoals db)
                         :done-goal-ids (read/done-goal-ids db (:id iteration))})))
   (GET "/goal" [id iterationid]
        (goal-handler config id iterationid))
   (GET "/subgoals" []
        (render/subgoals config {:subgoals (read/subgoals db)}))
   (GET "/books" [iterationid]
        (render.books/layout config
                             {:iteration (read/all db :iteration)
                              :books (sort-by :done (read/all db :book))
                              :iterationid iterationid}))
   (POST "/add/:kind" [kind desc deadline goalid url thisiteration deadline]
         (case (keyword kind)
           :book (create/row db :book {:title desc
                                       :done false})
           :goal (create/row db :goal {:description desc
                                       :deadline (util/->sqldate deadline)})
           :actionitem (create/row db :actionitem {:goalid (util/parse-int goalid)
                                                   :description desc})
           :subgoal (create/row db :subgoal {:goalid (util/parse-int goalid)
                                             :deadline (util/->localdate deadline)
                                             :description desc
                                             :thisiteration (util/checked->bool thisiteration)}))
         (if url
           (redirect url)
           (redirect "/")))
   (POST "/remove/:kind" [kind id taskid url]
         (when (= (keyword kind) :donetaskentry)
           (update/decrement db :task :current (util/parse-int taskid)))
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
         (nudge-at db table id url))
   (POST "/sort/:op/:table" [op table id goalid url]
         (update/tweak-sequence db (keyword table) (util/parse-int id) (keyword op))
         (redirect url))
   (POST "/prioritize/:op/:table" [op table id goalid]
         (update/tweak-priority db (keyword table) (util/parse-int id) (keyword op))
         (redirect (str "/goal?id=" goalid)))
   (POST "/update/goal-desc" [desc url id]
         (update/row db :goal {:description desc} (util/parse-int id))
         (redirect url))
   (route/resources "/")
   (route/not-found render/not-found)))

(defn new-handler
  [config]
  (-> (app-routes config)
      (wrap-keyword-params)
      (wrap-params)
      (wrap-defaults
       (-> site-defaults (assoc-in [:security :anti-forgery] false)))))
