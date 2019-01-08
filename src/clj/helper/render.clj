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

(defn books [config iterations iterationid books]
  (layout config
          {:iterations iterations
           :url "/books"
           :iterationid iterationid}
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
                  [:div
                   [:select {:name "actionitemid"}
                    (for [item actionitems]
                      [:option {:value (:id item)} (:description item)])]]
                  [:div
                   [:input {:type :hidden :name "goalid" :value (:id goal)}]]
                  [:div
                   [:input {:type :hidden :name "iterationid" :value (:id current-iteration)}]]
                  [:div
                   [:input {:type :text
                            :name "desc"
                            :placeholder "Task Description"
                            :required "true"}]]
                  [:div
                   [:input {:type :text
                            :name "estimate"
                            :placeholder "Time Estimate"}]]
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
   (add-task-form params [[:div [:input {:type :number :name "target" :value 1 :required "true"}]]
                          [:div [:input {:type :text :name "unit" :placeholder "Unit" :required "true"}]]
                          [:input {:type :hidden :name "current" :value "0"}]])])

(defmethod add-task :readingtask [{:keys [goal current-iteration actionitems books]}]
  [:div
   [:h3 (str "Add new reading task")]
   (form-to [:post "/add-task/readingtask"]
            [:div
             [:select {:name "actionitemid"}
              (for [item actionitems]
                [:option {:value (:id item)} (:description item)])]]
            [:div
             [:select {:name "bookid"}
              (for [book books]
                [:option {:value (:id book)} (:title book)])]]
            [:div
             [:input {:type :text
                      :name "estimate"
                      :placeholder "Time Estimate"}]]
            [:input {:type :number :name "target"}]
            [:div
             [:input {:type :submit :value "Add"}]]
            [:input {:type :hidden :name "goalid" :value (:id goal)}]
            [:input {:type :hidden :name "iterationid" :value (:id current-iteration)}]
            [:input {:type :hidden
                     :name "url"
                     :value (util/make-query-url "/goal" {:id (:id goal)
                                                          :iterationid (:id current-iteration)})}])])

(defn goal [config {:keys [iterations goal current-iteration actionitems subgoals total-estimate
                           incremental-tasks checked-tasks reading-tasks] :as params}]
  (layout config
          {:iterations iterations :url "/goal" :id (:id goal) :iterationid (:id current-iteration)}
          "Goal"
          [:div
           [:h2 (:description goal)]
           [:h3 (util/format-time total-estimate)]
           [:h3 "Add Subgoal"]
           (form-to [:post "/add/subgoal"]
                    [:input {:type :text
                             :name "desc"
                             :placeholder "Description"
                             :required "true"}]
                    [:input {:type :hidden :name "goalid" :value (:id goal)}]
                    [:input {:type :hidden
                             :name "url"
                             :value (util/make-query-url "/goal"
                                                         {:id (:id goal)
                                                          :iterationid (:id current-iteration)})}]
                    [:label "This iteration?"]
                    [:input {:type :checkbox :name "thisiteration" :checked "true"}])
           [:h3 "Subgoals"]
           [:table
            [:thead
             [:tr
              [:th "Name"]
              [:th "Remove"]]]
            [:tbody
             (for [subgoal subgoals]
               [:tr
                [:td (str (:description subgoal) (str (if (:thisiteration subgoal) " (this iteration)" "")))]
                [:td (form-to [:post "/remove/subgoal"]
                              [:input {:type :hidden :name "id" :value (:id subgoal)}]
                              [:input {:type :hidden
                                       :name "url"
                                       :value (util/make-query-url "/goal"
                                                                   {:id (:id goal)
                                                                    :iterationid (:id current-iteration)})}]
                              [:input {:type :submit :value "x"}])]])]]
           [:h3 "Add Action Item"]
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
                                                          :iterationid (:id current-iteration)})}])
           [:table
            [:thead
             [:tr
              [:th "Name"
               :th "Remove"]]]
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
                               [:input {:type :submit :value "x"}])]])]]
           (add-task (assoc params :kind :incrementaltask))
           (if current-iteration
             [:h3 (str "Current iteration: " (:startdate current-iteration) " to " (:enddate current-iteration))]
             [:h3 (str "No current iteration")])

           [:h3 "Tasks"]
           (html/table :incrementaltask
                       goal
                       (:id current-iteration)
                       incremental-tasks
                       (fn [task] (<= (:target task) (:current task)))
                       :current
                       :target
                       :unit)
           [:h3 "Updated Tasks"]
           [:ul
            (for [{:keys [description day]} (:incremental-task-log params)]
              [:li (str description " done on " day)])]]))

(defn- format-goal-title [goal]
  (str (:description goal) " by " (:deadline goal) " " (util/format-time (:estimate goal)) ")"))

(defn index [config {:keys [iterations iteration goals done-goal-ids]}]
  (layout config
          {:iterations iterations
           :url "/"
           :iterationid (:id iteration)}
          "Overview"
          [:div
           [:h2 "Current Goals"]
           [:ol
            (for [goal (sort-by :sequence goals)]
              [:li.mui-panel [:div {:class (if (some #{(:id goal)} done-goal-ids) "green" "")}
                              [:a {:href (util/make-query-url "/goal" {:id (:id goal)
                                                                       :iterationid (:id iteration)})}
                               (format-goal-title goal)]]])]
           [:p (str "To fulfill all tasks, a calculated average of "
                    (/ (apply + (filter some? (map (comp :hours :estimate) goals))) 90.0)
                    " hours per day needs to be spent")]
           [:h2 "Add new goal"]
           (form-to [:post "/add/goal"]
                    [:input {:name "desc"
                             :type :text
                             :placeholder "Goal Description"
                             :required "true"}]
                    [:input {:type :date :name "deadline" :required "true"}])])
  )

(def not-found (html5 "not found"))
