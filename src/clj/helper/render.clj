(ns helper.render
  "Namespace for rendering hiccup"
  (:require
   [clojure.string :as string]
   [taoensso.timbre :as logging]
   [hiccup.form :refer [form-to]]
   [hiccup.page :refer [html5 include-css include-js]]
   [helper.util :as util]
   [helper.html :as html]))

(defn- layout
  [config params title content]
  (html5
   [:head
    [:title (str "Helper - " title)]]
   [:body.mui-container
    (html/navbar params)
    content
    (apply include-css (:styles config))
    (apply include-js (:javascripts config))]))

(defn books [config iterations books]
  (layout config
          {:iterations iterations :url "/books"}
          "Books"
          [:div
           [:h2 "Add book"]
           (form-to [:post "/add/book"]
                    [:input {:type :text :name "desc" :placeholder "Book Title" :required "true"}])
           [:table
            [:thead
             [:tr
              [:th "Title"]
              [:th "Toggle Done"]
              [:th "Done date"]]]
            [:tbody
             [:tbody
              (for [{:keys [id title done donedate]} books]
                [:tr {:class (if done "green" "")}
                 [:td title]
                 [:td (form-to [:post "/nudge/at/book"]
                               [:input {:type :submit :value "+"}]
                               [:input {:type :hidden :name "id" :value id}]
                               [:input {:type :hidden
                                        :name "url"
                                        :value "/books"}])]
                 [:td donedate]])]]]]))

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
                  [:input {:type :hidden
                           :name "url"
                           :value (util/make-query-url "/goal" {:id (:id goal)
                                                                :iterationid (:id current-iteration)})}]
                  [:input.hidden {:type :submit}]]
                 extra-inputs)))

(defmulti add-task
  (fn [params] (:kind params)))

(defmethod add-task :incrementaltask [params]
  [:div
   [:h3 (str "Add new incremental task")]
   (add-task-form params [[:input {:type :number :name "target" :value 1 :required "true"}]
                          [:input {:type :text :name "unit" :placeholder "Unit" :required "true"}]
                          [:input {:type :hidden :name "current" :value "0"}]])])

(defmethod add-task :checkedtask [params]
  [:div
   [:h3 (str "Add new checked task")]
   (add-task-form params [])])

(defmethod add-task :readingtask [{:keys [goal current-iteration actionitems books]}]
  [:div
   [:h3 (str "Add new reading task")]
   (form-to [:post "/add-task/readingtask"]
            [:select {:name "actionitemid"}
             (for [item actionitems]
               [:option {:value (:id item)} (:description item)])]
            [:select {:name "bookid"}
             (for [book books]
               [:option {:value (:id book)} (:title book)])]
            [:input {:type :hidden :name "goalid" :value (:id goal)}]
            [:input {:type :hidden :name "iterationid" :value (:id current-iteration)}]
            [:input {:type :hidden
                     :name "url"
                     :value (util/make-query-url "/goal" {:id (:id goal)
                                                          :iterationid (:id current-iteration)})}]
            [:input {:type :number :name "target" :required "true"}])])

(defn goal [config iterations {:keys [goal current-iteration actionitems
                                      incremental-tasks checked-tasks reading-tasks] :as params}]
  (layout config
          {:iterations iterations :url "/goal" :id (:id goal) :iterationid (:id current-iteration)}
          "Goal"
          [:div
           [:h2 (:description goal)]
           [:h3 "Add action item"]
           (form-to [:post "/add/actionitem"]
                    [:input {:type :text
                             :name "desc"
                             :placeholder "Action Item Description"
                             :required "true"}]
                    [:input {:type :hidden :name "goalid" :value (:id goal)}]
                    [:input {:type :hidden :name "url" :value (util/make-query-url "/goal" goal [:id])}])
           [:ul
            (for [item actionitems]
              [:li (str (:description item))])]
           (add-task (assoc params :kind :incrementaltask))
           (add-task (assoc params :kind :readingtask))
           (if current-iteration
             [:h3 (str "Current iteration: " (:startdate current-iteration) " to " (:enddate current-iteration))]
             [:h3 (str "No current iteration")])

           [:h3 "Incremental tasks"]
           (html/table :incrementaltask
                       goal
                       incremental-tasks
                       (fn [task] (<= (:target task) (:current task)))
                       :current
                       :target
                       :unit)
           [:h3 "Reading tasks"]
           (html/table :readingtask
                       goal
                       reading-tasks
                       :done
                       :page)

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

(defn index [config iterations iteration goals done-goal-ids]
  (layout config
          {:iterations iterations :url "/"}
          "Overview"
          [:div
           [:h1 "Helper"]
           [:h2 "Add new goal"]
           (form-to [:post "/add/goal"]
                    [:input {:name "desc"
                             :type :text
                             :placeholder "Goal Description"
                             :required "true"}]
                    [:input {:type :date :name "deadline" :required "true"}])
           [:h2 "Current Goals"]
           [:ol
            (for [goal (sort-by :sequence goals)]
              [:li [:div {:class (if (some #{(:id goal)} done-goal-ids) "green" "")}
                    [:a {:href (util/make-query-url "/goal" {:id (:id goal)
                                                             :iterationid (:id iteration)})}
                     (str (:description goal) " by " (:deadline goal))]]])]]))

(def not-found (html5 "not found"))
