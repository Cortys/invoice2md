(ns invoice2md.render
  (:require [clojure.string :as str]
            [selmer.parser :as selmer]
            [selmer.util :as selmer-util]))

(defn- normalize-template-output
  [s]
  (-> s
      (str/replace #"\n{3,}" "\n")
      (str/replace #"\n\n---\s*$" "\n---")
      (str/replace #"\s+$" "")
      (str "\n")))

(defn render-string
  [template context]
  (selmer-util/without-escaping
    (selmer/render template context)))

(defn render-basename
  [template context]
  (str/trim (render-string template context)))

(defn render-markdown
  [template context]
  (normalize-template-output (render-string template context)))
