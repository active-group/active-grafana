(ns active-grafana.grafana-api-stub
  (:require [active-grafana.grafana-api-responses :as api-responses]))

(defn get-dashboards [dashboards]
  (constantly (api-responses/get-dashboards-response dashboards)))

(defn find-dashboards-by-query [dashboards]
  (constantly (api-responses/find-dashboards-by-query-response dashboards)))

(defn get-dashboard-by-uid [dashboard]
  (constantly (api-responses/get-dashboard-by-uid-response dashboard)))

(defn find-folders-by-query [folders]
  (constantly (api-responses/find-folders-by-query-response folders)))

(defn create-folder [folder]
  (constantly (api-responses/create-folder-response folder)))

(defn get-library-element-by-uid [id panel]
  (constantly (api-responses/get-library-panel-by-uid-response id panel)))

(defn get-all-alert-rules [alerts]
  (constantly (api-responses/get-all-alert-rules-response alerts)))

(defn get-library-panels [panels]
  (constantly (api-responses/get-library-panels-response panels)))

(defn create-alert-rule [alert-rule]
  (constantly (api-responses/create-alert-rule-response alert-rule)))

(defn update-alert-rule [alert-rule]
  (constantly (api-responses/update-alert-rule-response alert-rule)))

(defn create-library-element [library-element]
  (constantly (api-responses/create-library-element-response library-element)))

(defn update-library-element [library-element]
  (constantly (api-responses/update-library-element-response library-element)))

(defn create-update-dashboard [dashboard]
  (constantly (api-responses/create-update-dashboard-response dashboard)))
