(ns zookeyper.core-test
  (:require [clojure.test :refer :all]
            [zookeyper.core :refer :all]
            [ring.mock.request :as mock]
            [zookeeper :as zk]
            [cheshire.core :as json]))

;; Test Helpers

(defn with-zookeeper-state
  "Connect to Zookeeper, run the function and then ensure the connection is closed. The
  root znode will be created and then cleaned up after the function is run. This is
  specificly designed to be used in test cases."
  [hosts root f]
  (let [state (connect hosts :root root)]
    (try
      (f state)
      (finally
        (zk/delete-all (:client state) (:root state))
        (zk/close (:client state))))))

(defn with-zookeeper-test
  "Just like with-zookeeper-state but uses the local test zookeeper and the /test root."
  [f]
  (with-zookeeper-state "127.0.0.1" "/test" f))

(defn mock-store-request [state method body]
  "A test helper to run an JSON HTTP request against the app HTTP handler."
  ((app state) (-> (mock/request method "/store")
                   (mock/json-body body))))

(defn json-response [status body]
  "A test helper to build expected response bodies from the HTTP handlers."
  {:status status
   :headers {"Content-Type" "application/json; charset=utf-8"}
   :body (json/generate-string body)})

;; App HTTP Handler Tests

(deftest store-get-key-found
  (with-zookeeper-test
    (fn [state]
      (create-val state "foo" "bar")
      (is (= (json-response 200 {:val "bar"})
             (mock-store-request state :get {:key "foo"}))))))

(deftest store-get-key-not-found
  (with-zookeeper-test
    (fn [state]
      (is (= (json-response 404 {:error "KeeperErrorCode = NoNode for /test/foo"})
             (mock-store-request state :get {:key "foo"}))))))

(deftest store-delete-found-and-then-not-found
  (with-zookeeper-test
    (fn [state]
      (create-val state "foo" "bar")
      (is (= (json-response 200 {})
             (mock-store-request state :delete {:key "foo"})))
      (is (= (json-response 200 {})
             (mock-store-request state :delete {:key "foo"}))))))

(deftest store-create-and-update
  (with-zookeeper-test
    (fn [state]
      (is (= (json-response 200 {})
             (mock-store-request state :post {:key "foo" :val "bar"})))
      (is (= (json-response 200 {:val "bar"})
             (mock-store-request state :get {:key "foo"})))
      (is (= (json-response 200 {})
             (mock-store-request state :put {:key "foo" :val "lol"})))
      (is (= (json-response 200 {:val "lol"})
             (mock-store-request state :get {:key "foo"}))))))

;; Zookeeper Cache Tests

(deftest refresh-cache-watches-for-child-create-and-delete-events
  (with-zookeeper-test
    (fn [state]
      ; Cache should be empty before fetching from Zookeeper.
      (is (empty? @(:cache state)))

      ; Add three key value pairs.
      (create-val state "one" "1")
      (create-val state "two" "2")
      (create-val state "three" "3")

      ; Ensure they are received by the watch.
      (Thread/sleep 10)
      (is (= {"one" "1" "two" "2" "three" "3"} @(:cache state)))

      ; Delete two of the key value pairs.
      (delete-val state "one")
      (delete-val state "two")

      ; Ensure they are received by the watch.
      (Thread/sleep 10)
      (is (= {"three" "3"} @(:cache state))))))
