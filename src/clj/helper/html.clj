(ns helper.html
  "Namespace for HTML components"
  (:require [clojure.string :as string]
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
   (vec (concat [:tr
                 [:th "Sequence"]
                 [:th "Sequence Up"]
                 [:th "Sequence Down"]
                 [:th "Priority"]
                 [:th "Up priority"]
                 [:th "Down priority"]
                 [:th "Description"]
                 [:th "Toggle"]]
                (map #(conj [:th] %) extra-rows)))])

(defn make-tablebody [type goal tasks done-test & extra-keys]
  [:tbody
   (for [{:keys [sequence priority id description] :as task} (sort-by :sequence tasks)]
     (let [extra-rows (for [k extra-keys] (get task k))]
       (vec (concat  [:tr {:class (when (done-test task) "green")}
                      [:td sequence]
                      [:td (button-form :sort :up type (:id goal) id)]
                      [:td (button-form :sort :down type (:id goal) id)]
                      [:td priority]
                      [:td (button-form :prioritize :up type (:id goal) id)]
                      [:td (button-form :prioritize :down type (:id goal) id)]
                      [:td description]
                      [:td (button-form :nudge :at type (:id goal) id)]]
                     (map #(conj [:td] %) extra-rows)))))])

(defn table [type goal tasks done-test & extra-keys]
  [:table
   (apply make-tablehead (map (comp string/capitalize name) extra-keys))
   (apply (partial make-tablebody type goal tasks done-test) extra-keys)])
