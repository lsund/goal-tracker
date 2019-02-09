(ns goal-tracker.render.goal
  (:require [goal-tracker.render :as render]
            [hiccup.form :refer [form-to]]
            [goal-tracker.util :as util]
            [goal-tracker.html :as html]))

(defn add-subgoal [{:keys [goal current-iteration]}]
  [:div
   [:h3.center "Subgoals"]
   (form-to [:post "/add/subgoal"]
            [:input {:type :text
                     :name "desc"
                     :placeholder "Description"
                     :required "true"}]
            [:input {:type :date :name "deadline" :required "true"}]
            [:input {:type :hidden :name "goalid" :value (:id goal)}]
            [:input {:type :hidden
                     :name "url"
                     :value (util/make-query-url "/goal"
                                                 {:id (:id goal)
                                                  :iterationid (:id current-iteration)})}])])

(defn add-action-item [{:keys [goal current-iteration]}]
  [:div
   [:h3.center "Action Items"]
   (form-to [:post "/add/actionitem"]
            [:input {:type :text
                     :name "desc"
                     :placeholder "Action Item Description"
                     :required "true"}]
            [:input {:type :hidden :name "goalid" :value (:id goal)}]
            [:input {:type :hidden
                     :name "url"
                     :value (util/make-query-url "/goal"
                                                 {:id (:id goal)
                                                  :iterationid (:id current-iteration)})}])])

(defn list-subgoals [{:keys [current-iteration subgoals goal]}]
  [:div
   [:table
    [:thead
     [:tr
      [:th "Description"]
      [:th "Deadline"]
      [:th "Remove"]]]
    [:tbody
     (for [subgoal subgoals]
       [:tr
        [:td (:description subgoal)]
        [:td (:deadline subgoal)]
        [:td (form-to [:post "/remove/subgoal"]
                      [:input {:type :hidden :name "id" :value (:id subgoal)}]
                      [:input {:type :hidden
                               :name "url"
                               :value (util/make-query-url "/goal"
                                                           {:id (:id goal)
                                                            :iterationid (:id current-iteration)})}]
                      [:input {:type :submit :value "x"}])]])]]])

(defn list-action-items [{:keys [goal current-iteration actionitems]}]
  [:table
   [:thead
    [:tr
     [:th "Name"]
     [:th "Remove"]]]
   [:tbody
    (for [item actionitems]
      [:tr
       [:td  (str (:description item))]
       [:td  (form-to [:post "/remove/actionitem"]
                      [:input {:type :hidden :name "id" :value (:id item)}]
                      [:input {:type :hidden
                               :name "url"
                               :value (util/make-query-url "/goal"
                                                           {:id (:id goal)
                                                            :iterationid (:id current-iteration)})}]
                      [:input {:type :submit :value "x"}])]])]])

(defn add-task [{:keys [goal current-iteration actionitems]}]
  [:div
   [:h3.center "Tasks"]
   (form-to [:post "/add-task"]
            [:select {:name "actionitemid"}
             (for [item actionitems]
               [:option {:value (:id item)} (:description item)])]
            [:input {:type :hidden :name "goalid" :value (:id goal)}]
            [:input {:type :hidden :name "iterationid" :value (:id current-iteration)}]
            [:input {:type :text
                     :name "desc"
                     :placeholder "Description"
                     :required "true"}]
            [:input {:type :text
                     :name "estimate"
                     :placeholder "Time per increment"}]
            [:input {:type :hidden
                     :name "url"
                     :value (util/make-query-url "/goal"
                                                 {:id (:id goal)
                                                  :iterationid (:id current-iteration)})}]
            [:input.number-input {:type :number :name "target" :value 1 :required "true"}]
            [:input {:type :text
                     :name "unit"
                     :placeholder "Unit"
                     :required "true"}]
            [:input.hidden {:type :submit}]
            [:input {:type :hidden :name "current" :value "0"}])])

(defn- current-url [params]
  (util/make-query-url "/goal"
                       {:id (get-in params [:goal :id])
                        :iterationid (get-in params [:current-iteration :id])}))

(defn task-log [params]
  [:table
   [:thead
    [:tr
     [:th "Description"]
     [:th "Remove"]]]
   [:tbody
    (for [{:keys [description day id taskid]} (:task-log params)]
      [:tr
       [:td (str description " done on " day)]
       [:td
        (form-to [:post "/remove/donetaskentry"]
                 [:input {:type :hidden :name "id" :value id}]
                 [:input {:type :hidden :name "taskid" :value taskid}]
                 [:input {:type :hidden
                          :name "url"
                          :value (current-url params)}]
                 [:input {:type :submit :value "x"}])]])]])

(defn layout [config params]
  (render/layout config
                 (assoc params :url "/goal")
                 "Goal"
                 [:div
                  [:h2 (get-in params [:goal :description])]
                  [:h3 (util/format-time (:total-estimate params))]
                  [:div.mui-panel
                   (add-subgoal params)
                   (list-subgoals params)]
                  [:div.mui-panel
                   (add-action-item params)
                   (list-action-items params)]
                  [:div.mui-panel
                   (add-task params)
                   (html/table (:goal params)
                               (get-in params [:current-iteration :id])
                               (:tasks params)
                               (fn [task] (<= (:target task) (:current task)))
                               :current
                               :target
                               :unit)]
                  [:div.mui-panel
                   [:h3.center "Task Log"]
                   (task-log params)]]))
