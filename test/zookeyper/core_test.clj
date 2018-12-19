(ns zookeyper.core-test
  (:require [clojure.test :refer :all]
            [zookeyper.core :refer :all]
            [ring.mock.request :as mock]))

(deftest routes-return-hello
  (is (= (routes (mock/request :get "/"))
         {:status 200
          :headers {"Content-Type" "text/html"}
          :body "Hello there!"})))
