(ns active-grafana.main
  (:require [babashka.cli :as cli]
            [active-grafana.core :as core]
            [active-grafana.settings :as settings]))

;; TODO: Read throug https://blog.michielborkent.nl/babashka-cli-help-and-completions.html
;; More resources:
;; https://github.com/babashka/cli#help
;; https://github.com/babashka/cli
;; https://www.braveclojure.com/quests/babooka/
;; https://book.babashka.org/

(defn copy [{:keys [opts]}]
  (let [source-grafana-instance (settings/make-source-grafana-instance opts)
        target-grafana-instance (settings/make-target-grafana-instance opts)
        dashboard-title         (:title opts)]
    (println (pr-str source-grafana-instance))
    (println (pr-str target-grafana-instance))
    (println (pr-str dashboard-title))
    #_(core/convenient-copy source-grafana-instance
                            target-grafana-instance
                            dashboard-title)))

(def cli-opts
  {:convenient           {:coerce :boolean
                          :desc "A"}
   :source-url           {:desc    "URL of the source grafana instance to copy the dashboard from."
                          :require true}
   :source-token         {:desc    "Token to authenticate against the source grafana instance."
                          :require true}
   :target-url           {:desc    "URL of the target grafana instance to copy the dashboard to."
                          :require true}
   :target-token         {:desc    "Token to authenticate against the target grafana instance."
                          :require true}
   :title                {:desc    "The title of the dashboard to copy on the source grafana instance."
                          :require true}

   ;; :message              {:desc "An optional message to use as commit message for the updated/created dashboard on the target grafana instance."}
   ;; :target-folder-uid    {:desc "An optional target folder uid used to copy the dashboard to on the target grafana instance."}
   ;; :source-dashboard-uid {:desc "An optional dashboard uid to use instead of using "}
   })

(defn help [_]
  (println "TODO: read the babashka-cli-help-and-completions blog post. See the top of src/active_grafana/main.clj"))

(def table
  [{:cmds ["copy"] :fn copy :spec cli-opts}
   {:cmds [] :fn help}])

(cli/dispatch table *command-line-args*)
