(ns helper.db.update
  (:require [clojure.java.jdbc :as j]
            [helper.util :as util]
            [helper.db.create :as create]
            [helper.db.query :as query]))

(defn- update-on-id [db table update-map id]
  (j/update! db table update-map ["id=?" id]))

(defn increment [db table column id]
  (j/execute! db
              [(str "UPDATE "
                    (name table)
                    " SET "
                    (name column)
                    " = "
                    (name column)
                    " + 1 WHERE id=?") id])
  (create/taskupdate db :incrementaltask id))

(defn toggle-done [db table id]
  (update-on-id db (keyword table) {:done true} id)
  (when (some #{(keyword table)} [:checkedtask :readingtask])
    (create/taskupdate db table id)))

(defn tweak-priority [db table id op]
  (let [update-fn (if (= op :up) util/pred util/succ)
        nxt (some-> db
                    (query/value db table :priority id)
                    first
                    update-fn)]
    (if-not nxt
      (case op
        :up (update-on-id db table {:priority "A"} id)
        :down (update-on-id db table {:priority "C"} id))
      (when (and nxt (apply <= (map int [\A nxt \C])))
        (update db table {:priority (str nxt)} id)))))

(defn tweak-sequence [db table id op]
  (let [update-fn (if (= op :up) util/pred util/succ)
        nxt (some-> db
                    (query/value db table :sequence id)
                    update-fn)]
    (when (and nxt (< 0 nxt))
      (update-on-id db table {:sequence nxt} id))))
