(ns active-grafana.main-adjust
  (:require [active-grafana.core     :as core]
            [active-grafana.helper   :as helper]
            [active-grafana.settings :as settings]
            [clojure.tools.cli       :refer [parse-opts]])
  (:gen-class))

(set! *warn-on-reflection* true)

(def opts
  [["-h" "--help" "Print this help message."]
   ["-s" "--show" "Show information on the first 100 library panels of the grafana-instance (GRAFANA_URL and GRAFANA_TOKEN)."]
   ["-a" "--adjust" "Adjust the library panel (PANEL_UID) using the DATASOURCE_UIDS as targets of a grafana-instance (GRAFANA_URL and GRAFANA_TOKEN)."]
   [nil "--url GRAFANA_URL" "The grafana-url."]
   [nil "--token GRAFANA_TOKEN" "The grafana-token."]
   [nil "--panel-uid PANEL_UID" "The panel uid."]
   [nil "--datasource-uids DATASOURCE_UIDS" "Datasource uids used within the target-template as comma separated string."]])

(defn print-usage [opts-map]
  (println "\nOptions:")
  (println (:summary opts-map))

  (println "\nExamples:")
  (println "adjust --help")
  (println "adjust --show   --url=<grafana-url> --token=<grafana-token>")
  (println "adjust --adjust --url=<grafana-url> --token=<grafana-token> --panel-uid=<panel-uid> --datasource-uids=\"<datasource-uid-1>,<datasource-uid-2>,...,<datasource-uid-n>\""))

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

      (:show (:options opts-map))
      (core/adjust-show (settings/create-adjust-arguments! (:options opts-map)))

      (:adjust (:options opts-map))
      (core/adjust (settings/create-adjust-arguments! (:options opts-map)))

      :else
      (print-usage opts-map))))
