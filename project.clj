(defproject containru "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :main containru.core
  :profiles {:uberjar {:aot :all}}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [aleph "0.4.6"]
                 [compojure "1.6.2"]
                 [com.novemberain/monger "3.1.0"]
                 [lispyclouds/clj-docker-client "1.0.1"]
                 [cheshire "5.10.0"]
                 [crypto-password "0.2.1"]
                 [buddy/buddy-sign "1.1.0"]
                 [buddy/buddy-auth "2.2.0"]
                 [ring/ring-json "0.5.0"]
                 [org.clojure/core.async "1.3.610"]]
  :repl-options {:init-ns containru.core
                 :timeout 120000})
