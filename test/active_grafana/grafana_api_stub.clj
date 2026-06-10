(ns active-grafana.grafana-api-stub
  (:require [active-grafana.examples :as examples]))

(def get-dashboards (constantly examples/get-dashboards-response))

(defn find-dashboards-by-query [case-accessor]
  (constantly (get examples/find-dashboards-by-query-responses case-accessor)))
