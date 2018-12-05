(ns helper.render
  "Namespace for rendering hiccup"
  (:require
   [clojure.string :as string]
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
           (form-to [:post "/add/book"]
                    [:input {:type :text :name "desc" :placeholder "Book Title" :required "true"}])
           [:ul
            (for [book books]
              [:li (:title book)])]]))

(defn- make-query-url
  ([base m]
   (make-query-url base m (keys m)))
  ([base m ks]
   (str base "?" (string/join "&" (for [k ks] (if-let [v (m k)] (str (name k) "=" v) ""))))))

(defn add-task-form [{:keys [kind goal current-iteration actionitems]} extra-inputs]
  (apply form-to
         (concat [[:post (str "/add-task/" (name kind))]
                  [:select {:name "actionitemid"}
                   (for [item actionitems]
                     [:option {:value (:id item)} (:description item)])]
                  [:input {:type :hidden :name "goalid" :value (:id goal)}]
                  [:input {:type :hidden :name "iterationid" :value (:id current-iteration)}]
                  [:input {:type :text
                           :name "desc"
                           :placeholder "Task Description"
                           :required "true"}]
                  [:input {:type :hidden :name "url" :value (make-query-url "/goal" goal [:id])}]
                  [:input.hidden {:type :submit}]]
                 extra-inputs)))

(defmulti add-task
  (fn [params] (:kind params)))

(defmethod add-task :incrementaltask [params]
  [:div
   [:h2 (str "Add new incremental task")]
   (add-task-form params [[:input {:type :number :name "target" :value 0 :required "true"}]
                          [:input {:type :text :name "unit" :placeholder "Unit" :required "true"}]
                          [:input {:type :hidden :name "current" :value "0"}]])])

(defmethod add-task :checkedtask [params]
  [:div
   [:h2 (str "Add new checked task")]
   (add-task-form params [])])

(defmethod add-task :readingtask [params]
  [:div
   [:h2 (str "Add new reading task")]
   (add-task-form params [[:input {:type :number :name "target" :required "true"}]])])

(defn goal [config {:keys [goal current-iteration actionitems books incremental-tasks checked-tasks reading-tasks] :as params}]
  (layout config
          "Goal"
          [:div
           [:h1 (:description goal)]
           [:h2 "Add action item"]
           (form-to [:post "/add/actionitem"]
                    [:input {:type :text
                             :name "desc"
                             :placeholder "Action Item Description"
                             :required "true"}]
                    [:input {:type :hidden :name "goalid" :value (:id goal)}]
                    [:input {:type :hidden :name "url" :value (make-query-url "/goal" goal [:id])}])
           [:ul
            (for [item actionitems]
              [:li (str (:description item))])]
           (add-task (assoc params :kind :incrementaltask))
           (add-task (assoc params :kind :checkedtask))
           (add-task (assoc params :kind :readingtask))
           (if current-iteration
             [:h2 (str "Current iteration: " (:startdate current-iteration) " to " (:enddate current-iteration))]
             [:h2 (str "No current iteration")])
           [:table
            [:thead
             [:tr
              [:th "sequence"]
              [:th "Priority"]
              [:th "Description"]
              [:th "Current"]
              [:th "Target"]
              [:th "Unit"]
              [:th "Increment"]
              [:th "sequence up"]
              [:th "sequence Down"]
              [:th "Up priority"]
              [:th "Down priority"]]]
            [:tbody
             (for [{:keys [sequence priority id description current target unit]} (sort-by :description incremental-tasks)]
               [:tr {:class (if (<= target current) "green" "")}
                [:td sequence]
                [:td priority]
                [:td description]
                [:td current]
                [:td target]
                [:td unit]
                [:td (form-to [:post "/increment-task"]
                              [:input {:type :submit :value "+"}]
                              [:input {:type :hidden :name "goalid" :value (:id goal)}]
                              [:input {:type :hidden :name "id" :value id}])]
                [:td (form-to [:post "/tweak-sequence/up/incrementaltask"]
                              [:input {:type :submit :value "^"}]
                              [:input {:type :hidden :name "goalid" :value (:id goal)}]
                              [:input {:type :hidden :name "id" :value id}])]
                [:td (form-to [:post "/tweak-sequence/down/incrementaltask"]
                              [:input {:type :submit :value "v"}]
                              [:input {:type :hidden :name "goalid" :value (:id goal)}]
                              [:input {:type :hidden :name "id" :value id}])]
                [:td (form-to [:post "/tweak-priority/up/incrementaltask"]
                              [:input {:type :submit :value "^"}]
                              [:input {:type :hidden :name "goalid" :value (:id goal)}]
                              [:input {:type :hidden :name "id" :value id}])]
                [:td (form-to [:post "/tweak-priority/down/incrementaltask"]
                              [:input {:type :submit :value "v"}]
                              [:input {:type :hidden :name "goalid" :value (:id goal)}]
                              [:input {:type :hidden :name "id" :value id}])]])]]
           [:table
            [:thead
             [:tr
              [:th "sequence"]
              [:th "Priority"]
              [:th "Description"]
              [:th "Mark as Done"]
              [:th "sequence up"]
              [:th "sequence Down"]
              [:th "Up priority"]
              [:th "Down priority"]]]
            [:tbody
             (for [{:keys [sequence id priority description done]} checked-tasks]
               [:tr {:class (if done "green" "")}
                [:td sequence]
                [:td priority]
                [:td description]
                [:td (form-to [:post "/toggle-done/checkedtask"]
                              [:input {:type :submit :value "+"}]
                              [:input {:type :hidden :name "goalid" :value (:id goal)}]
                              [:input {:type :hidden :name "id" :value id}]
                              [:input {:type :hidden
                                       :name "url"
                                       :value (make-query-url "/goal" goal [:id])}])]
                [:td (form-to [:post "/tweak-sequence/up/checkedtask"]
                              [:input {:type :submit :value "^"}]
                              [:input {:type :hidden :name "goalid" :value (:id goal)}]
                              [:input {:type :hidden :name "id" :value id}])]
                [:td (form-to [:post "/tweak-sequence/down/checkedtask"]
                              [:input {:type :submit :value "v"}]
                              [:input {:type :hidden :name "goalid" :value (:id goal)}]
                              [:input {:type :hidden :name "id" :value id}])]
                [:td (form-to [:post "/tweak-priority/up/checkedtask"]
                              [:input {:type :submit :value "^"}]
                              [:input {:type :hidden :name "goalid" :value (:id goal)}]
                              [:input {:type :hidden :name "id" :value id}])]
                [:td (form-to [:post "/tweak-priority/down/checkedtask"]
                              [:input {:type :submit :value "v"}]
                              [:input {:type :hidden :name "goalid" :value (:id goal)}]
                              [:input {:type :hidden :name "id" :value id}])]])]]
           [:table
            [:thead
             [:tr
              [:th "Sequence"]
              [:th "Priority"]
              [:th "Title"]
              [:th "Page"]
              [:th "Mark as Done"]
              [:th "sequence up"]
              [:th "sequence Down"]
              [:th "Up Priority"]
              [:th "Down Priority"]]]
            [:tbody
             (for [{:keys [sequence id priority bookid page done]} reading-tasks]
               [:tr {:class (if done "green" "")}
                [:td sequence]
                [:td priority]
                [:td (:title (find-first #(= (:id %) bookid) books))]
                [:td page]
                [:td (form-to [:post "/toggle-done/readingtask"]
                              [:input {:type :submit :value "+"}]
                              [:input {:type :hidden :name "goalid" :value (:id goal)}]
                              [:input {:type :hidden :name "id" :value id}]
                              [:input {:type :hidden
                                       :name "url"
                                       :value (make-query-url "/goal" goal [:id])}])]
                [:td (form-to [:post "/tweak-sequence/up/readingtask"]
                              [:input {:type :submit :value "^"}]
                              [:input {:type :hidden :name "goalid" :value (:id goal)}]
                              [:input {:type :hidden :name "id" :value id}])]
                [:td (form-to [:post "/tweak-sequence/down/readingtask"]
                              [:input {:type :submit :value "v"}]
                              [:input {:type :hidden :name "goalid" :value (:id goal)}]
                              [:input {:type :hidden :name "id" :value id}])]
                [:td (form-to [:post "/tweak-priority/up/readingtask"]
                              [:input {:type :submit :value "^"}]
                              [:input {:type :hidden :name "goalid" :value (:id goal)}]
                              [:input {:type :hidden :name "id" :value id}])]
                [:td (form-to [:post "/tweak-priority/down/readingtask"]
                              [:input {:type :submit :value "v"}]
                              [:input {:type :hidden :name "goalid" :value (:id goal)}]
                              [:input {:type :hidden :name "id" :value id}])]])]]
           [:h3 "Updated incremental tasks"]
           [:ul
            (for [{:keys [description day]} (:incremental-task-log params)]
              [:li (str description " done on " day)])]
           [:h3 "Updated checked tasks"]
           [:ul
            (for [{:keys [description day]} (:checked-task-log params)]
              [:li (str description " " day)])]
           [:h3 "Updated reading tasks"]
           [:ul
            (for [{:keys [title day]} (:reading-task-log params)]
              [:li (str title " " day)])]]))

(defn index [config goals done-goal-ids]
  (layout config
          "Overview"
          [:div
           [:h1 "Helper"]
           [:p "This is a tool for personal development"]
           [:h2 "Add new goal"]
           (form-to [:post "/add/goal"]
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
