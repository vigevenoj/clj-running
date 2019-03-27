(ns running.util
  (:require [cljs-time.format :as format]
            [cljs-time.coerce :as c]))

(defn format-date [date]
  "Format a date for display to the user"
  ;(format/unparse (format/formatter "yyyy-MM-dd")
  ;           (c/from-date date))
  (str date))

(defn format-duration
  "Format an ISO-8601 style duration into something more familiar"
  [duration]
  (when (not (nil? duration))
    (let [duration-regex (re-pattern "^P(?!$)([0-9]+Y)?([0-9]+M)?([0-9]+W)?([0-9]+D)?(T(?=[0-9])([0-9]+H)?([0-9]+M)?([0-9]+S)?)?$")]
      ; The 6th through 9th elements are hours, minutes, and seconds.
      ; I don't expect any of our durations to be longer than that but if they are we can test for it
      (clojure.string/join " " (subvec (re-find duration-regex duration) 6 9)))))

(defn get-select-options
  "Returns an array of HTMLOptionElement js objects"
  [select-name]
  (array-seq (.-selectedOptions (js/document.getElementById select-name))))

(defn values-of-selected-options
  "Filters an array of HTMLOptionElement js objects to only the selected options"
  [array-of-options]
  (map (fn [e] (.-value e)) (filter #(.-selected %) array-of-options)))