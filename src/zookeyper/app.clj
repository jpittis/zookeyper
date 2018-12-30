(ns zookeyper.core
  (:require [compojure.core :refer [GET POST DELETE PUT]]
            [compojure.route :refer [not-found]]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [ring.util.response :refer [response]]))

(defn exception-to-status [e]
  (cond
    (= (type e) org.apache.zookeeper.KeeperException$NoNodeException) 404
    :else 500))

(defn exceptions-to-json [handler]
  (fn [request]
    (try
      (handler request)
      (catch Exception e
        (-> (response {:error (.getMessage e)})
            (assoc :status (exception-to-status e)))))))

(defn create-handler [state]
  (fn [request]
    (let [k ((:body request) "key")
          v ((:body request) "val")]
      (create-val state k v)
      (response {}))))

(defn update-handler [state]
  (fn [request]
    (let [k ((:body request) "key")
          v ((:body request) "val")]
      (update-val state k v)
      (response {}))))

(defn delete-handler [state]
  (fn [request]
    (let [k ((:body request) "key")
          v (delete-val state k)]
      (response {}))))

(defn get-handler [state]
  (fn [request]
    (let [k ((:body request) "key")
          v (get-val state k)]
      (response {:val v}))))

(defn routes [state]
  (compojure.core/routes
    (GET    "/store" [] (get-handler    state))
    (POST   "/store" [] (create-handler state))
    (DELETE "/store" [] (delete-handler state))
    (PUT    "/store" [] (update-handler state))))

(defn app [state]
  (-> (routes state)
      exceptions-to-json
      wrap-json-response
      wrap-json-body))
