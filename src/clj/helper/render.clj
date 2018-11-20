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

(defn books [config books]
  (html5
   [:head
    [:title "Books"]]
   [:body
    (html/navbar)
    [:h2 "Add book"]
    (form-to [:post "/add-book"]
             [:input {:name "title" :type :text :placeholder "Book Title" :required "true"}])
    [:ul
     (for [book books]
       [:li (:title book)])]
    (apply include-js (:javascripts config))
    (apply include-css (:styles config))]))

(defn goal [{:keys [db] :as config} goal]
  (let [iter (db/current-iteration db)]
    (html5
     [:head
      [:title "Helper"]]
     [:body
      (html/navbar)
      [:h1 (:description goal)]
      [:h2 "Add action item"]
      (form-to [:post "/add-action-item"]
               [:input {:name "desc" :type :text :placeholder "Action Item Description" :required "true"}]
               [:input {:name "goalid" :type :hidden :value (:id goal)}])
      [:h2 "Add new incremental item"]
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
      [:h2 "Add new checked item"]
      (form-to [:post "/add-checked-task"]
               [:select {:name "actionitemid"}
                (for [item (db/all db :actionitem)]
                  [:option {:value (:id item)} (:description item)])]
               [:input {:type :hidden :name "goalid" :value (:id goal)}]
               [:input {:type :hidden :name "iterationid" :value (:id iter)}]
               [:input {:type :text :name "desc" :placeholder "Task Description" :required "true"}]
               [:input.hidden {:type :submit}])
      [:h2 "Add new reading item"]
      (form-to [:post "/add-reading-task"]
               [:select {:name "actionitemid"}
                (for [item (db/all db :actionitem)]
                  [:option {:value (:id item)} (:description item)])]
               [:select {:name "bookid"}
                (for [book (db/all db :book)]
                  [:option {:value (:id book)} (:title book)])]
               [:input {:type :hidden :name "goalid" :value (:id goal)}]
               [:input {:type :hidden :name "iterationid" :value (:id iter)}]
               [:input {:type :number :name "page" :required "true"}]
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
      [:table
       [:thead
        [:tr
         [:th "Description"]
         [:th "Mark as Done"]]]
       [:tbody
        (for [{:keys [id description done]}
              (db/all-where db :checkedtask (str "iterationid="
                                                 (:id (db/current-iteration db))
                                                 " and goalid="
                                                 (:id goal)))]
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
        (let [books (db/all db :book)]
          (for [{:keys [id bookid page done]}
                (db/all-where db :readingtask (str "iterationid="
                                                   (:id (db/current-iteration db))
                                                   " and goalid="
                                                   (:id goal)))]
            [:tr {:class (if done "green" "")}
             [:td (:title (find-first #(= (:id %) bookid) books))]
             [:td page]
             [:td (form-to [:post "/mark-as-done/readingtask"]
                           [:input {:type :submit :value "+"}]
                           [:input {:type :hidden :name "goalid" :value (:id goal)}]
                           [:input {:type :hidden :name "id" :value id}])]]))]]
      (apply include-js (:javascripts config))
      (apply include-css (:styles config))])))

(defn index [{:keys [db] :as config}]
  (html5
   [:head
    [:title "Helper"]]
   [:body
    (html/navbar)
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
