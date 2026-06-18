(ns active-grafana.grafana-api-stub
  (:require [active-grafana.examples :as examples]))

(def get-dashboards (constantly examples/get-dashboards-response))

(defn find-dashboards-by-query [case-accessor]
  (constantly (get examples/find-dashboards-by-query-responses case-accessor)))

(defn get-dashboard-by-uid [dashboard]
  (constantly (examples/get-dashboard-by-uid-response dashboard)))

(defn find-folders-by-query [case-accessor]
  (constantly (get examples/find-folders-by-query-responses case-accessor)))

(def create-folder (constantly examples/create-folder-response))

(defn get-library-element-by-uid [id panel]
  (constantly (examples/make-get-library-panel-by-uid-response id panel)))

(defn get-all-alert-rules [alerts]
  (constantly (examples/get-all-alert-rules-response alerts)))

(defn get-library-panels [panels]
  (constantly (examples/make-get-library-panels-response panels)))

(def create-alert-rule (constantly examples/create-alert-rule-response))

(def update-alert-rule (constantly examples/update-alert-rule-response))

(def create-library-element (constantly examples/create-library-element-response))

(def update-library-element (constantly examples/update-library-element-response))

(defn create-update-dashboard [dashboard]
  (constantly (examples/create-update-dashboard-response dashboard)))
