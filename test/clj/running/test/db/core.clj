(ns running.test.db.core
  (:require [running.db.core :refer [*db*] :as db]
            [luminus-migrations.core :as migrations]
            [clojure.test :refer :all]
            [clojure.java.jdbc :as jdbc]
            [running.config :refer [env]]
            [mount.core :as mount]
            [clj-time.coerce :as c]))

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
                :first_name "Sam"
                :last_name  "Smith"
                :email      "sam.smith@example.com"
                :pass       "pass"})))
    (is (= {:id         "1"
            :first_name "Sam"
            :last_name  "Smith"
            :email      "sam.smith@example.com"
            :pass       "pass"
            :admin      nil
            :last_login nil
            :is_active  nil}
           (db/get-user t-conn {:id "1"})))))

(deftest test-pginterval-duration-conversion
  ())

(deftest test-runs
  (jdbc/with-db-transaction[t-conn *db*]
                           (jdbc/db-set-rollback-only! t-conn)
                           (is (= 1 (db/create-run!
                                      t-conn
                                      {:rdate     (c/to-sql-date "2018-05-16")
                                       :timeofday "noon"
                                       :distance  10.2
                                       :units     "miles"
                                       :elapsed   (running.db.core/string-duration-to-duration "PT1H30M6S")
                                       :comment   nil
                                       :effort    nil
                                       :shoeid    nil})))
                           ;(is (= {:rdate (c/to-sql-date "2018-05-16")
                           ;        :timeofday "noon"
                           ;        :distance 10.2
                           ;        :units "miles"
                           ;        :elapsed (running.db.core/string-duration-to-duration "PT1H30M6S")
                           ;        :comment nil
                           ;        :effort nil
                           ;        :shoeid nil}
                           ;       (db/get-run t-conn {:runid 1})))
                           (is (= {:rdate (c/to-sql-date "2018-05-16")
                                   :timeofday "noon"
                                   :distance 10.2M
                                   :units "miles"
                                   :elapsed "PT1H30M6S"
                                   :comment nil
                                   :effort nil
                                   :shoeid nil}
                                  (first (db/get-runs-by-date t-conn {:rdate (c/to-sql-date "2018-05-16")}))))
                           ))
