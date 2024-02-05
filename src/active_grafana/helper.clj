(ns active-grafana.helper
  (:require [clojure.data.json :as json]
            [clojure.pprint :as pprint]))

(set! *warn-on-reflection* true)

;; BB communication

(defn fresh-memory
  []
  (atom ""))

(defonce memory (fresh-memory))

(defn forget!
  []
  (reset! memory ""))

 (defn communicate!
   [msg]
   (if (System/getenv "BABASHKA_POD")
     (swap! memory str "\n" msg "\n")
     (println msg)))

(defn print-table!
  ([ks rows]
   (if (System/getenv "BABASHKA_POD")
     (swap! memory str "\n" (with-out-str (pprint/print-table ks rows)))
     (pprint/print-table ks rows)))
  ([rows]
   (if (System/getenv "BABASHKA_POD")
     (swap! memory str "\n" (with-out-str (pprint/print-table rows)))
     (pprint/print-table rows))))

(defn error-logic
  []
  (if (System/getenv "BABASHKA_POD")
    (throw (ex-info "grafana-active: errors in opts-map" {}))
    (System/exit -1)))

;; LOG/DEBUG

(defn log
  [log-str]
  (communicate! log-str))

(defn debug
  [deb]
  (prn deb)
  deb)

;; JSON/CLJ

(defn json->clj
  [request]
  (json/read-str (:body request)))

;; json from clojure map
(defn clj->json
  [body]
  (json/write-str body))
