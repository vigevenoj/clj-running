(ns running.test.db.core
  (:require [running.db.core :refer [*db*] :as db]
            [luminus-migrations.core :as migrations]
            [clojure.test :refer :all]
            [clojure.java.jdbc :as jdbc]
            [running.config :refer [env]]
            [mount.core :as mount]
    ;[clj-time.coerce :as c]
            [java-time :as jt]
            [buddy.hashers :as hashers]))

(use-fixtures
  :once
  (fn [f]
    (mount/start
      #'running.config/env
      #'running.db.core/*db*)
    (migrations/migrate ["migrate"] (select-keys env [:database-url]))
    (f)))

(deftest test-users
  (jdbc/with-db-transaction [t-conn *db*]
    (jdbc/db-set-rollback-only! t-conn)
    (is (= 1 (db/create-user!
               t-conn
               {:id         "1"
                :name "samsmith"
                :email      "sam.smith@example.com"
                :pass       (hashers/derive "pass")
                :is-active true})))
    (is (= {:id    "1"
            :name  "samsmith"
            :email "sam.smith@example.com"
            ;:pass  (hashers/derive "pass")
            :admin nil
            ;:last_login nil
            :is-active true}
           (dissoc (db/get-user t-conn {:id "1"}) :pass :last-login)))))

(deftest test-pginterval-duration-conversion
  ())

(deftest test-runs
  (jdbc/with-db-transaction[t-conn *db*]
                           (jdbc/db-set-rollback-only! t-conn)
                           (is (= 1 (db/create-run!
                                      t-conn
                                      ; I should try to use clojure.java-time for this
                                      ; and parse LocalDate. See matching comment below
                                      {:rdate     (jt/to-sql-date "2018-05-16")
                                       :timeofday "noon"
                                       :distance  10.2
                                       :units     "miles"
                                       :elapsed   (running.db.core/string-duration-to-duration "PT1H30M6S")
                                       :comment   nil
                                       :effort    nil
                                       :shoeid    nil})))
                           (is (= {:runid nil
                                   ; I should use clojure.java-time for this and parse a LocalDate out
                                   ; of this string--that would avoid the timezone issue that I keep seeing
                                   ; And I don't care about time zones in this field so that works fine
                                   ; rundis.github.io/blog/2015/clojure_dates.html suggests extending to
                                   ; do what I want, in section Reading from/Writing to the database
                                   :rdate (jt/local-date "2018-05-16")
                                   :timeofday "noon"
                                   :distance 10.2M
                                   :units "miles"
                                   ; I should set up a middleware like
                                   ; rundis.github.io/blog/2015/clojure_dates.html suggests in section about
                                   ; dates across process boundaries
                                   :elapsed (running.db.core/string-duration-to-duration "PT1H30M6S")
                                   :comment nil
                                   :effort nil
                                   :shoeid nil}
                                  (first (db/get-runs-by-date t-conn {:rdate (jt/to-sql-date "2018-05-16")}))))
                           ))
