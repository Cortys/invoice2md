(ns build
  (:require [clojure.string :as str]
            [clojure.tools.build.api :as b]))

(def app-name "invoice2md")
(def version (str/trim (slurp "VERSION")))
(def class-dir "target/classes")
(def uber-file (format "target/%s-%s-standalone.jar" app-name version))
(def basis (delay (b/create-basis {:project "deps.edn"})))

(defn clean
  [_]
  (b/delete {:path "target"}))

(defn uber
  [_]
  (clean nil)
  (b/copy-dir {:src-dirs ["src" "resources"]
               :target-dir class-dir})
  (b/compile-clj {:basis @basis
                  :src-dirs ["src"]
                  :class-dir class-dir
                  :ns-compile '[invoice2md.core]})
  (b/uber {:basis @basis
           :class-dir class-dir
           :uber-file uber-file
           :main 'invoice2md.core})
  (println "Built" uber-file))
