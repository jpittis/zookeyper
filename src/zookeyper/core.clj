(ns zookeyper.core
  (:require [ring.adapter.jetty :as jetty]
            [clojure.tools.cli :refer [parse-opts]]
            [clojure.string :as string])
  (:gen-class))

(load "zookeeper") ; Interacting with Zookeeper.
(load "app")       ; HTTP handlers.

(def cli-options
  [["-p" "--port PORT" "Listen port number"
    :default 3333
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 0 % 0x10000) "Must be a number between 0 and 65536"]]
   ["-z" "--zk-hosts HOSTS" "Zookeeper comma separated hosts"
    :default "127.0.0.1:2181"]
   ["-h" "--help"]])

(defn usage [summary errors]
  (let [usage-message ["Usage: zookeyper <options>"
                       "options:"
                       summary]]
    (string/join \newline (concat usage-message errors))))

(defn exit [status msg]
  (println msg)
  (System/exit status))

(defn parse-options
  [args]
  (let [{:keys [options _ errors summary]} (parse-opts args cli-options)]
    (cond
      (:help options) (exit 0 (usage summary errors))
      errors          (exit 1 (usage summary errors))
      :else           options)))

(defn -main
  [& args]
  (let [options (parse-options args)
        state (connect (:zk-hosts options))]
    (printf "Connected to %s...\n" (:zk-hosts options))
    (printf "Listening on port %s...\n" (:port options))
    (flush) ; printf always buffers output so it must be flushed.
    (jetty/run-jetty (app state) {:port (Integer. (:port options))})))
