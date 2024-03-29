(defproject de.active-group/active-grafana "0.1.0-SNAPSHOT"
  :description "Active Grafana: Helper to handle Grafana."
  :url "https://github.com/active-group/active-grafana"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.babashka/http-client "0.3.11"]
                 [org.clj-commons/clj-http-lite "1.0.13"]
                 [org.clojure/clojure "1.11.1"]
                 [org.clojure/data.json "2.4.0"]
                 [org.clojure/tools.cli "1.0.219"]
                 ; only needed for pod
                 [nrepl/bencode "1.1.0"]
                 [instaparse/instaparse  "1.4.12"]
                                           ; 1.0.333
                 [com.cognitect/transit-clj "1.0.329"]
                 ; only needed for graalvm
                 [com.github.clj-easy/graal-build-time "1.0.5"]]
  :profiles {:uberjar   {:aot :all}

             :as-pod    {:main active-grafana.pod
                         :uberjar-name "active-grafana-pod.jar"}
             :as-copy   {:main active-grafana.main-copy
                         :uberjar-name "active-grafana-copy.jar"}
             :as-adjust {:main active-grafana.main-adjust
                         :uberjar-name "active-grafana-adjust.jar"}})
