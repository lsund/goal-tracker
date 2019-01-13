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

(defn- format-goal-title [goal]
  (str (:description goal) " by " (:deadline goal) " " (util/format-time (:estimate goal)) ")"))

(defn index [config params]
  (layout config
          (assoc params :url "/" )
          "Overview"
          [:div
           [:h2 "Current Goals"]
           [:ol
            (for [goal (sort-by :sequence (:goals params))]
              [:li.mui-panel [:div {:class (if (some #{(:id goal)} (:done-goal-ids params)) "green" "")}
                              [:a {:href
                                   (util/make-query-url "/goal"
                                                        {:id (:id goal)
                                                         :iterationid (get-in params [:iteration :id])})}
                               (format-goal-title goal)]]])]
           [:p (str "To fulfill all tasks, a calculated average of "
                    (format "%.1f"
                            (/ (apply + (filter some? (map (comp :hours :estimate) (:goals params)))) 90.0))
                    " hours per day needs to be spent")]
           [:h2 "Add new goal"]
           (form-to [:post "/add/goal"]
                    [:input {:name "desc"
                             :type :text
                             :placeholder "Goal Description"
                             :required "true"}]
                    [:input {:type :date :name "deadline" :required "true"}])]))

(def not-found (html5 "not found"))
