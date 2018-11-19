(ns helper.render
  "Namespace for rendering hiccup"
  (:require
   [helper.db :as db]
   [taoensso.timbre :as logging]
   [hiccup.form :refer [form-to]]
   [hiccup.page :refer [html5 include-css include-js]]
   [helper.util :as util]
   [helper.html :as html]))


(defn goal [{:keys [db] :as config} {:keys [id description]}]
  (html5
   [:head
    [:title "Helper"]]
   [:body
    [:h1 description]
    (form-to [:post "/add-action-item"]
             [:input {:name "desc" :type :text :placeholder "Action Item Description" :required "true"}]
             [:input {:name "goalid" :type :hidden :value id}])
    [:h2 "Current Action Items"]
    [:ul
     (for [item (db/all-where db :actionitem (str "goalid=" id))]
       [:li (str (:description item))])]
    (apply include-js (:javascripts config))
    (apply include-css (:styles config))]))

(defn index [{:keys [db] :as config}]
  (html5
   [:head
    [:title "Helper"]]
   [:body
    [:h1 "Helper"]
    [:p "This is a tool for personal development"]
    (form-to [:post "/add-goal"]
             [:input {:name "desc" :type :text :placeholder "Goal Description" :required "true"}]
             [:input {:type :date :name "deadline" :required "true"}])
    [:h2 "Current Goals"]
    [:ul
     (for [goal (db/all db :goal)]
       [:li [:a {:href (str "/goal?id=" (:id goal))} (str (:description goal) " by " (:deadline goal))]])]
    (if-let [{:keys [startdate enddate]} (db/current-iteration db)]
      [:h3 (str "Current iteration: " startdate " to " enddate)]
      [:h3 (str "No current iteration")])
    (form-to [:post "/add-timed-task"]
             ;; TODO add hidden inputs for goalid and iterationid here
             ;; Iterationid should be the current iteration
             ;; Goalid should be user input
             [:input {:name "desc" :type :text :placeholder "Task Description" :required "true"}]
             [:input {:type :number :name "target" :value 0 :required "true"}]
             [:input {:type :text :name "unit" :placeholder "Unit" :required "true"}]
             [:input.hidden {:type :submit}])
    [:table
     [:thead
      [:tr
       [:th "Description"]
       [:th "Current"]
       [:th "Target"]
       [:th "Unit"]
       [:th "Increment"]]]
     [:tbody
      (for [{:keys [id description current target unit]}
            (db/all-where db :timedtask (str "iterationid=" (:id (db/current-iteration db))))]
        [:tr {:class (if (<= target current) "green" "")}
         [:td description]
         [:td current]
         [:td target]
         [:td unit]
         [:td (form-to [:post "/increment-timed-task"]
                       [:input {:type :submit :value "+"}]
                       [:input {:type :hidden :name "id" :value id}])]])]]
    (apply include-js (:javascripts config))
    (apply include-css (:styles config))]))

(def not-found (html5 "not found"))
