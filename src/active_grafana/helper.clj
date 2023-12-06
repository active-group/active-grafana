(ns active-grafana.helper
  (:require [clojure.data.json :as json]))

(defn log
  [log-str]
  (println log-str))

(defn debug
  [deb]
  (prn deb)
  deb)

(defn json->clj
  [request]
  (json/read-str (:body request)))

;; json from clojure map
(defn clj->json
  [body]
  (json/write-str body))
