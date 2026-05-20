(ns invoice2md.paths
  (:require [clojure.java.io :as io]
            [clojure.string :as str])
  (:import [java.nio.file Files StandardCopyOption]))

(defn pdf-file?
  [file]
  (and (.isFile file)
       (str/ends-with? (str/lower-case (.getName file)) ".pdf")))

(defn pdf-files
  ([dir]
   (pdf-files dir false))
  ([dir recursive?]
   (->> (if recursive?
          (file-seq (io/file dir))
          (or (seq (.listFiles (io/file dir))) []))
       (filter pdf-file?)
       (sort-by #(.getPath %)))))

(defn ensure-dir!
  [dir]
  (.mkdirs (io/file dir)))

(defn target-paths
  [{:keys [markdown-dir receipt-dir markdown-basename pdf-basename]}]
  {:markdown-path (io/file markdown-dir (str markdown-basename ".md"))
   :receipt-path (io/file receipt-dir (str pdf-basename ".pdf"))})

(defn copy-file!
  [source target overwrite?]
  (ensure-dir! (.getParentFile (io/file target)))
  (if overwrite?
    (Files/copy (.toPath (io/file source))
                (.toPath (io/file target))
                (into-array StandardCopyOption [StandardCopyOption/REPLACE_EXISTING]))
    (Files/copy (.toPath (io/file source))
                (.toPath (io/file target))
                (make-array StandardCopyOption 0))))
