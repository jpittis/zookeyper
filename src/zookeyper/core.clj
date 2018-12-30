(ns zookeyper.core
  (:require [ring.adapter.jetty :as jetty]
            [compojure.core :refer [GET POST DELETE PUT]]
            [compojure.route :refer [not-found]]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [ring.util.response :refer [response]])
  (:gen-class))

(load "zookeeper")

; TODO: All these handlers have similar logic. We can probably factor them out.

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

(defn -main
  ; TODO: Currently we crash with a terrible error message if you don't provide a port and
  ; a host list. We should probably use some kind of command line flag library.
  [port hosts]
  (let [state (connect hosts)]
    (println "Listening...")
    (jetty/run-jetty (app state) {:port (Integer. port)})))
