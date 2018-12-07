(ns helper.util
  "Namespace for utilities"
  (:require [clojure.string :as string]
            [clj-time.format :as time.format]))

(defn stringify [k] (-> k name s/capitalize))

(defn parse-int [s]
  (when s
    (Integer. (re-find  #"\d+" s))))

(defn parse-date [s]
  (time.format/parse (time.format/formatters :year-month-day) s))

(defn ->sqldate [x]
  (condp = (type x)
    java.lang.String (new java.sql.Date  (.getMillis (parse-date x)))
    org.joda.time.DateTime (new java.sql.Date  (.getMillis x))))

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
