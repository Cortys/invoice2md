(ns invoice2md.convert
  (:require [clojure.java.io :as io]
            [invoice2md.extract :as extract]
            [invoice2md.paths :as paths]
            [invoice2md.pdf :as pdf]
            [invoice2md.render :as render]))

(defn- conversion-context
  [config pdf-file]
  (let [text (pdf/extract-text pdf-file)
        fields (extract/extract-fields text (:fields config))]
    (merge fields (:static config) {:source_pdf (.getPath pdf-file)})))

(defn- planned-conversion
  [config opts pdf-file]
  (let [context (conversion-context config pdf-file)
        basename (render/render-basename (:filename config) context)
        context (assoc context :basename basename)
        targets (paths/target-paths {:markdown-dir (:markdown-dir opts)
                                     :receipt-dir (:receipt-dir opts)
                                     :basename basename})]
    (merge {:source-pdf pdf-file
            :basename basename
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
