(ns goal-tracker.util
  "Namespace for utilities"
  (:require [clojure.string :as string]
            [clj-time.format :as time.format]))

(defn stringify [k] (-> k name string/capitalize))

(defn parse-int [x]
  (cond
    (= (type x) java.lang.Integer) x
    (= (type x) java.lang.String) (try (Integer. (re-find #"\d+" x))
                                       (catch NumberFormatException _ nil))
    :default nil))

(defn parse-bool [x]
  (cond
    (= (type x) java.lang.Boolean) x
    (= (type x) java.lang.String) (Boolean. x)
    :default nil))

(defn checked->bool [x]
  (case x
    "on" true
    false))

(defn succ [x]
  (condp = (type x)
    java.lang.Character (-> x int inc char)
    java.lang.Integer (inc x)))

(defn pred [x]
  (condp = (type x)
    java.lang.Character (-> x int dec char)
    java.lang.Integer (dec x)))

(defn make-query-url
  ([base m]
   (make-query-url base m (keys m)))
  ([base m ks]
   (str base "?" (string/join "&" (for [k ks] (if-let [v (m k)] (str (name k) "=" v) ""))))))

(defn format-time [{:keys [hours minutes]}]
  (str "Estimate: " (or hours "0") "h " (or minutes "0") "m"))

(defn normalize-time [{:keys [hours minutes]}]
  {:hours (+ (or  hours 0) (quot (or minutes 0) 60))
   :minutes (rem (or minutes 0) 60)})
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Date

(def ^:private date-string "yyyy-MM-dd")

(defn parse-date [s]
  (time.format/parse (time.format/formatters :year-month-day) s))

(defn ->sqldate [x]
  (condp = (type x)
    java.lang.String (new java.sql.Date  (.getMillis (parse-date x)))
    org.joda.time.DateTime (new java.sql.Date  (.getMillis x))))

(defn string->localdate [s]
  (java.time.LocalDate/parse s (java.time.format.DateTimeFormatter/ofPattern date-string)))

(defn ->localdate
  [date]
  (cond (= (type date) java.sql.Timestamp) (.. date toLocalDateTime toLocalDate)
        (= (type date) java.sql.Date) (.toLocalDate date)
        (= (type date) java.time.LocalDate) date
        (= (type date) java.time.LocalDateTime) date
        (= (type date) java.lang.String) (string->localdate date)
        (nil? date) (throw (Exception.  "Nil argument to localdate"))
        :default (throw (Exception. (str "Unknown date type: " (type date))))))
