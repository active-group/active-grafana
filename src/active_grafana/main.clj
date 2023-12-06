(ns active-grafana.main
  (:require [clojure.tools.cli :refer [parse-opts]]
            [active-grafana.core :as core]
            [active-grafana.settings :as settings]))

(def opts
  [["-h" "--help" "Print this help message and exit."]
   ["-c" "--copy-dashboard" "Copy a dashboard from one instance to another."]
   ["-r" nil "Also copy the related alert-rules." :id :rules]
   [nil "--board-uid BOARD_UID" "The uid of the dashboard to copy."]
   [nil "--from-url FROM_URL" "The grafana-url to copy from."]
   [nil "--from-token FROM_TOKEN" "The grafana-token of the grafana-instance to copy from."]
   [nil "--to-url TO_URL" "The grafana-url to copy to."]
   [nil "--to-token TO_TOKEN" "The grafana-token of the grafana-instance to copy to."]
   [nil "--message MESSAGE" "Optional: The change-message."]
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

      (or (:rules (:options opts-map)) (:copy-dashboard (:options opts-map)))
      (core/copy (settings/create-arguments (:options opts-map)))

      :else
      (do
        (println "I don't know what to do.")
        (print-usage opts-map)))))
