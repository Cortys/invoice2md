(ns invoice2md.convert
  (:require [clojure.java.io :as io]
            [invoice2md.extract :as extract]
            [invoice2md.paths :as paths]
            [invoice2md.pdf :as pdf]
            [invoice2md.render :as render]))

(defn- conversion-context
  [config pdf-file]
  (let [sources (pdf/extract-sources pdf-file)
        fields (extract/extract-fields sources (:fields config))]
    (merge fields (:static config) {:source_pdf (.getPath pdf-file)})))

(defn- filename-template
  [config key]
  (or (get config key) (:filename config)))

(defn- planned-conversion
  [config opts pdf-file]
  (let [context (conversion-context config pdf-file)
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
  (mapv #(convert-file! config opts %) (paths/pdf-files (:pdf-dir opts))))
