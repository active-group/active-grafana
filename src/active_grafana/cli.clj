(ns active-grafana.cli
  "Resources in how to use the babashka cli:
     - https://blog.michielborkent.nl/babashka-cli-help-and-completions.html
     - https://github.com/babashka/cli
     - https://github.com/babashka/cli#help"
  (:require [active-grafana.core :as core]
            [active-grafana.settings :as settings]
            ;; NOTE: reload babashka.cli because of
            ;; the version built-in to babashka does not support --help
            ;; see https://github.com/babashka/babashka/issues/1984#issuecomment-4810338306
            [babashka.cli :as cli] :reload))

;; TODO: look after completions...
;; https://blog.michielborkent.nl/babashka-cli-help-and-completions.html#:~:text=for%20more%20information.-,Shell%20completions,-In%20Babashka%20CLI
;; for completions in fish use:
;; ./src/active_grafana/main.clj org.babashka.cli/completions snippet --shell fish | source

(defn convenient-copy [{:keys [opts]}]
  (let [source-grafana-instance (settings/make-source-grafana-instance opts)
        target-grafana-instance (settings/make-target-grafana-instance opts)
        dashboard-title         (:title opts)]
    (core/convenient-copy source-grafana-instance
                          target-grafana-instance
                          dashboard-title
                          opts)))

(defn legacy-copy [{:keys [opts]}]
  (let [copy-arguments (settings/create-copy-arguments! opts)]
    (println (pr-str copy-arguments))
    (core/copy copy-arguments)))

(defn legacy-show [{:keys [opts]}]
  (let [show-arguments (settings/create-copy-arguments! opts)]
    (println (pr-str show-arguments))
    (core/copy-show show-arguments)))

(defn legacy-adjust [{:keys [opts]}]
  (let [adjust-arguments (settings/create-adjust-arguments! opts)]
    (core/adjust adjust-arguments)))

(def tree
  {:spec      {:verbose {:coerce :boolean :desc "Be verbose" :alias :v}}
   :cmd-order ["copy" #_"adjust" "legacy"]
   :cmd
   {"copy"
    {:doc       "Copy a grafana thing from a source instance to a target instance."
     :cmd-order ["dashboard" #_#_"panel" "alert"]
     :cmd
     {"dashboard"
      {:fn         convenient-copy
       :doc        "Copy a dashboard matched by title from a :source to a :target grafana instance. Throws Exception if not possible."
       :spec       {:title                {:desc    "The title of the dashboard on the source grafana instance."
                                           :require true}
                    :message              {:desc "An optional message to use as commit message for the updated/created dashboard on the target grafana instance."}
                    :target-folder-uid    {:desc "An optional target folder uid used to copy the dashboard to on the target grafana instance."}
                    :source-dashboard-uid {:desc "An optional dashboard uid to use instead of using "}
                    :source-url           {:desc    "URL of the source grafana instance to copy the dashboard from."
                                           :require true}
                    :source-token         {:desc    "Token to authenticate against the source grafana instance."
                                           :require true}
                    :target-url           {:desc    "URL of the target grafana instance to copy the dashboard to."
                                           :require true}
                    :target-token         {:desc    "Token to authenticate against the target grafana instance."
                                           :require true}}
       :args->opts [:title]}
      ;; "panel"
      ;; {:doc "TODO: Implement a copy panel command."}
      ;; "alert"
      ;; {:doc "TODO: Implement a copy alert command."}
      }}
    ;; "adjust"
    ;; {:doc "TODO: Implement adjust by implementing a convenient version of the legacy adjust command."}
    "legacy"
    {:doc       "The legacy active-grafana commands."
     :cmd-order ["show" "copy" "adjust"]
     :cmd
     {"show"
      {:fn   legacy-show
       :doc  "Show things like dashboards or library-panels or alerts (related to a dashboard)."
       :spec {:show-dashboards       {:desc   "Show the first 1000 dashboards of a grafana-instance (*_URL, *_TOKEN). Use `--from` and/or `--to` to choose instance to show from (default: --from and --to)."
                                      :coerce :boolean}
              :show-folders          {:desc   "Show the first 1000 folders of a grafana-instance (*_URL, *_TOKEN). Use `--from` and/or `--to` to choose instance to show from (default: --from and --to)."
                                      :coerce :boolean}
              :show-panels           {:desc   "Show information on the first 100 library panels of the grafana-instance (*_URL, *_TOKEN). Use `--from` and/or `--to` to choose instance to show from (default: --from and --to)."
                                      :coerce :boolean}
              :show-dashboard-alerts {:desc   "Show alert-rules related to a dashboard (BOARD_UID) within a grafana-instance (*_URL, *_TOKEN). Use `--from` and/or `--to` to choose instance to show from (default: --from and --to)."
                                      :coerce :boolean}
              :show-dashboard-panels {:desc   "Show library-panels related to a dashboard (BOARD_UID) within a grafana-instance (*_URL, *_TOKEN). Use `--from` and/or `--to` to choose instance to show from (default: --from and --to)."
                                      :coerce :boolean}
              :from                  {:desc   "Use the from-grafana-instance for show commands."
                                      :coerce :boolean}
              :to                    {:desc   "Use the to-grafana-instance for show commands."
                                      :coerce :boolean}
              :from-url              {:desc     "The grafana-url to copy from."
                                      :required true}
              :from-token            {:desc     "The grafana-token of the grafana-instance to copy from."
                                      :required true}
              :to-url                {:desc     "The grafana-url to copy to."
                                      :required true}
              :to-token              {:desc     "The grafana-token of the grafana-instance to copy to."
                                      :required true}}}
      "copy"
      {:fn   legacy-copy
       :doc  "Copy grafana dashboards and its related library-panels and alerts."
       :spec {:board                {:desc   "Copy a dashboard (BOARD_UID) from one instance (FROM_URL, FROM_TOKEN) to another (TO_URL, TO_TOKEN). Optional provide a TO_MESSAGE and TO_BOARD_FOLDER_UID."
                                     :coerce :boolean
                                     :alias  :b}
              :alerts               {:desc   "Copy alert-rules associated to a dashboard (BOARD_UID) from one instance (FROM_URL, FROM_TOKEN) to the folder (TO_ALERTS_FOLDER_UID) on another (TO_URL, TO_TOKEN)."
                                     :coerce :boolean
                                     :alias  :a}
              :panels               {:desc   "Copy library-panels associated to a dashboard (BOARD_UID) from one instance (FROM_URL, FROM_TOKEN) to the folder (TO_PANELS_FOLDER_UID) on another (TO_URL, TO_TOKEN)."
                                     :coerce :boolean
                                     :alias  :p}
              :board-uid            {:desc     "Uid of the dashboard that is either be copied or from which the alert-rules or library-panels are copied."
                                     :required true}
              :from-url             {:desc     "The grafana-url to copy from."
                                     :required true}
              :from-token           {:desc     "The grafana-token of the grafana-instance to copy from."
                                     :required true}
              :to-url               {:desc     "The grafana-url to copy to."
                                     :required true}
              :to-token             {:desc     "The grafana-token of the grafana-instance to copy to."
                                     :required true}
              :to-message           {:desc     "Optional: The dashboard change-message when copying a dashboard."}
              :to-board-folder-uid  {:desc     "The folder-uid to copy the dashboard to. If not provided the General-folder is used."}
              :to-alerts-folder-uid {:desc     "The folder-uid to copy the alert-rules to."}
              :to-panels-folder-uid {:desc     "The folder-uid to copy the panels to."}}}
      "adjust"
      {:fn   legacy-adjust
       :doc  " grafana things like dashboards, library-panels or alerts."
       :spec {:panel-uid       {:desc     "The panel uid. (env var: PANEL_UID)"
                                :required true}
              :url             {:desc     "The grafana-url. (env var: GRAFANA_URL)"
                                :required true}
              :token           {:desc     "The grafana-token. (env var: GRAFANA_TOKEN)"
                                :required true}
              :datasource-uids {:desc     "Datasource uids used within the target-template as comma separated string. (env var: DATASOURCE_UIDS)"
                                :required true}
              :i-am-an-expert  {:desc     "{:path <path-to-f-target->target> :data <data>}, see Readme for details. (env var: EXPERT_DATA)"
                                :required true}}}}}}
   :epilog    "Docs: https://github.com/active-group/active-grafana/blob/main/README.md"})

(defn -main [& args]
  (println "args: " (pr-str args))
  (cli/dispatch tree args {:prog "./grafana.clj" :help true}))
