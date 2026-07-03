(ns active-grafana.grafana-api-stub
  (:require [active-grafana.examples :as examples]))

(defn get-dashboards [dashboards]
  (constantly (examples/get-dashboards-response dashboards)))

(defn find-dashboards-by-query [dashboards]
  (constantly (examples/find-dashboards-by-query-response dashboards)))

(defn get-dashboard-by-uid [dashboard]
  (constantly (examples/get-dashboard-by-uid-response dashboard)))

(defn find-folders-by-query [folders]
  (constantly (examples/find-folders-by-query-response folders)))

(defn create-folder [folder]
  (constantly (examples/create-folder-response folder)))

(defn get-library-element-by-uid [id panel]
  (constantly (examples/get-library-panel-by-uid-response id panel)))

(defn get-all-alert-rules [alerts]
  (constantly (examples/get-all-alert-rules-response alerts)))

(defn get-library-panels [panels]
  (constantly (examples/get-library-panels-response panels)))

(def create-alert-rule (constantly examples/create-alert-rule-response))

(def update-alert-rule (constantly examples/update-alert-rule-response))

(def create-library-element (constantly examples/create-library-element-response))

(def update-library-element (constantly examples/update-library-element-response))

(defn create-update-dashboard [dashboard]
  (constantly (examples/create-update-dashboard-response dashboard)))
