(defproject active-grafana "0.1.0-SNAPSHOT"
  :description "Active Grafana: Helper to handle Grafana."
  :url "https://github.com/active-group/active-grafana"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[clj-http "3.12.3"]
                 [de.active-group/active-clojure "0.42.2"]
                 [org.clojure/clojure "1.11.1"]
                 [org.clojure/data.json "2.4.0"]
                 [org.clojure/tools.cli "1.0.219"]]
  :main active-grafana.main)
