(ns running.routes.services.runs
  (:require [clj-time.coerce :as c]
            [clojure.tools.logging :as log]
            [ring.util.http-response :refer :all]
            [running.db.core :as db]
            [running.routes.services.common :refer [handler]]
            [schema.core :as s]
            [schema.coerce :as coerce])
  (:import (java.time Duration)))

; define some schema information
(def TimeOfDay (s/enum "am" "pm" "noon" "night"))
(def DistanceUnits (s/enum "m" "km" "mile" "miles" "meters"))
(def DurationSchema java.time.Duration)
(def ElapsedDuration (s/either DurationSchema s/Str))
(def iso-duration-regex #"^P(?!$)(\d+Y)?(\d+M)?(\d+W)?(\d+D)?(T(?=\d+[HMS])(\d+H)?(\d+M)?(\d+S)?)?$")
(def hhmmss-regex #"^(?:(?:([01]?\d|2[0-3]):)?([0-5]?\d):)?([0-5]?\d)$")

(defn elapsed-duration-matcher [schema]
  (when (= ElapsedDuration schema)
    (coerce/safe
      (fn [x]
        (if (and (string? x) (or
                               (re-matches iso-duration-regex x)
                               (re-matches hhmmss-regex x)))
                 db/string-duration-to-duration x)))))

; A schema for runs
(s/defschema Run
  {:runid                     s/Num
   :rdate                     s/Any ;LocalDate
   :timeofday                 (s/maybe TimeOfDay)
   (s/optional-key :distance) (s/maybe s/Num)
   (s/optional-key :units)    (s/maybe DistanceUnits)
   (s/optional-key :elapsed)  (s/maybe ElapsedDuration)
   (s/optional-key :comment)  (s/maybe s/Str)
   (s/optional-key :effort)   (s/maybe s/Str)
   (s/optional-key :shoeid)   (s/maybe s/Num)})

(s/defschema RunResult
  {(s/optional-key :run) Run
   (s/optional-key :error) s/Str})

(s/defschema RunsResult
  {(s/optional-key :runs) [Run]
   (s/optional-key :error) s/Str})

(handler all-runs []
  (ok (db/get-runs)))

(handler run [runid]
  (ok (db/get-run {:runid runid})))

(handler runs-by-date [rdate]
  (do
    (try
      (ok (db/get-runs-by-date {:rdate (c/to-sql-date rdate)}))
      (catch Exception e (log/error (.getMessage e))))))

(handler recent-runs [days]
         (ok (db/get-recent-runs {:limit (str days " days")})))

;(handler delete-run! [runid]
;  (ok (db/delete-run! {:runid runid})))

(handler filtered-runs [before-date after-date min-distance max-distance]
         (ok (db/get-filtered-runs {:before-date (c/to-sql-date :before-date)
                                    :after-date (c/to-sql-date :after-date)
                                    :min-distance :min-distance
                                    :max-distance :max-distance})))

;-- :snip filter-select-snip
;select :i*:cols
;-- :snip filter-from-snip
; from :i*:tables
;(filtered-query db {:select (filter-select-snip {:cols ["runid", "rdate", "timeofday", "distance", "units", "elapsed"]})
;                    :from (filter-from-snip {:tables ["runs"]})
;                    :where (filter-where-snip {})})