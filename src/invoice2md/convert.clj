(ns invoice2md.convert
  (:require [clojure.java.io :as io]
            [invoice2md.extract :as extract]
            [invoice2md.paths :as paths]
            [invoice2md.pdf :as pdf]
            [invoice2md.render :as render]))

(defn- conversion-context
  [config pdf-file]
  (let [sources (try
                  (pdf/extract-sources pdf-file)
                  (catch Exception e
                    (throw (ex-info (str "Could not read PDF: " (.getPath pdf-file))
                                    {:source-pdf pdf-file}
                                    e))))
        fields (try
                 (extract/extract-fields sources (:fields config))
                 (catch Exception e
                   (throw (ex-info (str "Could not extract fields from PDF: " (.getPath pdf-file))
                                   {:source-pdf pdf-file
                                    :pdf-sources sources}
                                   e))))]
    {:context (merge fields (:static config) {:source_pdf (.getPath pdf-file)})
     :extracted-fields fields}))

(defn- filename-template
  [config key]
  (or (get config key) (:filename config)))

(defn- planned-conversion
  [config opts pdf-file]
  (let [{:keys [context extracted-fields]} (conversion-context config pdf-file)
        markdown-basename (render/render-basename (filename-template config :markdown_filename) context)
        pdf-basename (render/render-basename (filename-template config :pdf_filename) context)
        context (assoc context
                       :basename markdown-basename
                       :markdown_basename markdown-basename
                       :pdf_basename pdf-basename)
        targets (paths/target-paths {:markdown-dir (:markdown-dir opts)
                                      :receipt-dir (:receipt-dir opts)
                                      :markdown-basename markdown-basename
                                      :pdf-basename pdf-basename})]
    (merge {:source-pdf pdf-file
            :basename markdown-basename
            :markdown-basename markdown-basename
            :pdf-basename pdf-basename
            :extracted-fields extracted-fields
            :context context
            :markdown (render/render-markdown (:markdown config) context)}
           targets)))

(defn- write-conversion!
  [{:keys [source-pdf markdown-path receipt-path markdown]} overwrite?]
  (paths/ensure-dir! (.getParentFile (io/file markdown-path)))
  (spit markdown-path markdown)
  (paths/copy-file! source-pdf receipt-path overwrite?))

(defn convert-file!
  [config opts pdf-file]
  (let [overwrite? (boolean (:overwrite opts))
        dry-run? (boolean (:dry-run opts))
        plan (planned-conversion config opts pdf-file)
        markdown-exists? (.exists (io/file (:markdown-path plan)))]
    (cond
      (and markdown-exists? (not overwrite?))
      (assoc plan :status :skipped :reason :markdown-exists)

      dry-run?
      (assoc plan :status :planned)

      :else
      (do
        (write-conversion! plan overwrite?)
        (assoc plan :status (if markdown-exists? :overwritten :created))))))

(defn convert-dir!
  [config opts]
  (paths/ensure-dir! (:markdown-dir opts))
  (paths/ensure-dir! (:receipt-dir opts))
  (mapv (fn [pdf-file]
          (try
            (convert-file! config opts pdf-file)
            (catch Exception e
              {:status :failed
               :source-pdf pdf-file
               :error e})))
        (or (:pdf-files opts) (paths/pdf-files (:pdf-dir opts)))))
