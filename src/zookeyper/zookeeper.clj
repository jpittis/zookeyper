(ns zookeyper.core
  (:require [zookeeper :as zk]
            [zookeeper.data :as data]))

(defn ensure-root-exists-or-create
  [state]
  (when-not (zk/exists (:client state) (:root state))
    (zk/create (:client state) (:root state) :persistent? true)))

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

(defn data-watcher
  "Watches for NodeDataChanged events for keys in the store."
  [state]
  (fn [event]
    (when (= (:event-type event) :NodeDataChanged)
      (let [path (:path event)
            k (clojure.string/replace path (str (:root state) "/") "")
            data (data/to-string
                   (:data
                     (zk/data (:client state) path :watcher (data-watcher state))))]
        (swap! (:cache state) assoc k data)))))


(defn children-watcher
  "Watches for NodeChildrenChanged events that signify a new key in the store."
  [state]
  (fn [event]
    (let [children (zk/children (:client state) (:root state) :watcher (children-watcher state))]
      (when (= (:event-type event) :NodeChildrenChanged)
        (let [[created-keys deleted-keys _] (clojure.data/diff (set children)
                                                               (set (keys @(:cache state))))]
          (do
            (when-not (empty? created-keys)
              (->> created-keys
                   (map (fn [child]
                          {child
                           (data/to-string
                             (:data
                               (zk/data (:client state) (str (:root state) "/" child)
                                        :watcher (data-watcher state))))}))
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
                       ; TODO: Possible race if delete happens during map.
                       ; TODO: Probably shouldn't use get-val because I'm going to add
                       ; caching at that layer. Maybe a no-cache flag?
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
    (ensure-root-exists-or-create state)
    (refresh-cache-from-zookeeper state)
    state))
