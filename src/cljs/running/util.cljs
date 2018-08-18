(ns running.util)

(defn format-date [date]
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