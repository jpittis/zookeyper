(ns zookeyper.core
  (:require [ring.adapter.jetty :as jetty]
            [compojure.core :refer [defroutes GET]]
            [compojure.route :refer [not-found]])
  (:gen-class))

(defn handler [request]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body "Hello there!"})

(defroutes routes
  (GET "/" [] handler)
  (not-found "404 not found"))

(defn -main
  [port]
  (jetty/run-jetty routes {:port (Integer. port)}))
