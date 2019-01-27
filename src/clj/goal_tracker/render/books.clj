(ns goal-tracker.render.books
  (:require [goal-tracker.render :as render]
            [hiccup.form :refer [form-to]]))

(defn layout [config params]
  (render/layout config
                 (assoc params :url "/books")
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
                     (for [{:keys [id title done donedate]} (:books params)]
                       [:tr {:class (if done "green" "")}
                        [:td title]
                        [:td (form-to [:post "/nudge/at/book"]
                                      [:input {:type :submit :value "+"}]
                                      [:input {:type :hidden :name "id" :value id}]
                                      [:input {:type :hidden
                                               :name "url"
                                               :value "/books"}])]
                        [:td donedate]])]]]]))
