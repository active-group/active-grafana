(ns active-grafana.settings)

(set! *warn-on-reflection* true)

(defrecord Grafana-Instance [url token])

;; >>> COPY

(defrecord Copy-Arguments [show-boards show-folders show-panels from to
                           board rules panels board-uid
                           from-instance to-instance message
                           board-folder-uid rules-folder-uid])

;; >>> Env variables or command line

(defn board-uid-arg
  [opts-map-options]
  (or (:board-uid opts-map-options) (System/getenv "BOARD_UID")))

(defn from-instance-arg
  [opts-map-options]
  (->Grafana-Instance (or (:from-url   opts-map-options) (System/getenv "FROM_URL"))
                      (or (:from-token opts-map-options) (System/getenv "FROM_TOKEN"))))

(defn to-instance-arg
  [opts-map-options]
  (->Grafana-Instance (or (:to-url   opts-map-options) (System/getenv "TO_URL"))
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

;; <<< Env variables or command line

(defn create-copy-arguments!
  [opts-map-options]
  (->Copy-Arguments (:show-dashboards     opts-map-options)
                    (:show-folders        opts-map-options)
                    (:show-panels         opts-map-options)
                    (:from                opts-map-options)
                    (:to                  opts-map-options)
                    (:board               opts-map-options)
                    (:rules               opts-map-options)
                    (:panels              opts-map-options)
                    (board-uid-arg        opts-map-options)
                    (from-instance-arg    opts-map-options)
                    (to-instance-arg      opts-map-options)
                    (message-arg          opts-map-options)
                    (board-folder-uid-arg opts-map-options)
                    (rules-folder-uid-arg opts-map-options)))

;; <<< COPY

;; >>> ADJUST PANEL

(defrecord Adjust-Arguments [grafana-instance
                            panel-uid
                            datasource-uids])

;; >>> Env variables or command line

(defn grafana-instance-arg
  [opts-map-options]
  (->Grafana-Instance (or (:url   opts-map-options) (System/getenv "GRAFANA_URL"))
                      (or (:token opts-map-options) (System/getenv "GRAFANA_TOKEN"))))

(defn panel-uid-arg
  [opts-map-options]
  (or (:panel-uid opts-map-options) (System/getenv "PANEL_UID")))

(defn datasource-uids-arg
  [opts-map-options]
  (or (:datasource-uids opts-map-options) (System/getenv "DATASOURCE_UIDS")))

;; <<< Env variables or command line

(defn create-adjust-arguments!
  [opts-map-options]
  (->Adjust-Arguments (grafana-instance-arg opts-map-options)
                     (panel-uid-arg        opts-map-options)
                     (datasource-uids-arg  opts-map-options)))

;; <<< ADJUST PANEL
