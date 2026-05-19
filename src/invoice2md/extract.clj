(ns invoice2md.extract
  (:require [clojure.string :as str])
  (:import [java.time LocalDate]
           [java.time.format DateTimeFormatter]
           [java.util Locale]
           [java.util.regex Pattern]))

(defn- formatter
  [pattern]
  (DateTimeFormatter/ofPattern pattern Locale/GERMANY))

(defn- coerce-value
  [value {:keys [type input_format output_format]}]
  (case type
    "date" (.format (LocalDate/parse value (formatter input_format))
                    (formatter (or output_format "yyyy-MM-dd")))
    (str/trim value)))

(defn- extract-field
  [text field-name {:keys [regex group] :as spec}]
  (let [matcher (re-matcher (Pattern/compile regex) text)]
    (if (.find matcher)
      (coerce-value (.group matcher (long (or group 1))) spec)
      (throw (ex-info (str "Could not extract field: " (name field-name))
                      {:field field-name :regex regex})))))

(defn extract-fields
  [text fields]
  (reduce-kv (fn [acc field-name spec]
               (assoc acc field-name (extract-field text field-name spec)))
             {}
             fields))
