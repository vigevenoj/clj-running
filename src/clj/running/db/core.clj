(ns running.db.core
  (:require
    [cheshire.core :refer [generate-string parse-string]]
    [clojure.java.jdbc :as jdbc]
    [clojure.tools.logging :as log]
    [conman.core :as conman]
    [java-time :as jt]
    [running.config :refer [env]]
    [mount.core :refer [defstate]])
  (:import org.postgresql.util.PGobject
           java.sql.Array
           clojure.lang.IPersistentMap
           clojure.lang.IPersistentVector
           [java.sql
            BatchUpdateException
            PreparedStatement]
           (java.time.format DateTimeParseException)
           (java.time Duration LocalTime)
           (org.postgresql.util PGInterval)))
(defstate ^:dynamic *db*
  :start (if-let [jdbc-url (env :database-url)]
           (conman/connect! {:jdbc-url jdbc-url})
           (do
             (log/warn "database connection URL was not found, please set :database-url in your config, e.g: dev-config.edn")
             *db*))
  :stop (conman/disconnect! *db*))

(conman/bind-connection *db* "sql/queries.sql")

(extend-protocol jdbc/IResultSetReadColumn
  Array
  (result-set-read-column [v _ _] (vec (.getArray v)))

  PGobject
  (result-set-read-column [pgobj _metadata _index]
    (let [type  (.getType pgobj)
          value (.getValue pgobj)]
      (case type
        "json" (parse-string value true)
        "jsonb" (parse-string value true)
        "citext" (str value)
        value))))

(defn to-pg-json [value]
      (doto (PGobject.)
            (.setType "jsonb")
            (.setValue (generate-string value))))

(extend-type clojure.lang.IPersistentVector
  jdbc/ISQLParameter
  (set-parameter [v ^java.sql.PreparedStatement stmt ^long idx]
    (let [conn      (.getConnection stmt)
          meta      (.getParameterMetaData stmt)
          type-name (.getParameterTypeName meta idx)]
      (if-let [elem-type (when (= (first type-name) \_) (apply str (rest type-name)))]
        (.setObject stmt idx (.createArrayOf conn elem-type (to-array v)))
        (.setObject stmt idx (to-pg-json v))))))

(extend-protocol jdbc/ISQLValue
  IPersistentMap
  (sql-value [value] (to-pg-json value))
  IPersistentVector
  (sql-value [value] (to-pg-json value)))

(defn pginterval-to-duration [pginterval]
  "Convert from a postgresql PGInterval to a java.time.Duration"
  (.plusSeconds
    (.plusMinutes
      (.plusHours
        (Duration/ofDays
          (+ (* 365 (.getYears pginterval))
             (* 30 (.getMonths pginterval))
             (.getDays pginterval)))
        (.getHours pginterval))
      (.getMinutes pginterval))
    (.getSeconds pginterval)))

(defn hhmmss-to-duration [string-duration]
  "Convert from HH:MM:SS to java.time.Duration"
  (Duration/between
    LocalTime/MIN
    (LocalTime/parse string-duration)))

(defn string-duration-to-duration
  "Convert from a stringified duration like PT1H30M6S to a java.time.Duration"
  [string-duration]
  (try
     (Duration/parse string-duration)
     (catch DateTimeParseException e
       (try
         (hhmmss-to-duration string-duration)
         (catch DateTimeParseException e
           (log/warn (str "Unable to parse " string-duration " as duration")))))))

(defn duration-to-pginterval [^Duration duration]
  "Convert from a java.time.Duration to a postgresql PGInterval"
  (let [seconds (.getSeconds duration)]
    (PGInterval. 0 0 0 (quot seconds 3600) (quot (rem seconds 3600) 60) (rem seconds 60))))

(defn string-duration-to-pginterval [string-duration]
  (duration-to-pginterval(string-duration-to-duration string-duration)))

(extend-protocol jdbc/IResultSetReadColumn
  org.postgresql.util.PGInterval
  (result-set-read-column [value metadata index]
    (pginterval-to-duration value)))

(extend-type java.time.Duration
  jdbc/ISQLParameter
  (set-parameter [value ^PreparedStatement stmt idx]
    (.setObject stmt idx (duration-to-pginterval value))))

(extend-protocol jdbc/IResultSetReadColumn
  java.sql.Date
  ;java.time.LocalDate
  (result-set-read-column [v _ _] (jt/local-date v)))

(extend-type java.time.LocalDate
  jdbc/ISQLParameter
  (set-parameter [value ^PreparedStatement stmt idx]
    (.setObject stmt idx value)))