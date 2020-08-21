(ns containru.container-manager
  (:require [clj-docker-client.core :as docker]
            [clojure.string :as str]))
(defn die [o] (throw (Exception. (str o))))

(defprotocol ContainerManager
  (create [this volume-path pass]) ;; return id
  (prepare [this id]) ;; get cotnainer ready for use
  (sleep [this id]) ;; sleep container
  (delete [this id]) ;; deletes the container
  (get-ip [this id])) ;; gets the ip of a container

(deftype SimpleContainerManager [containers networks user]
  ContainerManager
  (create [this volume-path pass]
    (let [binds [(str volume-path ":/var/lib/mysql")]
          env [(str "MYSQL_ROOT_PASSWORD=" pass)] ;; TODO BAD have user set password. potentially encrypted in memory
          container (docker/invoke containers
                                   {:op :ContainerCreate
                                    :params {:body {:Image "mariadb"
                                                    :Binds binds
                                                    :Env env
                                                    :User user
                                                    :PublishAllPorts false}}})]
      (docker/invoke containers
                     {:op :ContainerStart :params {:id (container :Id)}})
      (sleep this (container :Id))
      (container :Id)))
  (prepare [this id]
    (docker/invoke containers {:op :ContainerUnpause :params {:id id}}))
  (sleep [this id]
    (docker/invoke containers {:op :ContainerPause :params {:id id}}))
  (delete [this id]
    (docker/invoke containers {:op :ContainerStop :params {:id id}})
    (docker/invoke containers {:op :ContainerDelete :params {:id id}}))
  (get-ip [this id]
    (-> (docker/invoke networks {:op :NetworkInspect :params {:id "bridge"}})
        (get-in [:Containers (keyword id) :IPv4Address])
        (str/split #"/")
        (nth 0)
        )))

(defn new-simple [containers networks user]
  (SimpleContainerManager. containers networks user))

(defn local-containers []
  (docker/client {:category :containers
                  :conn {:uri "unix:///var/run/docker.sock"}}))
(defn local-networks []
  (docker/client {:category :networks
                  :conn {:uri "unix:///var/run/docker.sock"}}))
