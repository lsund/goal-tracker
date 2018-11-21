(ns helper.render
  "Namespace for rendering hiccup"
  (:require
   [medley.core :refer [find-first]]
   [helper.db :as db]
   [taoensso.timbre :as logging]
   [hiccup.form :refer [form-to]]
   [hiccup.page :refer [html5 include-css include-js]]
   [helper.util :as util]
   [helper.html :as html]))

(defn- layout
  [config title content]
  (html5
   [:head
    [:title (str "Helper - " title)]]
   [:body.mui-container
    (html/navbar)
    content
    (apply include-css (:styles config))
    (apply include-js (:javascripts config))]))

(defn books [config books]
  (layout config
          "Books"
          [:div
           [:h2 "Add book"]
           (form-to [:post "/add-book"]
                    [:input {:name "title" :type :text :placeholder "Book Title" :required "true"}])
           [:ul
            (for [book books]
              [:li (:title book)])]]))

(defn goal [config {:keys [goal current-iteration actionitems books incremental-tasks checked-tasks reading-tasks]}]
  (layout config
          "Goal"
          [:div
           [:h1 (:description goal)]
           [:h2 "Add action item"]
           (form-to [:post "/add-action-item"]
                    [:input {:type :text
                             :name "desc"
                             :placeholder "Action Item Description"
                             :required "true"}]
                    [:input {:name "goalid" :type :hidden :value (:id goal)}])
           [:ul
            (for [item actionitems]
              [:li (str (:description item))])]
           [:h2 "Add new incremental item"]
           (form-to [:post "/add-incremental-task"]
                    [:select {:name "actionitemid"}
                     (for [item actionitems]
                       [:option {:value (:id item)} (:description item)])]
                    [:input {:type :hidden :name "goalid" :value (:id goal)}]
                    [:input {:type :hidden :name "iterationid" :value (:id current-iteration)}]
                    [:input {:type :text
                             :name "desc"
                             :placeholder "Task Description"
                             :required "true"}]
                    [:input {:type :number :name "target" :value 0 :required "true"}]
                    [:input {:type :text :name "unit" :placeholder "Unit" :required "true"}]
                    [:input.hidden {:type :submit}])
           [:h2 "Add new checked item"]
           (form-to [:post "/add-checked-task"]
                    [:select {:name "actionitemid"}
                     (for [item actionitems]
                       [:option {:value (:id item)} (:description item)])]
                    [:input {:type :hidden :name "goalid" :value (:id goal)}]
                    [:input {:type :hidden :name "iterationid" :value (:id current-iteration)}]
                    [:input {:type :text
                             :name "desc"
                             :placeholder "Task Description"
                             :required "true"}]
                    [:input.hidden {:type :submit}])
           [:h2 "Add new reading item"]
           (form-to [:post "/add-reading-task"]
                    [:select {:name "actionitemid"}
                     (for [item actionitems]
                       [:option {:value (:id item)} (:description item)])]
                    [:select {:name "bookid"}
                     (for [book books]
                       [:option {:value (:id book)} (:title book)])]
                    [:input {:type :hidden :name "goalid" :value (:id goal)}]
                    [:input {:type :hidden :name "iterationid" :value (:id current-iteration)}]
                    [:input {:type :number :name "page" :required "true"}]
                    [:input.hidden {:type :submit}])
           (if current-iteration
             [:h2 (str "Current iteration: " (:startdate current-iteration) " to " (:enddate current-iteration))]
             [:h2 (str "No current iteration")])
           [:table
            [:thead
             [:tr
              [:th "Description"]
              [:th "Current"]
              [:th "Target"]
              [:th "Unit"]
              [:th "Increment"]]]
            [:tbody
             (for [{:keys [id description current target unit]} (sort-by :description incremental-tasks)]
               [:tr {:class (if (<= target current) "green" "")}
                [:td description]
                [:td current]
                [:td target]
                [:td unit]
                [:td (form-to [:post "/increment-incremental-task"]
                              [:input {:type :submit :value "+"}]
                              [:input {:type :hidden :name "goalid" :value (:id goal)}]
                              [:input {:type :hidden :name "id" :value id}])]])]]
           [:table
            [:thead
             [:tr
              [:th "Description"]
              [:th "Mark as Done"]]]
            [:tbody
             (for [{:keys [id description done]} checked-tasks]
               [:tr {:class (if done "green" "")}
                [:td description]
                [:td (form-to [:post "/mark-as-done/checkedtask"]
                              [:input {:type :submit :value "+"}]
                              [:input {:type :hidden :name "goalid" :value (:id goal)}]
                              [:input {:type :hidden :name "id" :value id}])]])]]
           [:table
            [:thead
             [:tr
              [:th "Title"]
              [:th "Page"]
              [:th "Mark as Done"]]]
            [:tbody
             (for [{:keys [id bookid page done]} reading-tasks]
               [:tr {:class (if done "green" "")}
                [:td (:title (find-first #(= (:id %) bookid) books))]
                [:td page]
                [:td (form-to [:post "/mark-as-done/readingtask"]
                              [:input {:type :submit :value "+"}]
                              [:input {:type :hidden :name "goalid" :value (:id goal)}]
                              [:input {:type :hidden :name "id" :value id}])]])]]]))

(defn index [config goals done-goal-ids]
  (layout config
          "Overview"
          [:div
           [:h1 "Helper"]
           [:p "This is a tool for personal development"]
           [:h2 "Add new goal"]
           (form-to [:post "/add-goal"]
                    [:input {:name "desc"
                             :type :text
                             :placeholder "Goal Description"
                             :required "true"}]
                    [:input {:type :date :name "deadline" :required "true"}])
           [:h2 "Current Goals"]
           [:ol
            (for [goal (sort-by :priority goals)]
              [:li [:div {:class (if (some #{(:id goal)} done-goal-ids) "green" "")}
                    [:a {:href (str "/goal?id=" (:id goal))} (str (:description goal)
                                                                       " by "
                                                                       (:deadline goal))]]])]]))

(def not-found (html5 "not found"))
