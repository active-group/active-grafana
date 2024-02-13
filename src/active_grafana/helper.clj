(ns active-grafana.helper
  (:require [clojure.data.json :as json]))

(set! *warn-on-reflection* true)

;; POD vs PROGRAM behaviour

(defn error-logic
  []
  (if (System/getenv "BABASHKA_POD")
    (throw (ex-info "grafana-active: pod equivalent to System exit -1" {}))
    (System/exit -1)))

;; LOG/DEBUG

(defn log
  [log-str]
  (println log-str))

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
