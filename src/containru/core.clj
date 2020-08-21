(ns containru.core
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [monger.core :as mg]
            [cheshire.core :as json]
            [cheshire.generate :as cg]
            [containru.db :as db]
            [containru.container-manager :as cm]
            [containru.volume-manager :as vm]
            [containru.user-routes :refer [user-routes]]
            [containru.instance-routes :refer [instance-routes]]
            [containru.tcp-proxy :as tcp-proxy]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [aleph.http :as http])
  (:import org.bson.types.ObjectId)
  (:gen-class))

(cg/add-encoder ObjectId cg/encode-str)



;; for use in repl
(defn init-conf []
  (def conf {:dbconn (mg/get-db (mg/connect) "containru")
   :secret "its free real estate"
   :containers (cm/new-simple (cm/local-containers)
                              (cm/local-networks)
                              "1000")
   :volumes (vm/new-fs "/home/maek/prog/containru/volumes")
   :freq 10000
   :thresh 20000
   :api-port 3000}))


(defn start [conf]
  (def server (http/start-server
               (wrap-json-response
                (wrap-json-body
                 (routes (user-routes conf)
                         (instance-routes conf))))
                {:port (conf :api-port)})))
(defn stop [] (.close server))
(defn restart [] (stop) (start conf))

(defn anti-main []
  (stop)
  (tcp-proxy/proxy-shutdown conf)
  (tcp-proxy/stop-sleep-loop)
  (shutdown-agents))

(defn -main []
  (init-conf)
  (start conf)
  (tcp-proxy/proxy-startup conf)
  (tcp-proxy/start-sleep-loop conf)
  (.addShutdownHook (Runtime/getRuntime)
                    (Thread. ^Runnable anti-main)))

(defn re []
  (require 'containru.db :reload)
  (require 'containru.container-manager :reload)
  (require 'containru.volume-manager :reload)
  (require 'containru.user-routes :reload)
  (require 'containru.instance-routes :reload)
  (require 'containru.tcp-proxy :reload)
  (use 'containru.core :reload))
