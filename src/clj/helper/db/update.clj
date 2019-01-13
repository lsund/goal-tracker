(ns helper.db.update
  (:require [clojure.java.jdbc :as j]
            [clj-time.core :as time]
            [helper.util :as util]
            [helper.db.core :as db]
            [helper.db.create :as create]
            [helper.db.read :as read]
            [helper.db.delete :as delete]))

(defn row [db table data id]
  (j/update! db table data ["id=?" id]))

(defn increment [db table column id]
  (j/execute! db
              [(str "UPDATE "
                    (name table)
                    " SET "
                    (name column)
                    " = "
                    (name column)
                    " + 1 WHERE id=?") id])
  (create/done-task-entry db :incrementaltask id))

(defn toggle-done-task [db table id]
  (let [was-done? (read/value db table :done id)]
    (row db (keyword table) {:done (not was-done?)} id)
    (if was-done?
      (delete/done-task-entry db id)
      (create/done-task-entry db table id))))

(defn toggle-done-book [db id]
  (let [was-done? (read/value db :book :done id)]
    (row db :book {:done (not was-done?)
                            :donedate (when (not was-done?) (util/->sqldate (time/now)))} id)))

(defn- succ-priority [p]
  (case p
    :low :middle
    :middle :high
    :high :high))

(defn- pred-priority [p]
  (case p
    :low :low
    :middle :low
    :high :middle))

(defn tweak-priority [db table id op]
  (let [current-priority (read/value db table :priority id)
        update-fn (if (= op :up) succ-priority pred-priority)
        nxt (if current-priority
              (some-> current-priority keyword update-fn)
              (if (= op :up) "high" "low"))]
    (row db table {:priority (name nxt)} id)))

(defn tweak-sequence [db table id op]
  (let [update-fn (if (= op :up) util/pred util/succ)
        nxt (some-> db
                    (read/value table :sequence id)
                    update-fn)]
    (when (and nxt (< 0 nxt))
      (row db table {:sequence nxt} id))))
