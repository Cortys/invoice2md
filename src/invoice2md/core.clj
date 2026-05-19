(ns invoice2md.core
  (:require [invoice2md.cli :as cli])
  (:gen-class))

(defn -main
  [& args]
  (cli/dispatch args))
