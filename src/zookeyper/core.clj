(ns zookeyper.core
  (:require [ring.adapter.jetty :as jetty])
  (:gen-class))

(load "zookeeper") ; Interacting with Zookeeper.
(load "app")       ; HTTP handlers.

(defn -main
  ; TODO: Currently we crash with a terrible error message if you don't provide a port and
  ; a host list. We should probably use some kind of command line flag library.
  [port hosts]
  (let [state (connect hosts)]
    (println "Listening...")
    (jetty/run-jetty (app state) {:port (Integer. port)})))
