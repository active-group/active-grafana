(ns active-grafana.main
  (:require [clojure.tools.cli :refer [parse-opts]]
            [active-grafana.core :as core]
            [active-grafana.settings :as settings]))

(def opts
  [["-h" "--help" "Print this help message and exit."]
   [nil  "--show-dashboards" "Show the first 1000 dashboards of 'from'-grafana-instance and exit."]
   [nil  "--show-folders" "Show the first 1000 folders of 'from'-grafana-instance and exit."]
   ["-c" nil "Copy a dashboard from one instance to another." :id :board]
   ["-r" nil "Copy alert-rules associated to a dashboard."    :id :rules]
   [nil "--board-uid BOARD_UID" "Either the uid of the dashboard to copy
                                           or the uid of the dashboard from which the
                                           alert-rules should be copied."]
   [nil "--from-url FROM_URL" "The grafana-url to copy from."]
   [nil "--from-token FROM_TOKEN" "The grafana-token of the grafana-instance to copy from."]
   [nil "--to-url TO_URL" "The grafana-url to copy to."]
   [nil "--to-token TO_TOKEN" "The grafana-token of the grafana-instance to copy to."]
   [nil "--message MESSAGE" "Optional: The dashboard change-message."]
   [nil "--board-folder-uid BOARD_FOLDER_UID" "The folder-uid to copy the dashboard to."]
   [nil "--rules-folder-uid RULES_FOLDER_UID" "The folder-uid to copy the rules to."]])

(defn print-usage [opts-map]
  (do (println "Usage:")
      (println "active-grafana
               [-c]
               [-r]
               [--board-uid=<dashboard-uid>]
               [--from-url=<from-grafana-url]
               [--from-token=<from-grafana-token>]
               [--to-url<to-grafana-url>]
               [--to-token=<to-grafana-token>]
               [--message=<message>]
               [--board-folder-uid=<board-folder-uid>]
               [--rules-folder-uid=<rules-folder-uid>]")
      (println "Options:")
      (println (:summary opts-map))))

(defn -main [& args]
  (let [opts-map (parse-opts args opts)]
    (cond
      (:errors opts-map)
      (do (doall (map println (:errors opts-map)))
          (print-usage opts-map)
          (System/exit -1))

      (:help (:options opts-map))
      (print-usage opts-map)

      (or (:show-dashboards (:options opts-map)) (:show-folders (:options opts-map)))
      (core/show (settings/create-arguments! (:options opts-map)))

      (or (:board (:options opts-map)) (:rules (:options opts-map)))
      (core/copy (settings/create-arguments! (:options opts-map)))

      :else
      (do
        (println "I don't know what to do.")
        (print-usage opts-map)))))
