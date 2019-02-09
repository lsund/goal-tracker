(ns goal-tracker.db.update
  (:require [clojure.java.jdbc :as j]
            [clj-time.core :as time]
            [goal-tracker.util :as util]
            [goal-tracker.db.core :as db]
            [goal-tracker.db.create :as create]
            [goal-tracker.db.read :as read]
            [goal-tracker.db.delete :as delete]))

(defn row [db table data id]
  (j/update! db table data ["id=?" id]))

(defn increment [db table column id]
  (let [current (read/value db table :current id)
        target (read/value db table :target id)]
    (if (= current target)
      (do
        (row db table {:current 0} id)
        (delete/by-column-id db :donetaskentry :taskid id))
      (do
        (j/execute! db
                    [(str "UPDATE "
                          (name table)
                          " SET "
                          (name column)
                          " = "
                          (name column)
                          " + 1 WHERE id=?") id])
        (create/done-task-entry db :incrementaltask id)))))

(defn decrement [db table column id]
  (j/execute! db
              [(str "UPDATE "
                    (name table)
                    " SET "
                    (name column)
                    " = "
                    (name column)
                    " - 1 WHERE id=?") id]))

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
