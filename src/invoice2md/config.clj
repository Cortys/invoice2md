(ns invoice2md.config
  (:require [clj-yaml.core :as yaml]
            [clojure.java.io :as io]
            [clojure.walk :as walk]))

(defn load-config
  [path]
  (with-open [reader (io/reader path)]
    (walk/keywordize-keys (yaml/parse-stream reader))))
