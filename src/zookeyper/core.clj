(ns zookeyper.core
  (:require [ring.adapter.jetty :as jetty]
            [compojure.core :refer [defroutes GET]]
            [compojure.route :refer [not-found]]
            [zookeeper :as zk]
            [zookeeper.data :as data])
  (:gen-class))

(defn handler
  [request]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body "Hello there!"})

(defroutes routes
  (GET "/" [] handler)
  (not-found "404 not found"))

(declare zk-client)

(def root-path "/zookeyper/")

(defn -main
  [port hosts]
  (def zk-client (zk/connect hosts))
  (try
    (when-not (zk/exists zk-client root-path) (zk/create zk-client root-path))
    (jetty/run-jetty routes {:port (Integer. port)})
    (finally (zk/close zk-client))))

(defn create-val
  [client k v]
  (zk/create client k :persistent? true :data (.getBytes v "UTF-8")))

(defn update-val
  [client k v]
  (zk/set-data client k (.getBytes v "UTF-8") -1))

(defn get-val
  [client k]
  (data/to-string (:data (zk/data client k))))

(defn delete-val
  [client k]
  (zk/delete client k))
