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
  (let [value (str/trim value)]
    (case type
      "date" (.format (LocalDate/parse value (formatter input_format))
                      (formatter (or output_format "yyyy-MM-dd")))
      value)))

(defn- field-stages
  [spec]
  (or (:stages spec) [spec]))

(defn- stage-spec
  [field-spec stage]
  (merge (dissoc field-spec :stages) stage))

(defn- stage-source
  [{:keys [content metadata]} {:keys [target metadata_key]}]
  (case (or target "content")
    "content" content
    "metadata" (get metadata (keyword metadata_key))
    nil))

(defn- extract-stage
  [sources field-spec stage]
  (let [{:keys [regex group] :as spec} (stage-spec field-spec stage)
        source (stage-source sources spec)]
    (when (and regex (seq source))
      (let [matcher (re-matcher (Pattern/compile regex) source)]
        (when (.find matcher)
          (coerce-value (.group matcher (long (or group 1))) spec))))))

(defn- extract-field
  [sources field-name spec]
  (if-let [value (some #(extract-stage sources spec %) (field-stages spec))]
    value
    (throw (ex-info (str "Could not extract field: " (name field-name))
                    {:field field-name :stages (field-stages spec)}))))

(defn extract-fields
  [sources fields]
  (reduce-kv (fn [acc field-name spec]
                (assoc acc field-name (extract-field sources field-name spec)))
              {}
              fields))
