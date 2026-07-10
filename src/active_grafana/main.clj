(ns active-grafana.main
  (:require [active-grafana.core :as core]
            [active-grafana.settings :as settings]
            ;; NOTE: reload babashka.cli because of
            ;; the version built-in to babashka does not support --help
            ;; see https://github.com/babashka/babashka/issues/1984#issuecomment-4810338306
            [babashka.cli :as cli] :reload))

;; TODO: Read throug https://blog.michielborkent.nl/babashka-cli-help-and-completions.html
;; More resources:
;; https://github.com/babashka/cli#help
;; https://github.com/babashka/cli
;; https://www.braveclojure.com/quests/babooka/
;; https://book.babashka.org/
;; https://github.com/babashka/babashka/blob/master/doc/projects.md

(defn copy [{:keys [opts]}]
  (let [source-grafana-instance (settings/make-source-grafana-instance opts)
        target-grafana-instance (settings/make-target-grafana-instance opts)
        dashboard-title         (:title opts)]
    (core/convenient-copy source-grafana-instance
                          target-grafana-instance
                          dashboard-title)))

(def table
  [{:cmds ["copy"]
    :fn copy
    :doc "Copy a file\nMore details here"
    :spec {:convenient           {:coerce :boolean
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
           }}])

(defn -main [& args]
  (cli/dispatch table args {:prog "active-grafana" :help true}))

(apply -main *command-line-args*)
