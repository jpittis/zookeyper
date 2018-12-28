(ns zookeyper.core
  (:require [zookeeper :as zk]
            [zookeeper.data :as data]))

(defn ensure-root-exists-or-create
  [state]
  (when-not (zk/exists (:client state) (:root state))
    (zk/create (:client state) (:root state))))

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

(defn refresh-cache-from-zookeeper
  "Atomically replaces cache with the current state of key values found in Zookeeper."
  [state]
  (let [children (zk/children (:client state) (:root state))
        updated (if children
                  (->> children
                       ; TODO: Possible race if delete happens during map.
                       (map (fn [k] {k (get-val state k)}))
                       (into {}))
                  ; No children exist so cache is empty.
                  {})]
    (reset! (:cache state) updated)))

(defn connect
  "Connect wraps a Zookeeper connection into a state object which is then passed around to
  all functions that operate on Zookeeper."
  [hosts & {:keys [root], :or {root "/zookeyper"}}]
  (let [client (zk/connect hosts)
        state {:client client
               :root root
               :cache (atom {})}]
    (refresh-cache-from-zookeeper state)
    state))
