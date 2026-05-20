(ns invoice2md.pdf
  (:import [org.apache.pdfbox Loader]
           [org.apache.pdfbox.pdmodel PDDocumentInformation]
           [org.apache.pdfbox.text PDFTextStripper]))

(defn extract-text
  [pdf-path]
  (with-open [document (Loader/loadPDF (java.io.File. (str pdf-path)))]
    (.getText (PDFTextStripper.) document)))

(defn- document-metadata
  [^PDDocumentInformation info]
  {:title (.getTitle info)
   :author (.getAuthor info)
   :subject (.getSubject info)
   :keywords (.getKeywords info)
   :creator (.getCreator info)
   :producer (.getProducer info)})

(defn extract-sources
  [pdf-path]
  (with-open [document (Loader/loadPDF (java.io.File. (str pdf-path)))]
    {:content (.getText (PDFTextStripper.) document)
     :metadata (document-metadata (.getDocumentInformation document))}))
