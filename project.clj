(defproject zookeyper "0.1.0"
  :description "A caching key value store on top of Zookeeper."
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [ring/ring-core "1.7.1"]
                 [ring/ring-jetty-adapter "1.7.1"]
                 [compojure "1.6.1"]
                 [ring/ring-mock "0.3.2"]]
  :main ^:skip-aot zookeyper.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
