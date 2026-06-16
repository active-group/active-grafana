(ns active-grafana.grafana-api-stub
  (:require [active-grafana.examples :as examples]))

(def get-dashboards (constantly examples/get-dashboards-response))

(defn find-dashboards-by-query [case-accessor]
  (constantly (get examples/find-dashboards-by-query-responses case-accessor)))

(def get-dashboard-by-uid (constantly examples/get-dashboard-by-uid-response))

(defn find-folders-by-query [case-accessor]
  (constantly (get examples/find-folders-by-query-responses case-accessor)))

(def create-folder (constantly examples/create-folder-response))

(def get-library-element-by-uid (constantly examples/get-library-panel-by-uid-response))

(def get-all-alert-rules (constantly examples/get-all-alert-rules-response))

(def get-folder-by-folder-uid (constantly examples/get-folder-by-folder-uid-response))

(def get-library-panels (constantly examples/get-library-panels-response))

(def create-alert-rule (constantly examples/create-alert-rule-response))

(def update-alert-rule (constantly examples/update-alert-rule-response))

(def create-update-dashboard (constantly examples/create-update-dashboard-response))
