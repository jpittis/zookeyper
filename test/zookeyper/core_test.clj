(ns zookeyper.core-test
  (:require [clojure.test :refer :all]
            [zookeyper.core :refer :all]
            [ring.mock.request :as mock]
            [zookeeper :as zk]))

(deftest store-get
  (is (= ((routes nil) (mock/request :get "/store"))
         {:status 200
          :headers {"Content-Type" "text/html"}
          :body "Get!"})))

(deftest store-delete
  (is (= ((routes nil) (mock/request :delete "/store"))
         {:status 200
          :headers {"Content-Type" "text/html"}
          :body "Delete!"})))

(deftest store-create
  (is (= ((routes nil) (mock/request :post "/store"))
         {:status 200
          :headers {"Content-Type" "text/html"}
          :body "Create!"})))

(deftest store-create
  (is (= ((routes nil) (mock/request :put "/store"))
         {:status 200
          :headers {"Content-Type" "text/html"}
          :body "Update!"})))

;; Zookeeper scratchpad.

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
