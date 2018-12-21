(ns zookeyper.core
  (:require [ring.adapter.jetty :as jetty]
            [compojure.core :refer [GET POST DELETE PUT]]
            [compojure.route :refer [not-found]]
            [zookeeper :as zk]
            [zookeeper.data :as data])
  (:gen-class))

(defn create-handler
  [state]
  (fn [request]
    {:status 200
     :headers {"Content-Type" "text/html"}
     :body "Create!"}))

(defn update-handler
  [state]
  (fn [request]
    {:status 200
     :headers {"Content-Type" "text/html"}
     :body "Update!"}))

(defn delete-handler
  [state]
  (fn [request]
    {:status 200
     :headers {"Content-Type" "text/html"}
     :body "Delete!"}))

(defn get-handler
  [state]
  (fn [request]
    {:status 200
     :headers {"Content-Type" "text/html"}
     :body "Get!"}))

(defn routes [state]
  (compojure.core/routes
    (GET    "/store" [] (get-handler    state))
    (POST   "/store" [] (create-handler state))
    (DELETE "/store" [] (delete-handler state))
    (PUT    "/store" [] (update-handler state))))

(defn connect
  [hosts & {:keys [root], :or {root "/zookeyper"}}]
  (let [client (zk/connect hosts)]
    (when-not (zk/exists client root) (zk/create client root))
    {:client client
     :root root}))

(defn -main
  [port hosts]
  (println "Listening...")
  (jetty/run-jetty (routes (connect hosts)) {:port (Integer. port)}))

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
