(ns zookeyper.core
  (:require [ring.adapter.jetty :as jetty]
            [compojure.core :refer [GET POST DELETE PUT]]
            [compojure.route :refer [not-found]]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [ring.util.response :refer [response]]
            [zookeeper :as zk]
            [zookeeper.data :as data])
  (:gen-class))

(defn create-handler
  [state]
  (fn [request]
    (response (:body request))))

(defn update-handler
  [state]
  (fn [request]
    (response (:body request))))

(defn delete-handler
  [state]
  (fn [request]
    (response (:body request))))

(defn get-handler
  [state]
  (fn [request]
    (response (:body request))))

(defn routes [state]
  (compojure.core/routes
    (GET    "/store" [] (get-handler    state))
    (POST   "/store" [] (create-handler state))
    (DELETE "/store" [] (delete-handler state))
    (PUT    "/store" [] (update-handler state))))

(defn app [state]
  (-> (routes state)
      wrap-json-response
      wrap-json-body))

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
