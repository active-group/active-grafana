(ns active-grafana.settings
  (:require [active.clojure.record :refer [define-record-type]]))

(define-record-type ^{:doc "Grafana instance: URL and Token"}
  GrafanaInstance
  make-grafana-instance
  grafana-instance?
  [url   grafana-instance-url
   token grafana-instance-token])

(define-record-type ^{:doc "Provided arguments."}
  Arguments
  make-arguments
  arguments?
  [rules            arguments-rules
   board-uid        arguments-board-uid
   from-instance    arguments-from-instance
   to-instance      arguments-to-instance
   message          arguments-message
   board-folder-uid arguments-board-folder-uid
   rules-folder-uid arguments-rules-folder-uid])

;; FIXME: read values from env
(defn create-arguments
  [opts-map-options]
  (make-arguments (:rules     opts-map-options)
                  (:board-uid opts-map-options)
                  (make-grafana-instance (:from-url opts-map-options) (:from-token opts-map-options))
                  (make-grafana-instance (:to-url   opts-map-options) (:to-token   opts-map-options))
                  (or (:message opts-map-options) "Changes made by the active-grafana tool.")
                  (:board-folder-uid  opts-map-options)
                  (:rules-folder-uid  opts-map-options)))
