(ns containru.db
  (:require [monger.core :as mg]
            [monger.collection :as mc]
            [monger.operators :refer :all]
            [crypto.password.bcrypt :as password]))

(def users "users")
(def instances "instances")

(defn create-user [db name pass]
  (try (do
         (mc/insert db users {:_id name :pass
                              (password/encrypt pass)})
         true)
       (catch com.mongodb.DuplicateKeyException e
         false)))

(defn check-user [db name pass]
  (let [user (mc/find-one-as-map db users {:_id name})]
    (password/check pass (user :pass))))

(defn get-instance [db _id]
  (mc/find-one-as-map db instances {:_id _id}))

(defn instance-owner? [db _id user]
  (if-let [instance (get-instance db _id)]
    (= (instance :owner) user)
    false))

(defn create-instance [db {:keys [user port container-id volume-id]}]
  (if (empty? (mc/find-maps db instances {:port port}))
    (mc/insert-and-return db instances
               {:owner user :port port
                :container-id container-id :volume-id volume-id})
    (throw (ex-info (str "port number " port " is in use")
                    {:containru-type :used-port}))))

(defn delete-instance [db _id]
  (mc/remove-by-id db instances _id))

(defn list-instances [db user all?]
  (if all?
    (mc/find-maps db instances {})
    (mc/find-maps db instances {:owner user})))
