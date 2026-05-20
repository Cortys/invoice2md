(ns invoice2md.cli
  (:require [clojure.pprint :as pprint]
            [clojure.string :as str]
            [clojure.tools.cli :as cli]
            [invoice2md.config :as config]
            [invoice2md.convert :as convert]
            [invoice2md.paths :as paths]))

(def convert-options
  [["-c" "--config PATH" "YAML config path"]
   [nil "--pdf-dir DIR" "Directory containing input PDFs"]
   [nil "--markdown-dir DIR" "Directory for generated Markdown files"]
   [nil "--receipt-dir DIR" "Directory for copied/renamed PDFs"]
   [nil "--overwrite" "Overwrite existing target files"]
   [nil "--dry-run" "Print planned actions without writing files"]
   [nil "--verbose" "Print extracted fields for each PDF"]
   ["-r" "--recursive" "Recursively scan pdf-dir for input PDFs"]
   ["-h" "--help" "Show help"]])

(defn- usage
  [summary]
  (str/join
   \newline
    ["Usage: invoice2md convert --config CONFIG --pdf-dir DIR --markdown-dir DIR --receipt-dir DIR"
    ""
    "Options:"
    summary]))

(defn- required-missing
  [opts]
  (seq (remove opts [:config :pdf-dir :markdown-dir :receipt-dir])))

(defn- format-field-value
  [value]
  (str/replace (str value) #"\s+" " "))

(defn- format-fields
  [fields]
  (->> fields
       (sort-by (comp name key))
       (map (fn [[k v]] (format "%s=%s" (name k) (format-field-value v))))
       (str/join ", ")))

(defn- result-line
  [{:keys [status source-pdf markdown-path receipt-path reason error extracted-fields]} verbose?]
  (let [line (case status
               :skipped (format "skip    %s -> %s (%s)" (.getName source-pdf) (.getName markdown-path) (name reason))
               :planned (format "plan    %s -> %s + %s" (.getName source-pdf) (.getName markdown-path) (.getName receipt-path))
               :created (format "create  %s -> %s + %s" (.getName source-pdf) (.getName markdown-path) (.getName receipt-path))
               :overwritten (format "write   %s -> %s + %s" (.getName source-pdf) (.getName markdown-path) (.getName receipt-path))
               :failed (format "fail    %s (%s)" (.getName source-pdf) (ex-message error))
               (format "%s %s" status source-pdf))]
    (if (and verbose? (seq extracted-fields))
      (format "%s (%s)" line (format-fields extracted-fields))
      line)))

(defn- print-result!
  [result verbose?]
  (println (result-line result verbose?)))

(defn- print-run-info!
  [options pdf-count]
  (println "Config:" (:config options))
  (println "Input PDFs:" (:pdf-dir options))
  (println "Recursive:" (if (:recursive options) "yes" "no"))
  (println "Markdown output:" (:markdown-dir options))
  (println "Receipt output:" (:receipt-dir options))
  (println "Found input PDFs:" pdf-count))

(defn- root-cause
  [^Throwable error]
  (loop [cause error]
    (if-let [next-cause (ex-cause cause)]
      (recur next-cause)
      cause)))

(defn- print-failure-detail!
  [{:keys [source-pdf error]}]
  (let [root (root-cause error)
        data (ex-data error)
        root-data (ex-data root)
        {:keys [content metadata]} (:pdf-sources data)]
    (binding [*out* *err*]
      (println)
      (println "Conversion failed")
      (println "  PDF:" (.getPath source-pdf))
      (println "  Error:" (ex-message error))
      (when-not (identical? error root)
        (println "  Cause:" (ex-message root)))
      (when (seq root-data)
        (println "  Cause data:")
        (pprint/pprint root-data))
      (when (seq metadata)
        (println "  PDF metadata:")
        (pprint/pprint metadata))
      (when (seq content)
        (println "  PDF text:")
        (println content)))))

(defn convert-command
  [args]
  (let [{:keys [options errors summary]} (cli/parse-opts args convert-options)
        missing (required-missing options)]
    (cond
      (:help options)
      (println (usage summary))

      (seq errors)
      (do
        (binding [*out* *err*]
          (doseq [error errors] (println error))
          (println)
          (println (usage summary)))
        (System/exit 1))

      missing
      (do
        (binding [*out* *err*]
          (println "Missing required options:" (str/join ", " (map name missing)))
          (println)
          (println (usage summary)))
        (System/exit 1))

      :else
      (let [pdf-files (paths/pdf-files (:pdf-dir options) (:recursive options))
            _ (print-run-info! options (count pdf-files))
            cfg (config/load-config (:config options))
            results (convert/convert-dir! cfg (assoc options :pdf-files pdf-files))]
        (doseq [result results]
          (print-result! result (:verbose options)))
        (when-let [failures (seq (filter #(= :failed (:status %)) results))]
          (doseq [failure failures]
            (print-failure-detail! failure))
          (System/exit 1))))))

(defn dispatch
  [args]
  (let [[command & command-args] args]
    (case command
      "convert" (convert-command command-args)
      (do
        (binding [*out* *err*]
          (println "Usage: invoice2md <command>")
          (println)
          (println "Commands:")
          (println "  convert"))
        (System/exit 1)))))
