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
  [show-boards      arguments-show-boards
   show-folders     arguments-show-folders
   board            arguments-board
   rules            arguments-rules
   board-uid        arguments-board-uid
   from-instance    arguments-from-instance
   to-instance      arguments-to-instance
   message          arguments-message
   board-folder-uid arguments-board-folder-uid
   rules-folder-uid arguments-rules-folder-uid])

;; >>> Env variables or command line

(defn board-uid-arg
  [opts-map-options]
  (or (:board-uid opts-map-options) (System/getenv "BOARD_UID")))

(defn from-instance-arg
  [opts-map-options]
  (make-grafana-instance (or (:from-url   opts-map-options) (System/getenv "FROM_URL"))
                         (or (:from-token opts-map-options) (System/getenv "FROM_TOKEN"))))

(defn to-instance-arg
  [opts-map-options]
  (make-grafana-instance (or (:to-url   opts-map-options) (System/getenv "TO_URL"))
                         (or (:to-token opts-map-options) (System/getenv "TO_TOKEN"))))

(defn message-arg
  [opts-map-options]
  (or (:message opts-map-options) (System/getenv "MESSAGE")))

(defn board-folder-uid-arg
  [opts-map-options]
  (or (:board-folder-uid opts-map-options) (System/getenv "BOARD_FOLDER_UID")))

(defn rules-folder-uid-arg
  [opts-map-options]
  (or (:rules-folder-uid  opts-map-options) (System/getenv "RULES_FOLDER_UID")))

;; <<<

(defn create-arguments!
  [opts-map-options]
  (make-arguments (:show-dashboards     opts-map-options)
                  (:show-folders        opts-map-options)
                  (:board               opts-map-options)
                  (:rules               opts-map-options)
                  (board-uid-arg        opts-map-options)
                  (from-instance-arg    opts-map-options)
                  (to-instance-arg      opts-map-options)
                  (message-arg          opts-map-options)
                  (board-folder-uid-arg opts-map-options)
                  (rules-folder-uid-arg opts-map-options)))
