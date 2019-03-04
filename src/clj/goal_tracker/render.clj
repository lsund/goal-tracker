(ns goal-tracker.render
  "Namespace for rendering views"
  (:require
   [clojure.string :as string]
   [taoensso.timbre :as logging]
   [hiccup.form :refer [form-to]]
   [hiccup.page :refer [html5 include-css include-js]]
   [goal-tracker.util :as util]
   [goal-tracker.html :as html]))

(defn layout
  [config params title content]
  (html5
   [:head
    [:title (str "Goal Tracker - " title)]]
   [:body.mui-container
    (html/navbar params)
    content
    (apply include-css (:styles config))
    (apply include-js (:javascripts config))]))

(defn- url [to params]
  (case to
    :goal (util/make-query-url "/goal" {:iterationid (get-in params [:iteration :id])
                                        :goalid (get-in params [:goal :id])})
    :index (util/make-query-url "/" {:iterationid (get-in params [:iteration :id])})))

(defn subgoal-table [subgoals]
  [:table
   [:thead
    [:tr
     [:th "Goal Sequence Number"]
     [:th "Description"]
     [:th "Deadline"]
     [:th "Toggle Done"]]]
   [:tbody
    (for [subgoal subgoals]
      [:tr
       [:td (:sequence subgoal)]
       [:td (:description subgoal)]
       [:td (:deadline subgoal)]
       [:td
        (form-to [:post "/nudge/at/subgoal"]
                 [:input {:type :hidden :name "id" :value (:id subgoal)}]
                 [:input {:type :hidden :name "url" :value "/"}]
                 [:input {:type :submit :value "x"}])]])]])

(defn subgoals [config params]
  (layout config
          (assoc params :url "/")
          "All Subgoals"
          (subgoal-table (:subgoals params))))

(defn- list-goals [params]
  [:div
   [:h2 "Urgent subgoals"]
   (subgoal-table (take 3 (:subgoals params)))
   [:a {:href "/subgoals"} "Show all"]
   [:h2 "Current Goals"]
   [:table
    [:thead
     [:tr
      [:th "Number"]
      [:th "Description"]
      [:th "Details"]
      [:th "Sort up"]
      [:th "Sort down"]]]
    [:tbody
     (for [goal (sort-by :sequence (:goals params))]
       [:tr {:class (if (some #{(:id goal)} (:done-goal-ids params))
                                "green"
                                "")}
        [:td [:h3 (:sequence goal)]]
        [:td (form-to [:post "/update/goal-desc"]
                      [:input.long-text {:type :text
                                         :name "desc"
                                         :value (:description goal)}]
                      (str "by " (:deadline goal))
                      [:input {:type :hidden :name "id" :value (:id goal)}]
                      [:input {:type :hidden :name "url" :value "/"}])]
        [:td (form-to [:get "/goal"]
                      [:input {:type :hidden :name "id" :value (:id goal)}]
                      [:input {:type :hidden
                               :name "iterationid"
                               :value (get-in params [:iteration :id])}]
                      [:input {:type :submit :value "E"}])]
        [:td (form-to [:post "/sort/up/goal"]
                  [:input {:type :hidden :name "url" :value "/"}]
                  [:input {:type :hidden :name "id" :value (:id goal)}]
                  [:input {:type :submit :value "U"}])]
        [:td (form-to [:post "/sort/down/goal"]
                  [:input {:type :hidden :name "url" :value "/"}]
                  [:input {:type :hidden :name "id" :value (:id goal)}]
                  [:input {:type :submit :value "D"}])]])]]])

(defn index [config params]
  (layout config
          (assoc params :url "/" )
          "Overview"
          [:div
           (list-goals params)
           [:p (str "To fulfill all tasks, a calculated average of "
                    (format "%.1f"
                            (/ (apply + (filter some?
                                                (map (comp :hours :estimate)
                                                     (:goals params))))
                               90.0))
                    " hours per day needs to be spent")]
           [:h2 "Add new goal"]
           (form-to [:post "/add/goal"]
                    [:input {:name "desc"
                             :type :text
                             :placeholder "Goal Description"
                             :required "true"}]
                    [:input {:type :date :name "deadline" :required "true"}])]))

(def not-found (html5 "not found"))
