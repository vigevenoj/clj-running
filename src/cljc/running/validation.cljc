(ns running.validation
  (:require [struct.core :as st]
            [bouncer.core :as b]
            [bouncer.validators :as v]))

(defn format-validation-errors [errors]
  (->> errors
       first
       (map (fn [[k [v]]] [k v]))
       (into {})
       not-empty))

(defn validate-create-run [run]
  (b/validate
    (fn [{:keys [path]}]
      ({[:rdate] "Date is required"
        [:timeofday] "Time of day is required"}
        path))
    run
    :rdate [v/required v/datetime]))

(defn pass-matches? [pass-confirm pass]
  (= pass pass-confirm))

(defn validate-create-user [user]
  (format-validation-errors
    (b/validate
    (fn [{:keys [path]}]
      ({[:name] "Username is required"
        [:pass] "Password of 8+ characters is required"
        [:pass-confirm] "Password confirmation does not match"
        [:active] "You must specify if the user is active"}
        path))
    user
    :pass [v/required [v/min-count 8]]
    :pass-confirm [[pass-matches? (:pass user)]]
    :name v/required
    :is-active v/required)))

(defn validate-update-user [user]
  (format-validation-errors
    (b/validate
      (fn [{:keys [path]}]
        ({[:name] "Name is required"
          [:pass-confirm] "Password does not match"
          [:is-active] "You must specify if the user is active"}
          path))
      user
      :name v/required
      :pass-confirm [[pass-matches? (:pass user)]]
      :is-active v/required)))