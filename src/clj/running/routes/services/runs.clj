(ns running.routes.services.runs
  (:require [java-time :as jt]
            [clojure.tools.logging :as log]
            [ring.util.http-response :refer :all]
            [running.db.core :as db]
            [running.routes.services.common :refer [handler]]
            [schema.core :as s]
            [schema.coerce :as coerce]))

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
  {(s/optional-key :runid)    (s/maybe s/Num)
   :rdate                     java.time.LocalDate
   :timeofday                 (s/maybe TimeOfDay)
   (s/optional-key :distance) (s/maybe s/Num)
   (s/optional-key :units)    (s/maybe DistanceUnits)
   (s/optional-key :elapsed)  (s/maybe ElapsedDuration)
   (s/optional-key :comment)  (s/maybe s/Str)
   (s/optional-key :effort)   (s/maybe s/Str)
   (s/optional-key :shoeid)   (s/maybe s/Num)})

(s/defschema RunResult
  {(s/optional-key :run)   Run
   (s/optional-key :error) s/Str})

(s/defschema RunsResult
  {(s/optional-key :runs)  [Run]
   (s/optional-key :error) s/Str})

(handler all-runs []
         (db/get-runs))

(handler run [runid]
         (db/get-run {:runid runid}))

(handler runs-by-date [rdate]
         (do
           (try
             (ok (db/get-runs-by-date {:rdate (jt/to-sql-date rdate)}))
             (catch Exception e
               (do
                 (log/error (.getMessage e))
                 (internal-server-error))))))

(handler recent-runs [days]
         (ok (db/get-recent-runs {:limit (str days " days")})))

(handler latest-runs [limit]
         (log/error "Limit is " limit)
         (do
           (try
             (db/get-latest-runs {:limit limit})
             (catch Exception e
               (do
                 (log/error (.printStackTrace e))
                 (internal-server-error))))))


(handler current-period-distance [period units]
         (try
           (case period
             ; todo: clean this up, extract common logic
             ; todo be consistent about handling response logic here or in services
             "year" (ok
                     (merge (first(db/get-current-year-distance {:units units}))
                            {:units units}))
             "month" (ok
                      (merge (first (db/get-current-month-distance {:units units}))
                             {:units units}))
             "week" (ok
                     (merge (first (db/get-current-week-distance {:units units}))
                            {:units units}))
             (bad-request))
           (catch Exception e                               ; likely org.postgresql.util.PSQLException but really we handle anything the same way
             (do
               (log/error (.printStackTrace e))
               (internal-server-error)))))


(handler rolling-period-distance [period units]
         (try
           (case period
             "week" (ok (db/get-rolling-period-distance {:period "1 week" :units units}))
             "month" (ok (db/get-rolling-period-distance {:period "1 month" :units units}))
             "90" (ok (db/get-rolling-period-distance {:period "90 days" :units units}))
             "180" (ok (db/get-rolling-period-distance {:period "180 days" :units units}))
             "year" (ok (db/get-rolling-period-distance {:period "1 year" :units units}))
             (bad-request))
           (catch Exception e                               ; likely org.postgresql.util.PSQLException but really we handle anything the same way
             (do
               (log/error (.printStackTrace e))
               (internal-server-error)))))


;(handler delete-run! [runid]
;  (ok (db/delete-run! {:runid runid})))

(handler filtered-runs [before-date after-date min-distance max-distance]
         (ok (db/get-filtered-runs {:before-date  (jt/to-sql-date :before-date)
                                    :after-date   (jt/to-sql-date :after-date)
                                    :min-distance :min-distance
                                    :max-distance :max-distance})))

(handler daily-distance-by-years [units years]
         (db/get-daily-distance-by-years {:years years
                                 :units "miles"}))

(handler daily-distance-all-years [units]
         (case units
           "miles" (db/daily-miles-all-years)
           (db/get-daily-distance-all-years {:units units})))

;-- :snip filter-select-snip
;select :i*:cols
;-- :snip filter-from-snip
; from :i*:tables
;(filtered-query db {:select (filter-select-snip {:cols ["runid", "rdate", "timeofday", "distance", "units", "elapsed"]})
;                    :from (filter-from-snip {:tables ["runs"]})
;                    :where (filter-where-snip {})})