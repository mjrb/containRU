(ns containru.instance-routes
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [containru.db :as db]
            [containru.container-manager :as cm]
            [containru.volume-manager :as vm]
            [containru.tcp-proxy :as tp]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [buddy.auth.middleware :refer [wrap-authentication]]
            [buddy.auth.backends :as auth-backends])
  (:import org.bson.types.ObjectId))

(defn wrap-require-auth [handler]
  (fn [request]
    (if (nil? (request :identity))
      {:status 401 :body {:err "auth token required"}}
      (handler request))))

(defn wrap-owner-required [dbconn handler]
  (fn [{{_id :_id} :params
        {user :user} :identity :as request}]
    (if (db/instance-owner? dbconn (ObjectId. _id) user)
      (handler (assoc request :instance (db/get-instance dbconn (ObjectId. _id))))
      {:status 401 :body {:err "you are not authorized for that instance"}})))

(defn create-instance [user dbconn containers volumes pass]
  (if (nil? pass)
    {:status 400 :body {:err "pass for root required to create instance"}}
    (try
      (let [vid (vm/create volumes)
            vpath (vm/prepare volumes vid)
            cid (cm/create containers vpath pass)
            port (+ 10000 (rand-int 55536))
            instance (db/create-instance dbconn
                                         {:user user :port port
                                          :container-id cid
                                          :volume-id vid})]
        (tp/proxy containers instance)
        {:status 201 :body {:_id (instance :_id) :owner user :port port}})
      (catch clojure.lang.ExceptionInfo e ;; TODO implement auto retry/ better port assignments
        (if (= ((ex-data e) :containru-type) :used-port)
          {:status 520 :body
           {:err "random port already in use, please try again"}}
          (throw e))))))

(def pubkeys [:owner :_id :port])

(defn instance-routes [{:keys [secret dbconn containers volumes]}]
  (-> (context "/instance" []
               (POST "/" {{user :user} :identity {pass "pass"} :body :as request}
                     (create-instance user dbconn containers volumes pass))
               (GET "/" {{user :user} :identity}
                    {:status 200
                     :body (map #(select-keys % pubkeys)
                                (db/list-instances dbconn user false))})
                        
               (->> (routes
                     (GET "/" {instance :instance :as request}
                          {:status 200 :body (select-keys instance pubkeys)})
                     (DELETE "/" {instance :instance}
                             (db/delete-instance dbconn (instance :_id))
                             (cm/delete containers (instance :container-id))
                             (vm/delete volumes (instance :volume-id))
                             {:status 204}))
                    (wrap-owner-required dbconn)
                    (context "/:_id" [])))
      (routes)
      (wrap-require-auth)
      (wrap-authentication (auth-backends/jws {:secret secret}))
      ))
