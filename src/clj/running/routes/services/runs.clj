(ns running.routes.services.runs
  (:require [clj-time.coerce :as c]
            [clojure.tools.logging :as log]
            [ring.util.http-response :refer :all]
            [running.db.core :as db]
            [running.routes.services.common :refer [handler]]
            [schema.core :as s]))

; define some schema information
(def TimeOfDay (s/enum "am" "pm" "noon" "night"))
(def DistanceUnits (s/enum "m" "km" "mile" "miles" "meters"))

; A schema for runs
(s/defschema Run
  {:runid s/Num
   :rdate s/Any;LocalDate
   :timeofday (s/maybe TimeOfDay)
   (s/optional-key :distance) (s/maybe s/Num)
   (s/optional-key :units) (s/maybe DistanceUnits)
   (s/optional-key :elapsed) (s/maybe s/Any)
   (s/optional-key :comment) (s/maybe s/Str)
   (s/optional-key :effort) (s/maybe s/Str)
   (s/optional-key :shoeid) (s/maybe s/Num)})

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

(handler filtered-runs [])