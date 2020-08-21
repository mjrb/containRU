(ns containru.user-routes
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [containru.db :as db]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [buddy.sign.jwt :as jwt]))

(defn user-routes [{:keys [dbconn secret]}]
  (-> (context "/user" []
               (POST "/" {{user "user" pass "pass"} :body :as request}
                     (throw (ex-info "here" request))
                     (if (or (nil? "user") (nil? pass))
                       {:status 400 :body {:err "user and pass required"}}
                       (if (db/create-user dbconn user pass)
                         {:status 204}
                         {:status 409 :body {:err "user already exists"}})))
               (POST "/login" {{user "user" pass "pass"} :body}
                     (if (or (nil? "user") (nil? pass))
                       (if (db/check-user dbconn user pass)
                         {:status 200 :body {:token (jwt/sign {:user user} secret)}}
                         {:status 401 :body {:err "bad username or pass"}})))
               (route/not-found "couldn't find that :("))
      (routes)))
