(ns goal-tracker.html
  "Namespace for HTML components"
  (:require [clojure.string :as string]
            [hiccup.form :refer [form-to]]
            [goal-tracker.util :as util]
            [medley.core :refer [find-first]]))

(defn navbar [params]
  (println (:url params))
  [:div.mui-appbar
   [:table {:width "100%"}
    (let [iterationid (get-in params [:iteration :id])]
      [:tr {:style "vertical-align:middle;"}
       [:td.mui--appbar-height
        (form-to [:get (util/make-query-url "/" {:iterationid iterationid})]
                 (when iterationid
                   [:input {:type :hidden :name "iterationid" :value iterationid}])
                 [:input {:type :submit :value "Index"}])]
       [:td.mui--appbar-height
        (form-to [:get (:url params)]
                 [:input {:type :hidden :name "id" :value (get-in params [:goal :id])}]
                 [:select {:name "iterationid"}
                  (for [iteration (:iterations params)]
                    [:option {:value (:id iteration)} (str  (:startdate iteration)
                                                            "-----"
                                                            (:enddate iteration))])]
                 [:input {:type :submit :value "Go"}])]
       [:td.mui--appbar-height
        (form-to [:get (util/make-query-url "/books" {:iterationid iterationid})]
                 (when iterationid
                   [:input {:type :hidden :name "iterationid" :value iterationid}])
                 [:input {:type :submit :value "Books"}])]])]])

(defn button-form [op control goalid id iterationid]
  (form-to [:post (str "/" (name op) "/" (name control) "/" "task")]
           [:input {:type :submit :value "x"}]
           [:input {:type :hidden :name "goalid" :value goalid}]
           [:input {:type :hidden :name "id" :value id}]
           [:input {:type :hidden
                    :name "url"
                    :value (util/make-query-url "/goal" {:id goalid
                                                         :iterationid iterationid})}]))

(defn make-tablehead [& extra-rows]
  [:thead
   (vec (concat [:tr
                 [:th "Actionitem"]
                 [:th "Description"]
                 [:th "Sequence"]
                 [:th "Sequence Up"]
                 [:th "Sequence Down"]
                 [:th "Priority"]
                 [:th "Up priority"]
                 [:th "Down priority"]
                 [:th "Toggle"]
                 [:th "Estimate"]]
                (map #(conj [:th] %) extra-rows)))])

(defn make-tablebody [goal iterationid tasks done-test & extra-keys]
  [:tbody
   (for [{:keys [actionitemdescription sequence priority
                 id description timeestimate] :as task} (sort-by :sequence tasks)]
     (let [extra-rows (for [k extra-keys] (get task k))]
       (vec (concat  [:tr {:class (when (done-test task) "green")}
                      [:td actionitemdescription]
                      [:td description]
                      [:td sequence]
                      [:td (button-form :sort :up (:id goal) id iterationid)]
                      [:td (button-form :sort :down (:id goal) id iterationid)]
                      [:td priority]
                      [:td (button-form :prioritize :up (:id goal) id iterationid)]
                      [:td (button-form :prioritize :down (:id goal) id iterationid)]
                      [:td (button-form :nudge :at (:id goal) id iterationid)]
                      [:td timeestimate]]
                     (map #(conj [:td] %) extra-rows)))))])

(defn table [goal iterationid tasks done-test & extra-keys]
  [:table
   (apply make-tablehead (map (comp string/capitalize name) extra-keys))
   (apply (partial make-tablebody goal iterationid tasks done-test) extra-keys)])
