(ns active-grafana.settings)

(set! *warn-on-reflection* true)

(defrecord Grafana-Instance [url token])

;; >>> COPY

(defrecord Copy-Arguments [show-boards show-folders show-panels
                           show-board-alerts show-board-panels
                           from to
                           board alerts panels board-uid
                           from-instance to-instance to-message
                           to-board-folder-uid to-alerts-folder-uid to-panels-folder-uid])

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

(defn to-message-arg
  [opts-map-options]
  (or (:to-message opts-map-options) (System/getenv "TO_MESSAGE")))

(defn to-board-folder-uid-arg
  [opts-map-options]
  (or (:to-board-folder-uid opts-map-options) (System/getenv "TO_BOARD_FOLDER_UID")))

(defn to-alerts-folder-uid-arg
  [opts-map-options]
  (or (:to-alerts-folder-uid  opts-map-options) (System/getenv "TO_ALERTS_FOLDER_UID")))

(defn to-panels-folder-uid-arg
  [opts-map-options]
  (or (:to-panels-folder-uid  opts-map-options) (System/getenv "TO_PANELS_FOLDER_UID")))

;; <<< Env variables or command line

(defn create-copy-arguments!
  [opts-map-options]
  (->Copy-Arguments (:show-dashboards         opts-map-options)
                    (:show-folders            opts-map-options)
                    (:show-panels             opts-map-options)
                    (:show-dashboard-alerts   opts-map-options)
                    (:show-dashboard-panels   opts-map-options)
                    (:from                    opts-map-options)
                    (:to                      opts-map-options)
                    (:board                   opts-map-options)
                    (:alerts                  opts-map-options)
                    (:panels                  opts-map-options)
                    (board-uid-arg            opts-map-options)
                    (from-instance-arg        opts-map-options)
                    (to-instance-arg          opts-map-options)
                    (to-message-arg           opts-map-options)
                    (to-board-folder-uid-arg  opts-map-options)
                    (to-alerts-folder-uid-arg opts-map-options)
                    (to-panels-folder-uid-arg opts-map-options)))

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
