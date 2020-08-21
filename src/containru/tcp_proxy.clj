(ns containru.tcp-proxy
  (:require [aleph.tcp :as tcp]
            [manifold.stream :as s]
            [containru.container-manager :as cm]
            [clojure.core.async :as async]
            [containru.db :as db]))


(defn esv [] (tcp/start-server
              (fn [s info]
                (s/connect s s))
              {:port 10001}))

(def ports (atom {}))

(defn sleep-all [containers dbconn]
  (let [instances (db/list-instances dbconn "root" true)]
    (doall (map #(cm/sleep containers (% :container-id)) instances))
    instances))

(defn proxy-shutdown [{:keys [containers dbconn]}]
  (swap! ports (fn [ports] (doall (map
                                   (fn [[port vals]] (.close (vals :server)))
                                   ports))
                 ports))
  (sleep-all containers dbconn)
  (reset! ports {}))

(defn open [port]
  (let [now (System/currentTimeMillis)]
    (swap! ports (fn [ports]
                   (assoc-in ports [port :connections]
                             (conj (get-in ports [port :connections]) now))))
    (prn "open" now port @ports)
    now))

(defn close [connect-time port]
  (let [now (System/currentTimeMillis)]
    (swap! ports (fn [ports]
                   (-> (assoc-in ports [port :connections]
                                 (disj (get-in ports [port :connections])
                                       connect-time))
                       (assoc-in [port :last-disconnect] now))))
    (prn "close" connect-time port @ports)
    now))

(defn proxy-server [containers ip cid port]
  (tcp/start-server
   (fn [client info]
     (let [connect-time (open port)]
       (cm/prepare containers cid)
       (let [server @(tcp/client {:host ip :port 3306})]
         (s/connect client server)
         (s/connect server client)
         (s/on-closed client #(close connect-time port)))))
   {:port port}))

(defn proxy [containers {cid :container-id port :port}]
  (let [ip (cm/get-ip containers cid)]
    (swap! ports assoc port {:cid cid :connections #{}
                             :server (proxy-server containers ip cid port)})))

(defn proxy-startup [{:keys [containers dbconn]}]
  (let [instances (sleep-all containers dbconn)]
    (doall (map #(proxy containers %) instances))))

(defn should-sleep [threash]
  (fn [[port {:keys [connections last-disconnect]}]]
    (and
     (some? last-disconnect)
     (= 0 (count connections))
     (> (- (System/currentTimeMillis) last-disconnect) threash))))

(defn sleep [containers]
  (fn [[port {cid :cid :as values}]]
    (cm/sleep containers cid)
    [port (dissoc values :last-disconnect)]))

(defn sleep-necissary [containers thresh]
  (fn [ports]
    (let [targets (->> (filter (should-sleep thresh) ports)
                       (map (sleep containers)))
          others (filter (complement (should-sleep thresh))
                         ports)
          result (into {} (conj targets others))]
      (prn "\n sleep-necissary result" result)
      result)))

(def close-sleep-chan (async/chan))
(defn start-sleep-loop [{:keys [containers freq thresh]}]
  (async/go (loop []
      (let [tout (async/timeout freq)
            [v ch] (async/alts! [close-sleep-chan tout])]
        (if (nil? v)
          (do
            (prn "purge" (= ch tout))
            (swap! ports (sleep-necissary containers thresh))
            (recur))
          (prn "sleep-loop stoped" v ch))))))
(defn stop-sleep-loop [] (async/go (async/>! close-sleep-chan "stop")))
;;(tcp-proxy/stop-sleep-loop)
