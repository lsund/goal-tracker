(ns helper.render
  "Namespace for rendering hiccup"
  (:require
   [clojure.string :as string]
   [taoensso.timbre :as logging]
   [hiccup.form :refer [form-to]]
   [hiccup.page :refer [html5 include-css include-js]]
   [helper.util :as util]
   [helper.html :as html]))

(defn layout
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
                    (format "%.1f"
                            (/ (apply + (filter some? (map (comp :hours :estimate) goals))) 90.0))
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
