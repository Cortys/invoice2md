(ns invoice2md.pdf
  (:import [org.apache.pdfbox Loader]
           [org.apache.pdfbox.text PDFTextStripper]))

(defn extract-text
  [pdf-path]
  (with-open [document (Loader/loadPDF (java.io.File. (str pdf-path)))]
    (.getText (PDFTextStripper.) document)))
