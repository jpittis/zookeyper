(ns zookeyper.core-test
  (:require [clojure.test :refer :all]
            [zookeyper.core :refer :all]
            [ring.mock.request :as mock]
            [zookeeper :as zk]
            [cheshire.core :as json]))

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

(defn with-zookeeper-test
  "Just like with-zookeeper-state but uses the local test zookeeper and the /test root."
  [f]
  (with-zookeeper-state "127.0.0.1" "/test" f))

(defn mock-store-request [state method body]
  ((app state) (-> (mock/request method "/store")
                   (mock/json-body body))))

(defn json-response [status body]
  {:status status
   :headers {"Content-Type" "application/json; charset=utf-8"}
   :body (json/generate-string body)})

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
