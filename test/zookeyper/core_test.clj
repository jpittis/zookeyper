(ns zookeyper.core-test
  (:require [clojure.test :refer :all]
            [zookeyper.core :refer :all]
            [ring.mock.request :as mock]
            [zookeeper :as zk]
            [clojure.data.json :as json]))

(defn with-zookeeper
  "Connect to Zookeeper, run the function and then ensure the connection is closed. The
  root znode will be created and then cleaned up after the function is run. This is
  specificly designed to be used in test cases."
  [hosts root f]
  (let [client (zk/connect hosts)]
     (try (if (zk/exists client root)
            (throw (Exception. "root node already exists"))
            (do
              (zk/create client root :persistent? true)
              (f client)))
          (finally
            (zk/delete-all client root)
            (zk/close client)))))

(defn with-zookeeper-state
  "Just like with-zookeeper but upgrades the client to a state object used by the
  application handlers."
  [hosts root f]
  (with-zookeeper hosts root
    (fn [client]
      (let [state {:client client :root root}]
        (f state)))))

(deftest store-get-found
  (with-zookeeper-state "127.0.0.1" "/test"
    (fn [state]
      (create-val (:client state) "/test/foo" "bar")
      (is (= ((app state) (-> (mock/request :get "/store")
                              (mock/json-body {:key "foo"})))
             {:status 200
              :headers {"Content-Type" "application/json; charset=utf-8"}
              :body (json/write-str {:val "bar"})})))))

(deftest store-delete
  (is (= ((app nil) (-> (mock/request :delete "/store")
                        (mock/json-body {:key "foo"})))
         {:status 200
          :headers {"Content-Type" "application/json; charset=utf-8"}
          :body (json/write-str {:key "foo"})})))

(deftest store-create
  (is (= ((app nil) (-> (mock/request :post "/store")
                        (mock/json-body {:key "foo" :val "bar"})))
         {:status 200
          :headers {"Content-Type" "application/json; charset=utf-8"}
          :body (json/write-str {:key "foo" :val "bar"})})))

(deftest store-update
  (is (= ((app nil) (-> (mock/request :put "/store")
                        (mock/json-body {:key "foo" :val "bar"})))
         {:status 200
          :headers {"Content-Type" "application/json; charset=utf-8"}
          :body (json/write-str {:key "foo" :val "bar"})})))

(deftest zookeeper-exists
  (is (with-zookeeper "127.0.0.1" "/test"
        (fn [client] (zk/exists client "/test")))))

(deftest zookeeper-not-exists
  (is (not (with-zookeeper "127.0.0.1" "/test"
        (fn [client] (zk/exists client "/foo"))))))

(deftest create-update-get-delete
  (with-zookeeper "127.0.0.1" "/test"
    (fn [client]
      (is (create-val client "/test/one" "un"))
      (is (= (get-val client "/test/one") "un"))
      (is (update-val client "/test/one" "uno"))
      (is (= (get-val client "/test/one") "uno"))
      (is (delete-val client "/test/one")))))
