(ns helper.render
  "Namespace for rendering hiccup"
  (:require
   [helper.db :as db]
   [taoensso.timbre :as logging]
   [hiccup.form :refer [form-to]]
   [hiccup.page :refer [html5 include-css include-js]]
   [helper.util :as util]
   [helper.html :as html]))


(defn goal [{:keys [db] :as config} goal]
  (let [iter (db/current-iteration db)]
    (html5
     [:head
      [:title "Helper"]]
     [:body
      [:h1 (:description goal)]
      [:h2 "Add action item"]
      (form-to [:post "/add-action-item"]
               [:input {:name "desc" :type :text :placeholder "Action Item Description" :required "true"}]
               [:input {:name "goalid" :type :hidden :value (:id goal)}])
      [:h2 "Add new action item"]
      (form-to [:post "/add-incremental-task"]
               [:select {:name "actionitemid"}
                (for [item (db/all db :actionitem)]
                  [:option {:value (:id item)} (:description item)])]
               [:input {:type :hidden :name "goalid" :value (:id goal)}]
               [:input {:type :hidden :name "iterationid" :value (:id iter)}]
               [:input {:type :text :name "desc" :placeholder "Task Description" :required "true"}]
               [:input {:type :number :name "target" :value 0 :required "true"}]
               [:input {:type :text :name "unit" :placeholder "Unit" :required "true"}]
               [:input.hidden {:type :submit}])
      (if iter
        [:h2 (str "Current iteration: " (:startdate iter) " to " (:enddate iter))]
        [:h2 (str "No current iteration")])
      [:ul
       (for [item (db/all-where db :actionitem (str "goalid=" (:id goal)))]
         [:li (str (:description item))])]
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
              (db/all-where db :incrementaltask (str "iterationid="
                                                     (:id (db/current-iteration db))
                                                     " and goalid="
                                                     (:id goal)))]
          [:tr {:class (if (<= target current) "green" "")}
           [:td description]
           [:td current]
           [:td target]
           [:td unit]
           [:td (form-to [:post "/increment-incremental-task"]
                         [:input {:type :submit :value "+"}]
                         [:input {:type :hidden :name "goalid" :value (:id goal)}]
                         [:input {:type :hidden :name "id" :value id}])]])]]
      (apply include-js (:javascripts config))
      (apply include-css (:styles config))])))

(defn index [{:keys [db] :as config}]
  (html5
   [:head
    [:title "Helper"]]
   [:body
    [:h1 "Helper"]
    [:p "This is a tool for personal development"]
    [:h2 "Add new goal"]
    (form-to [:post "/add-goal"]
             [:input {:name "desc" :type :text :placeholder "Goal Description" :required "true"}]
             [:input {:type :date :name "deadline" :required "true"}])
    [:h2 "Current Goals"]
    [:ul
     (for [goal (db/all db :goal)]
       [:li [:div [:a {:href (str "/goal?id=" (:id goal))} (str (:description goal)
                                                                " by "
                                                                (:deadline goal))]]])]


    (apply include-js (:javascripts config))
    (apply include-css (:styles config))]))

(def not-found (html5 "not found"))
