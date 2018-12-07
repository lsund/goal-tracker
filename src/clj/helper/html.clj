(ns helper.html
  "Namespace for HTML components"
  (:require
   [hiccup.form :refer [form-to]]
   [helper.util :as util]
   [medley.core :refer [find-first]]))

(defn navbar []
  [:div.mui-appbar
   [:table {:width "100%"}
    [:tr {:style "vertical-align:middle;"}
     [:td.mui--appbar-height
      (form-to [:get "/"]
               [:input {:type :submit :value "Index"}])]
     [:td.mui--appbar-height
      (form-to [:get "/books"]
               [:input {:type :submit :value "Books"}])]]]])

(defn button-form [op control type goalid id]
  (form-to [:post (str "/" (name op) "/" (name control) "/" (name type))]
           [:input {:type :submit :value "x"}]
           [:input {:type :hidden :name "goalid" :value goalid}]
           [:input {:type :hidden :name "id" :value id}]
           [:input {:type :hidden
                    :name "url"
                    :value (util/make-query-url "/goal" {:id goalid})}]))

(defn make-tablehead [& extra-rows]
  [:thead
   (concat [:tr
            [:th "Sequence"]
            [:th "Sequence Up"]
            [:th "Sequence Down"]
            [:th "Priority"]
            [:th "Up priority"]
            [:th "Down priority"]
            [:th "Description"]
            [:th "Toggle"]]
           (map #(conj [:th] %) extra-rows))])

(defn make-tablebody [goal tasks & extra-keys]

  [:tbody
   (for [{:keys [sequence priority id description] :as task} (sort-by :description tasks)]
     (let [extra-rows (for [k extra-keys] (get task k))]
       ;; TODO class "green" check is currently missing (fix for this?)
       (concat  [:tr
                 [:td sequence]
                 [:td (button-form :sort :up type (:id goal) id)]
                 [:td (button-form :sort :down type (:id goal) id)]
                 [:td priority]
                 [:td (button-form :prioritize :up type (:id goal) id)]
                 [:td (button-form :prioritize :down type (:id goal) id)]
                 [:td description]
                 [:td (button-form :nudge :at type (:id goal) id)]]
                (map #(conj [:td] %) extra-rows))))])

(defn table [type goal tasks extra-rows]
  [:table
   (apply make-tablehead extra-rows)
   [:tbody
    (for [{:keys [sequence
                  priority
                  id
                  description
                  current
                  target
                  unit]} (sort-by :description tasks)]
      [:tr {:class (if (<= target current) "green" "")}
       [:td sequence]
       [:td (button-form :sort :up type (:id goal) id)]
       [:td (button-form :sort :down type (:id goal) id)]
       [:td priority]
       [:td (button-form :prioritize :up type (:id goal) id)]
       [:td (button-form :prioritize :down type (:id goal) id)]
       [:td description]
       [:td (button-form :nudge :at type (:id goal) id)]
       [:td current]
       [:td target]
       [:td unit]])]])

(defn checkedtask-table [goal checked-tasks]
  [:table
   (make-tablehead)
   [:tbody
    (for [{:keys [sequence id priority description done]} checked-tasks]
      [:tr {:class (if done "green" "")}
       [:td sequence]
       [:td (button-form :sort :up :checkedtask (:id goal) id)]
       [:td (button-form :sort :down :checkedtask (:id goal) id)]
       [:td priority]
       [:td (button-form :prioritize :up :checkedtask (:id goal) id)]
       [:td (button-form :prioritize :down :checkedtask (:id goal) id)]
       [:td description]
       [:td (button-form :nudge :at :checkedtask (:id goal) id)]])]])


(defn readingtask-table [goal reading-tasks books]
  [:table
   [:thead
    (make-tablehead "Target Page")]
   [:tbody
    (for [{:keys [sequence id priority bookid page done]} reading-tasks]
      [:tr {:class (if done "green" "")}
       [:td sequence]
       [:td (button-form :sort :up :readingtask (:id goal) id)]
       [:td (button-form :sort :down :readingtask (:id goal) id)]
       [:td priority]
       [:td (button-form :prioritize :up :readingtask (:id goal) id)]
       [:td (button-form :prioritize :down :readingtask (:id goal) id)]
       [:td (:title (find-first #(= (:id %) bookid) books))]
       [:td (button-form :nudge :at :readingtask (:id goal) id)]
       [:td page]])]])
