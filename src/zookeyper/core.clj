(ns zookeyper.core
  (:require [ring.adapter.jetty :as jetty]
            [compojure.core :refer [GET POST DELETE PUT]]
            [compojure.route :refer [not-found]]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [ring.util.response :refer [response]]
            [zookeeper :as zk]
            [zookeeper.data :as data])
  (:gen-class))

(defn namespace-key [state k] (str (:root state) "/" k))

(defn create-val
  [state k v]
  (let [namespaced-key (namespace-key state k)]
    (zk/create (:client state) namespaced-key :persistent? true :data (.getBytes v "UTF-8"))))

(defn update-val
  [state k v]
  (let [namespaced-key (namespace-key state k)]
    (zk/set-data (:client state) namespaced-key (.getBytes v "UTF-8") -1)))

(defn get-val
  [state k]
  (let [namespaced-key (namespace-key state k)]
    (data/to-string (:data (zk/data (:client state) namespaced-key)))))

(defn delete-val
  [state k]
  (let [namespaced-key (namespace-key state k)]
    (zk/delete (:client state) namespaced-key)))

(defn create-handler
  [state]
  (fn [request]
    (try
      (let [k ((:body request) "key")
            v ((:body request) "val")]
        (do (create-val state k v)
            (response {})))
      (catch Exception e
        (-> (response {:error (.getMessage e)})
            (assoc :status 404))))))

(defn update-handler
  [state]
  (fn [request]
    (try
      (let [k ((:body request) "key")
            v ((:body request) "val")]
        (do (update-val state k v)
            (response {})))
      (catch Exception e
        (-> (response {:error (.getMessage e)})
            (assoc :status 404))))))

(defn delete-handler
  [state]
  (fn [request]
    (try
      (let [k ((:body request) "key")
            v (delete-val state k)]
        (response {}))
      (catch Exception e
        (-> (response {:error (.getMessage e)})
            (assoc :status 404))))))

(defn get-handler
  [state]
  (fn [request]
    (try
      (let [k ((:body request) "key")
            v (get-val state k)]
        (response {:val v}))
      (catch Exception e
        (-> (response {:error (.getMessage e)})
            (assoc :status 404))))))

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
    {:client client
     :root root}))

(defn -main
  [port hosts]
  (let [state (connect hosts)]
    (when-not (zk/exists (:client state) (:root state))
      (zk/create (:client state) (:root state)))
    (println "Listening...")
    (jetty/run-jetty (app state) {:port (Integer. port)})))
