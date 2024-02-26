(ns active-grafana.main-copy
  (:require [active-grafana.core     :as core]
            [active-grafana.helper   :as helper]
            [active-grafana.settings :as settings]
            [clojure.tools.cli       :refer [parse-opts]]
            )
  (:gen-class))

(set! *warn-on-reflection* true)

(def opts
  [["-h" "--help" "Print this help message."]
   [nil  "--show-dashboards" "Show the first 1000 dashboards of a grafana-instance (*_URL, *_TOKEN). Use `--from` and/or `--to` to choose instance to show from (default: --from and --to)."]
   [nil  "--show-folders" "Show the first 1000 folders of a grafana-instance (*_URL, *_TOKEN). Use `--from` and/or `--to` to choose instance to show from (default: --from and --to)."]
   [nil  "--show-panels" "Show information on the first 100 library panels of the grafana-instance (*_URL, *_TOKEN). Use `--from` and/or `--to` to choose instance to show from (default: --from and --to)."]

   [nil  "--show-dashboard-alerts" "Show alert-rules related to a dashboard (BOARD_UID) within a grafana-instance (*_URL, *_TOKEN). Use `--from` and/or `--to` to choose instance to show from (default: --from and --to)."]
   [nil  "--show-dashboard-panels" "Show library-panels related to a dashboard (BOARD_UID) within a grafana-instance (*_URL, *_TOKEN). Use `--from` and/or `--to` to choose instance to show from (default: --from and --to)."]

   ["-b" nil "Copy a dashboard (BOARD_UID) from one instance (FROM_URL, FROM_TOKEN) to another (TO_URL, TO_TOKEN). Optional provide a MESSAGE and BOARD_FOLDER_UID." :id :board]
   ["-r" nil "Copy alert-rules associated to a dashboard (BOARD_UID) from one instance (FROM_URL, FROM_TOKEN) to the folder (RULES_FOLDER_UID) on another (TO_URL, TO_TOKEN)." :id :rules]
   ["-p" nil "Copy library-panels associated to a dashboard (BOARD_UID) from one instance (FROM_URL, FROM_TOKEN) to the folder (PANELS_FOLDER_UID) on another (TO_URL, TO_TOKEN)." :id :panels]
   [nil "--from" "Use the from-grafana-instance for show commands."]
   [nil "--to" "Use the to-grafana-instance for show commands."]
   [nil "--board-uid BOARD_UID" "Uid of the dashboard that is either be copied or from which the alert-rules are copied."]
   [nil "--from-url FROM_URL" "The grafana-url to copy from."]
   [nil "--from-token FROM_TOKEN" "The grafana-token of the grafana-instance to copy from."]
   [nil "--to-url TO_URL" "The grafana-url to copy to."]
   [nil "--to-token TO_TOKEN" "The grafana-token of the grafana-instance to copy to."]
   [nil "--message MESSAGE" "Optional: The dashboard change-message when copying a dashboard."]
   [nil "--board-folder-uid BOARD_FOLDER_UID" "The folder-uid to copy the dashboard to. If not provided the General-folder is used."]
   [nil "--rules-folder-uid RULES_FOLDER_UID" "The folder-uid to copy the rules to."]
   [nil "--panels-folder-uid PANELS_FOLDER_UID" "The folder-uid to copy the panels to."]])

(defn print-usage [opts-map]
  (println "\nOptions:")
  (println (:summary opts-map))

  (println "\nExamples:")
  (println "copy --help")
  (println "copy --show-dashboards --from --from-url=<from-grafana-url --from-token=<from-grafana-token>")
  (println "copy --show-folders --to --to-url=<to-grafana-url --to-token=<to-grafana-token>")
  (println "copy -b --board-uid=<dashboard-uid> --from-url=<from-grafana-url --from-token=<from-grafana-token> --to-url=<to-grafana-url --to-token=<to-grafana-token> [--board-folder-uid=<board-folder-uid>] [--message=<message>]")
  (println "copy -r --board-uid=<dashboard-uid> --from-url=<from-grafana-url --from-token=<from-grafana-token> --to-url=<to-grafana-url --to-token=<to-grafana-token> --rules-folder-uid=<rules-folder-uid>"))

(defn -main [& args]
  (let [opts-map (parse-opts args opts)]
    (cond
      (:errors opts-map)
      (do (doall (map println (:errors opts-map)))
          (print-usage opts-map)
          (helper/error-logic))

      (not (empty? (:arguments opts-map)))
      (do (println "Unknown arguments: " (:arguments opts-map))
          (print-usage opts-map)
          (helper/error-logic))

      (:help (:options opts-map))
      (print-usage opts-map)

      (or (:show-dashboards (:options opts-map)) (:show-folders (:options opts-map)) (:show-panels (:options opts-map))
          (:show-dashboard-alerts (:options opts-map)) (:show-dashboard-panels (:options opts-map)))
      (core/copy-show (settings/create-copy-arguments! (:options opts-map)))

      (or (:board (:options opts-map)) (:rules (:options opts-map)) (:panels (:options opts-map)))
      (core/copy (settings/create-copy-arguments! (:options opts-map)))

      :else
      (print-usage opts-map))))
