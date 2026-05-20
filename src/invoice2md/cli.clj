(ns invoice2md.cli
  (:require [clojure.string :as str]
            [clojure.tools.cli :as cli]
            [invoice2md.config :as config]
            [invoice2md.convert :as convert]))

(def convert-options
  [["-c" "--config PATH" "YAML config path"]
   [nil "--pdf-dir DIR" "Directory containing input PDFs"]
   [nil "--markdown-dir DIR" "Directory for generated Markdown files"]
   [nil "--receipt-dir DIR" "Directory for copied/renamed PDFs"]
   [nil "--overwrite" "Overwrite existing target files"]
   [nil "--dry-run" "Print planned actions without writing files"]
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

(defn- print-result!
  [{:keys [status source-pdf markdown-path receipt-path reason]}]
  (println
   (case status
     :skipped (format "skip    %s -> %s (%s)" (.getName source-pdf) (.getName markdown-path) (name reason))
     :planned (format "plan    %s -> %s + %s" (.getName source-pdf) (.getName markdown-path) (.getName receipt-path))
     :created (format "create  %s -> %s + %s" (.getName source-pdf) (.getName markdown-path) (.getName receipt-path))
     :overwritten (format "write   %s -> %s + %s" (.getName source-pdf) (.getName markdown-path) (.getName receipt-path))
     (format "%s %s" status source-pdf))))

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
      (let [cfg (config/load-config (:config options))
            results (convert/convert-dir! cfg options)]
        (doseq [result results]
          (print-result! result))))))

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
