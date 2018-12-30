(ns zookeyper.core
  (:require [zookeeper :as zk]
            [zookeeper.data :as data]
            [clojure.data :refer [diff]]))

(defn ensure-root-exists-or-create
  [state]
  (when-not (zk/exists (:client state) (:root state))
    (zk/create (:client state) (:root state) :persistent? true)))

(defn namespace-key [state k] (str (:root state) "/" k))

(defn get-data
  [state k & {:keys [watcher]}]
  (data/to-string (:data (zk/data (:client state) k :watcher watcher))))

(defn create-val [state k v]
  (zk/create (:client state) (namespace-key state k) :persistent? true :data (.getBytes v "UTF-8")))

(defn update-val [state k v]
    (zk/set-data (:client state) (namespace-key state k) (.getBytes v "UTF-8") -1))

(defn get-val [state k]
  (if-let [cached-val (@(:cache state) k)]
    cached-val
    (get-data state (namespace-key state k))))

(defn delete-val [state k]
  (zk/delete (:client state) (namespace-key state k)))

(defn data-watcher
  "Watches for NodeDataChanged events for keys in the store."
  [state]
  (fn [event]
    (when (= (:event-type event) :NodeDataChanged)
      (let [path (:path event)
            k (clojure.string/replace path (str (:root state) "/") "")
            data (get-data state path :watcher (data-watcher state))]
        (swap! (:cache state) assoc k data)))))


(defn children-watcher
  "Watches for NodeChildrenChanged events that signify a new key in the store."
  [state]
  (fn [event]
    (let [children (zk/children (:client state) (:root state) :watcher (children-watcher state))]
      (when (= (:event-type event) :NodeChildrenChanged)
        (let [[created-keys deleted-keys _] (diff (set children)
                                                  (set (keys @(:cache state))))]
          (do
            (when-not (empty? created-keys)
              (->> created-keys
                   (map (fn [child]
                          {child (get-data state (str (:root state) "/" child)
                                           :watcher (data-watcher state))}))
                   (into {})
                   (swap! (:cache state) merge)))
            (when-not (empty? deleted-keys)
              (swap! (:cache state) #(apply dissoc % deleted-keys)))))))))

(defn refresh-cache-from-zookeeper
  "Atomically replaces cache with the current state of key values found in Zookeeper."
  [state]
  (let [children (zk/children (:client state) (:root state) :watcher (children-watcher state))
        updated (if children
                  (->> children
                       ; TODO: Possible race if delete happens during map. The easiest fix
                       ; is to have this not raise an exception when a value is not found.
                       (map (fn [k] {k (get-data state (namespace-key state k)
                                                 :watcher (data-watcher state))}))
                       (into {}))
                  ; No children exist so cache is empty.
                  {})]
    (reset! (:cache state) updated)))

(defn connect
  "Connect wraps a Zookeeper connection into a state object which is then passed around to
  all functions that operate on Zookeeper."
  [hosts & {:keys [root] :or {root "/zookeyper"}}]
  (let [client (zk/connect hosts)
        state {:client client
               :root root
               :cache (atom {})}]
    (ensure-root-exists-or-create state)
    (refresh-cache-from-zookeeper state)
    state))
