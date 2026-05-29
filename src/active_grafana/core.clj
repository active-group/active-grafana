(ns active-grafana.core
  (:require [active-grafana.grafana-api :as api]
            [active-grafana.helper      :as helper]
            [clojure.edn                :as edn]
            [clojure.pprint             :as pprint]
            [clojure.string             :as str]))

(set! *warn-on-reflection* true)

;; >>> SHOW

(defn show-dashboards
  ^{:doc "Print the title, uid and url of the first 1000 dashboards of the
          grafana-instance.

          grafana-instance: url and token as GrafanaInstance record."}
  [grafana-instance]
  (let [boards (helper/json->clj
                (api/get-dashboards
                 (-> grafana-instance :url)
                 (-> grafana-instance :token)))]
    (println "First 1000 dashboards:")
    (pprint/print-table ["title" "uid" "url"] boards)))

(defn show-folders
  ^{:doc "Print the title and uid of the first 1000 folders of the grafana
          instance.

          grafana-instance: url and token as GrafanaInstance record."}
  [grafana-instance]
  (let [folders (helper/json->clj
                 (api/get-folders
                  (-> grafana-instance :url)
                  (-> grafana-instance :token)))]
    (println "First 1000 folders:")
    (pprint/print-table ["title" "uid"] folders)))

(defn show-library-panels
  ^{:doc "Show for a given grafana-instance the name, uid and folder-uid of the
          first 100 library panels"}
  [grafana-instance]
  (let [library-panels (helper/json->clj
                        (api/get-library-panels
                         (-> grafana-instance :url)
                         (-> grafana-instance :token)))
        panels (map (fn [panel]
                      {"name"      (get panel "name")
                       "uid"       (get panel "uid")
                       "folderUid" (get panel "folderUid")})
                    (get-in library-panels ["result" "elements"]))]
    (println "First 100 library panels:")
    (println (str "totalCount: " (get-in library-panels ["result" "totalCount"])))
    (pprint/print-table panels)))

(defn find-dashboard-related-alert-rules
  ^{:doc "Find alert-rules that are related to a specific dashboard.
          Note: searches for the uid within `annotations` > `__dashboardUid__`.

          grafana-instance: url and token as GrafanaInstance record.
          dashboard-uid:    to search for in the alert-rules."}
  [grafana-instance dashboard-uid]
  (let [alert-rules (helper/json->clj
                     (api/get-all-alert-rules
                      (-> grafana-instance :url)
                      (-> grafana-instance :token)))]
    ;; rule-structure:
    ;; { ..., "uid" "my-uid", "annotations" { ..., "__dashboardUid__" "dash-uid", ...}, ...}
    ;; note: there are several other fields within "annotations" where you could add a dashboard-uid,
    ;; but this is the expected one
    ;; alternative: search within all values in the annotations map for the dashboard-uid
    (filter (fn [rule] (= dashboard-uid
                          (-> rule
                              (get "annotations")
                              (get "__dashboardUid__"))))
            alert-rules)))

(defn show-dashboard-alerts
  ^{:doc "Show alert-rules related to a specific dashboard.

          grafana-instance: url and token as GrafanaInstance record.
          dashboard-uid:    to search for in the alert-rules."}
  [grafana-instance board-uid]
  (let [alert-rules (find-dashboard-related-alert-rules grafana-instance board-uid)]
    (println (str "Alert alert-uids related to dashboard: " board-uid))
    (pprint/print-table ["uid" "title" "folderUID"] alert-rules)))

;; FIXME: Is there any better way to find dashboard related library panels

;; alternative: get all library-panel - for each library-panel, search all connections, check whether connection is dashboard-uid
;; note: you only get the first 100 library-panels
(defn find-dashboard-related-panels
  ^{:doc "Find library panels that are related to a specific dashboard.

          grafana-instance: url and token as GrafanaInstance record.
          dashboard-uid:    to search for in the panels."}
  [grafana-instance dashboard-uid]
  (let [dashboard (helper/json->clj
                   (api/get-dashboard-by-uid (-> grafana-instance :url)
                                             (-> grafana-instance :token)
                                             dashboard-uid))

        ;; go through all panels and search for "libraryPanel" entries
        ;; (contains nil for every non-library-panel)
        panel-uids (->> (get-in dashboard ["dashboard" "panels"])
                        (map (fn [panel] (get-in panel ["libraryPanel" "uid"])))
                        (remove nil?))]
    ;; get cannot be nil - since the dashboard points to this library-panel
    (map (fn [panel-uid] (get (helper/json->clj
                               (api/get-library-element-by-uid (-> grafana-instance :url)
                                                               (-> grafana-instance :token)
                                                               panel-uid))
                              "result"))
         panel-uids)))

(defn show-dashboard-panels
  ^{:doc "Show panels related to a specific dashboard.

          grafana-instance: url and token as GrafanaInstance record.
          dashboard-uid:    to search for in the panels."}
  [grafana-instance board-uid]
  (let [panels (find-dashboard-related-panels grafana-instance board-uid)]
    (println (str "Panels related to dashboard: " board-uid))
    (pprint/print-table ["uid" "name"] panels)))

(defn copy-show
  ^{:doc "Based on the given arguments, print information about the first 1000
          dashboards and/or the first 1000 folders and/or the first 100 library panels.

          args: Provided arguments, as Copy-Arguments record.
                If neither `from`, nor `to` is set, default to show both."}
  [args]
  ;; if neither from nor to is set, show both
  (let [from-to-not-set (and (nil? (-> args :from)) (nil? (-> args :to)))]

    (when (and (-> args :show-boards) (or (-> args :from) from-to-not-set))
      (helper/log "show from-dashboards")
      (show-dashboards (-> args :from-instance)))
    (when (and (-> args :show-boards) (or (-> args :to) from-to-not-set))
      (helper/log "show to-dashboards")
      (show-dashboards (-> args :to-instance)))

    (when (and (-> args :show-folders) (or (-> args :from) from-to-not-set))
      (helper/log "show from-folders")
      (show-folders (-> args :from-instance)))
    (when (and (-> args :show-folders) (or (-> args :to) from-to-not-set))
      (helper/log "show to-folders")
      (show-folders (-> args :to-instance)))

    (when (and (-> args :show-panels) (or (-> args :from) from-to-not-set))
      (helper/log "show from-panels")
      (show-library-panels (-> args :from-instance)))
    (when (and (-> args :show-panels) (or (-> args :to) from-to-not-set))
      (helper/log "show to-panels")
      (show-library-panels (-> args :to-instance)))

    (when (and (-> args :show-board-alerts) (or (-> args :from) from-to-not-set))
      (helper/log "show from-dashboard related alerts")
      (show-dashboard-alerts (-> args :from-instance)
                             (-> args :board-uid)))
    (when (and (-> args :show-board-alerts) (or (-> args :to) from-to-not-set))
      (helper/log "show to-dashboard related alerts")
      (show-dashboard-alerts (-> args :to-instance)
                             (-> args :board-uid)))

    (when (and (-> args :show-board-panels) (or (-> args :from) from-to-not-set))
      (helper/log "show from-dashboard related library panels")
      (show-dashboard-panels (-> args :from-instance)
                             (-> args :board-uid)))
    (when (and (-> args :show-board-panels) (or (-> args :to) from-to-not-set))
      (helper/log "show to-dashboard related library panels")
      (show-dashboard-panels (-> args :to-instance)
                             (-> args :board-uid)))))

(defn adjust-show
  ^{:doc "Show for a given grafana-instance the name, uid and folder-uid of the
          first 100 library panels

          args: Provided arguments, as Adjust-Arguments record. "}
  [args]
  (show-library-panels (-> args :grafana-instance)))

;; <<< SHOW

;; >>> COPY

(defn copy-dashboard
  ^{:doc "Copy a dashboard from a grafana instance to another instance.
          Note: an existing dashboard in the 'to'-instance will be overwritten.

          from-grafana:  url and token as GrafanaInstance record.
          to-grafana:    url and token as GrafanaInstance record.
          dashboard-uid: uid of a dashboard in the 'from'-instance,
                         that will be copied to the 'to'-instance.
          to-folder-uid: uid of a folder within the 'to'-instance,
                         where the dashboard will be copied/moved to.
                         If `nil` the General-folder of the
                         'to'-instance will be used.
          to-message:    The change-message. "}
  [from-grafana to-grafana dashboard-uid to-folder-uid to-message]
  (let [dashboard        (helper/json->clj
                          (api/get-dashboard-by-uid
                           (-> from-grafana :url)
                           (-> from-grafana :token)
                           dashboard-uid))
        clean-board-data (-> dashboard
                             (get "dashboard")
                             ;; alternative: check for changes before overwriting
                             (dissoc "version")
                             (dissoc "id"))]
    (api/create-update-dashboard (-> to-grafana :url)
                                 (-> to-grafana :token)
                                 (helper/clj->json {"dashboard" clean-board-data
                                                    "message"   to-message
                                                    ;; alternative: check for changes before overwriting
                                                    "overwrite" true
                                                    ;; folder must exist, otherwise it throws an exception
                                                    "folderUid" to-folder-uid}))))

(defn copy-alert
  ^{:doc "Copy (create/update) a rule to a given folder.

          instance:     url and token as GrafanaInstance record.
          rule-to-copy: the rule to copy.
          folder-uid:   the folder-uid where the rule should be copied to."}
  ;; Note: inefficient to run the available-alerts within copy-alert for every
  ;; rule within copy-alerts
  ;; However, if the alert-rules-list contains duplicates, we can handle it.
  [instance folder-uid rule-to-copy-with-id]
  (let [available-alerts (helper/json->clj
                          (api/get-all-alert-rules (-> instance :url)
                                                   (-> instance :token)))
        ;; the id within a grafana-instance needs to be unique
        ;; if the rule-to-copy contains an already existing "id" the copy fails
        ;; be aware: we have id, uid, title as some identifiers
        rule-to-copy (dissoc rule-to-copy-with-id "id")]
    (if (some (fn [available-alert]
                (= (get available-alert "uid")
                   (get rule-to-copy   "uid")))
              available-alerts)
      (api/update-alert-rule (-> instance :url)
                             (-> instance :token)
                             (get rule-to-copy "uid")
                             (helper/clj->json
                              (assoc rule-to-copy "folderuid" folder-uid)))
      (api/create-alert-rule (-> instance :url)
                             (-> instance :token)
                             (helper/clj->json
                              (assoc rule-to-copy "folderuid" folder-uid))))))

(defn copy-alerts
  ^{:doc "Copy (create/update) all alert-rules associated with a dashboard.

          from-instance: url and token as GrafanaInstance record.
          to-instance:   url and token as GrafanaInstance record.
          dashboard-uid: uid of a dashboard in the 'from'-instance
          to-folder-uid: uid of a folder in the 'to'-instance, where the
                         alert-rules should be copied to."}
  [from-instance to-instance dashboard-uid to-folder-uid]
  (let [alert-rules (find-dashboard-related-alert-rules from-instance dashboard-uid)]
      ;; Note: folder must exist (otherwise rule will be added but cannot be
      ;; seen in the gui) the call will fail with an exception if the to-folder-uid
      ;; is not available
    (api/get-folder-by-folder-uid (-> to-instance :url)
                                  (-> to-instance :token)
                                  to-folder-uid)
    (run! (fn [alert] (copy-alert to-instance to-folder-uid alert)) alert-rules)))

(defn copy-panel
  [grafana-instance panel folder-uid]
  ^{:doc "Copy (create/update) a library-panel.

          instance:   url and token as GrafanaInstance record.
          panel:      the panel to copy.
          folder-uid: uid of a folder in the instance, where the
                      library-panel should be copied to."}
    ;; Note: inefficient to run the available-panels within copy-panel for every
    ;; panel within copy-panels
    ;; However, if the panels-list contains duplicates, we can handle it.
  (let [panel-uid (get panel "uid")
        available-panel-uids (map (fn [available-panel] (get available-panel "uid"))
                                  (get-in (helper/json->clj
                                           (api/get-library-panels (-> grafana-instance :url)
                                                                   (-> grafana-instance :token)))
                                          ["result" "elements"]))
        adjusted-panel (-> panel
                           (dissoc "id" "folderId" "meta")
                           (assoc "folderUid" folder-uid))]
    (if (some #(= panel-uid %) available-panel-uids)
      ;; before we can update the panel
      ;; we need to have its most recent version and put this version in the patch
      (let [panel-version (get-in (helper/json->clj
                                   (api/get-library-element-by-uid (-> grafana-instance :url)
                                                                   (-> grafana-instance :token)
                                                                   panel-uid))
                                  ["result" "version"])]
        (api/update-library-element (-> grafana-instance :url)
                                    (-> grafana-instance :token)
                                    panel-uid
                                    (helper/clj->json (assoc adjusted-panel "version" panel-version))))
      (api/create-library-element (-> grafana-instance :url)
                                  (-> grafana-instance :token)
                                  (helper/clj->json adjusted-panel)))))

(defn copy-panels
  ^{:doc "Copy (create/update) all library-panels associated with a dashboard.

          from-instance: url and token as GrafanaInstance record.
          to-instance:   url and token as GrafanaInstance record.
          dashboard-uid: uid of a dashboard in the 'from'-instance
          to-folder-uid: uid of a folder in the 'to'-instance, where the
                         library-panels should be copied to."}
  [from-instance to-instance dashboard-uid to-folder-uid]
  (let [panels (find-dashboard-related-panels from-instance dashboard-uid)]
    (run! (fn [panel] (copy-panel to-instance panel to-folder-uid)) panels)))

(defn copy
  ^{:doc "Based on the given arguments, copy a dashboard and/or its
          associated alert-rules and/or panels.

          args: Provided arguments, as Copy-Arguments record."}
  [args]
  ;; if associated panels aren't there, the dashboard copy will fail
  (when (-> args :panels)
    (helper/log "copy panels")
    (copy-panels (-> args :from-instance)
                 (-> args :to-instance)
                 (-> args :board-uid)
                 (-> args :to-panels-folder-uid)))
  (when (-> args :board)
    (helper/log "copy dashboard")
    (copy-dashboard (-> args :from-instance)
                    (-> args :to-instance)
                    (-> args :board-uid)
                    (-> args :to-board-folder-uid)
                    (-> args :to-message)))
  (when (-> args :alerts)
    (helper/log "copy alert-rules")
    (copy-alerts (-> args :from-instance)
                 (-> args :to-instance)
                 (-> args :board-uid)
                 (-> args :to-alerts-folder-uid))))

(defn- deleted? [m]
  (get m "isDeleted" false))

(defn- title= [title m]
  (= title (get m "title")))

;; TODO: An idea to resolve the ambiguity could be
;; to do a dry run of the whole thing persisting the
;; chosen dashboard and folder uids
;; we need to run the real thing without support
;; of a user. Or without the need to be be an
;; interactive program.

(defn choose-dashboard-metadata
  "Searches a dashboard on a given [[grafana-instance]] by using a given
   [[dashboard-title]] as query string returning the dashboard metadata, if the
   search yields an unambiguous result.  If the search yields an ambiguous or no
   result at all, this function throws an Exception."
  [grafana-instance dashboard-title]
  (let [dashboard-candidates       (->> dashboard-title
                                        (api/find-dashboards-by-query
                                         (:url grafana-instance)
                                         (:token grafana-instance))
                                        (helper/json->clj)
                                        (remove deleted?)
                                        (filter (partial title= dashboard-title)))
        dashboard-candidates-count (count dashboard-candidates)
        no-dashboard-candidate?    (zero? dashboard-candidates-count)
        dashboard-unambiguous?     (= 1 dashboard-candidates-count)
        dashboard-ambiguous?       (< 1 dashboard-candidates-count)
        dashboard-metadata         (when dashboard-unambiguous?
                                     (first dashboard-candidates))]
    (cond no-dashboard-candidate?
          (throw (ex-info (str "No dashboard with the following title was found: "
                               dashboard-title)
                          {:dashboard-title dashboard-title
                           :grafana-url     (:url grafana-instance)}))

          dashboard-ambiguous?
          (do
            ;; TODO: We need a convenient way to choose the
            ;; dashboard to copy or to refine the search
            (pprint/print-table ["title" "uid" "url" "description" "folderTitle" "folderUrl"]
                                dashboard-candidates)
            (throw (ex-info (str "More than one (" dashboard-candidates-count
                                 ") dashboard was found using the search query: " dashboard-title)
                            {:dashboard-title                  dashboard-title
                             :dashboard-candidates             dashboard-candidates
                             :grafana-url                      (:url grafana-instance)})))

          dashboard-unambiguous?
          dashboard-metadata

          :else
          (throw (ex-info "Unexpected choose-dashboard-meta result!"
                          {:dashboard-candidates-count       dashboard-candidates-count
                           :dashboard-candidates             dashboard-candidates})))))

(defn choose-folder-uid
  "Searches a folder on a given [[grafana-instance]] by using a given
  [[folder-title]] as query string returning a folder uid, if the search yields
  an unambiguous result. If the search yields no result at all this function
  creates a folder with the title [[folder-title]] and returns its uid. If the
  search yields an ambiguous result, this function throws an exception."
  [grafana-instance folder-title]
  (let [folder-candidates       (->> folder-title
                                     (api/find-folders-by-query (:url grafana-instance)
                                                                (:token grafana-instance))
                                     (helper/json->clj)
                                     (remove deleted?)
                                     (filter (partial title= folder-title)))
        folder-candidates-count (count folder-candidates)
        no-folder-candidate?    (zero? folder-candidates-count)
        folder-unambiguous?     (= 1 folder-candidates-count)
        folder-ambiguous?       (< 1 folder-candidates-count)
        folder-uid              (when folder-unambiguous?
                                  (get (first folder-candidates) "uid"))]
    (cond no-folder-candidate?
          (do (api/create-folder (:url grafana-instance)
                                 (:token grafana-instance)
                                 folder-title)
              (choose-folder-uid grafana-instance folder-title))

          folder-ambiguous?
          (do
            ;; TODO: we need a more convenient way to resolve
            ;; the ambiguity of the folder-ccandidates
            ;; the user should be able to choose
            ;; a folder conveniently
            (pprint/print-table ["title" "uid" "url"]
                                folder-candidates)
            (throw (ex-info (str "More than one (" folder-candidates-count
                                 ") folder was found using the search query: " folder-title)
                            {:folder-title      folder-title
                             :folder-candidates folder-candidates
                             :grafana-url       (:url grafana-instance)})))

          folder-unambiguous?
          folder-uid

          :else
          (throw (ex-info "Unexpected choose-folder-uid result!"
                          {:folder-candidates-count folder-candidates-count
                           :folder-candidates       folder-candidates})))))

(defn choose-panel-folder-title
  "Returns the title of the folder the given [[panels]] are located in, if all
   the given [[panels]] are located in the same folder.  If the panels are
   located in more then one folder this function throws an exception."
  [panels]
  (let [panels-by-folder (->> panels
                              (map #(assoc %
                                           :folder-identification
                                           {:title (get-in % ["meta" "folderName"])
                                            :uid   (get-in % ["meta" "folderUid"])}))
                              (group-by :folder-identification))
        panels-folders   (keys panels-by-folder)

        all-panels-located-in-same-folder? (->> panels-folders
                                                (distinct)
                                                (count)
                                                (= 1))]
    (if all-panels-located-in-same-folder?
      (:title (first panels-folders))
      (throw (ex-info (str "The panels are located in different folders: "
                           (pr-str panels-folders))
                      {:panels           panels
                       :panels-by-folder panels-by-folder})))))

(defn choose-alert-folder-uid
  "Returns the uid of the folder the given [[alerts]] are located in, if all
   the given [[alerts]] are located in the same folder.  If the alerts are
   located in more then one folder this function throws an exception."
  [alerts]
  (let [alerts-by-folder (->> alerts
                              (map #(assoc %
                                           :folder-identification
                                           {:uid (get % "folderUID")}))
                              (group-by :folder-identification))
        alerts-folders   (keys alerts-by-folder)

        all-alerts-located-in-same-folder? (->> alerts-folders
                                                (distinct)
                                                (count)
                                                (= 1))]
    (if all-alerts-located-in-same-folder?
      (:uid (first alerts-folders))
      (throw (ex-info (str "The alerts are located in different folders: "
                           (pr-str alerts-folders))
                      {:alerts           alerts
                       :alerts-by-folder alerts-by-folder})))))

(defn convenient-copy
  "Copy a dashboard titled [[dashboard-title]] from [[from-grafana-instance]] to
   [[to-grafana-instance]] conveniently.  Also copy the library panels and
   alerts the dashboard depends on.  The convenience this function provides
   consists of several simple heuristics: 1. It tries to find a dashboard titled
   [[dashboard-title]] on the given [[from-grafana-instance]].  2. It tries to
   identify the folder to copy the dashboard to using the name of the folder the
   dashboard titled [[dashboard-title]] is located on the
   [[from-grafana-instance]]. See the fn [[choose-folder-uid]] for details.  3.
   It creates the folder to copy the dashboard to, if it does not find one on
   [[to-grafana-instance]], which has the same title as on the
   [[from-grafana-instance]].  4. It checks if the library panels and alerts the
   dashboard depends on are all located in the same folder. See
   [[choose-panel-folder-tiele]] and [[choose-alert-folder-uid]] for details.
   5. If the folders where the library panels and alerts are located in
   [[from-grafana-instance]] do not exist on [[to-grafana-instance]], they are
   created using the title of the folders on [[from-grafana-instance]]. See
   [[choose-folder-uid]] for details."
  [from-grafana-instance to-grafana-instance dashboard-title & {:as   _options
                                                                :keys [to-message
                                                                       to-board-folder-uid
                                                                       board-uid]}]
  (let [dashboard-uid          (or board-uid
                                   (get (choose-dashboard-metadata from-grafana-instance
                                                                   dashboard-title)
                                        "uid"))
        dashboard-response     (helper/json->clj
                                (api/get-dashboard-by-uid
                                 (:url from-grafana-instance)
                                 (:token from-grafana-instance)
                                 dashboard-uid))
        dashboard-folder-title (get-in dashboard-response ["meta" "folderTitle"])
        ;; NOTE: Since the alerts and panels "normally" live in the same folder as the
        ;; dashboard itself. This is due to access permissions, since access is managed using folders.
        ;; So it seems like we can use the dashboard-folder also as the target
        ;; for the panels and alerts.
        ;; Anyways this is not enough, we should check if the folder with the same
        ;; title exists on the target grafana instance and if the found folder
        ;; uid is the same as the dashboard-folder-uid
        ;; BUT: What to do if the checks fail?
        dashboard-folder-uid   (or to-board-folder-uid
                                   (choose-folder-uid to-grafana-instance
                                                      dashboard-folder-title))
        message                (or to-message
                                   (str "Copy " (get dashboard-response "title")
                                        " (uid: " (get dashboard-response "uid") ") "
                                        "from " (:url from-grafana-instance)
                                        "to " dashboard-folder-title
                                        " (uid: " dashboard-folder-uid ") "
                                        "at " (:url to-grafana-instance) "."))

        panels                (find-dashboard-related-panels from-grafana-instance dashboard-uid)
        has-dependent-panels? (not-empty panels)
        panels-folder-title   (when has-dependent-panels? (choose-panel-folder-title panels))
        panels-folder-uid     (when has-dependent-panels? (choose-folder-uid to-grafana-instance
                                                                             panels-folder-title))

        source-alerts              (find-dashboard-related-alert-rules from-grafana-instance dashboard-uid)
        has-dependent-alerts?      (not-empty source-alerts)
        source-alerts-folder-uid   (when has-dependent-alerts? (choose-alert-folder-uid source-alerts))
        source-alerts-folder       (when has-dependent-alerts?
                                     (api/get-folder-by-folder-uid (-> from-grafana-instance :url)
                                                                   (-> from-grafana-instance :token)
                                                                   source-alerts-folder-uid))
        source-alerts-folder-title (when has-dependent-alerts?
                                     (get (helper/json->clj source-alerts-folder) "title"))
        target-alerts-folder-uid   (when has-dependent-alerts?
                                     (choose-folder-uid to-grafana-instance
                                                        source-alerts-folder-title))]
    (when has-dependent-panels?
      (copy-panels from-grafana-instance
                   to-grafana-instance
                   dashboard-uid
                   panels-folder-uid))
    (copy-dashboard from-grafana-instance
                    to-grafana-instance
                    dashboard-uid
                    dashboard-folder-uid
                    message)
    (when has-dependent-alerts?
      (copy-alerts from-grafana-instance
                   to-grafana-instance
                   dashboard-uid
                   target-alerts-folder-uid))))

;; <<< COPY

;; >>> ADJUST

(defn standard-target->target
  ^{:doc "Create target from reference-target.
 Use datasource-uid also as refId."}
  [reference-target uid]
  ;; target { "datasource" { "uid" <datasource-uid>, ...},
  ;;          "refId" <ref-id>,
  ;;          ...}
  (assert (contains? (get reference-target "datasource") "uid")
          (str "reference-target does not have the expected structure { \"datasource\" { \"uid\" ... }}. "
               "Current reference-target: " reference-target))
  (assert (contains? reference-target "refId")
          (str "reference-target does not have the expected structure { \"refId\" ... }. "
               "Current reference-target: " reference-target))

  (assoc (assoc-in reference-target ["datasource" "uid"] uid)
         "refId" uid))

(defn target->target-from-file
  ^{:doc "Create target from reference-target. Creation of the target is
  determined by the `expert-string`. The `expert-string` has the form: `\"{:path
  <path to a function target->target in a file> :data '<some-data>'}\"`The
  `expert-string` is read with `clojure.edn/read-string`. The function
  `target->target` is loaded via `(load-string (slurp file-path))`. The function
  `target->target` is provided with the arguments `reference-target`, `uid` and
  `data`."}
  [expert-string reference-target uid]
  (let [path-data-map (edn/read-string expert-string)
        f-target->target (load-string (slurp (:path path-data-map)))
        data (:data path-data-map)]
    (f-target->target reference-target uid data)))

(defn create-targets
  ^{:doc "Create targets based on a reference-target and given datasource-uids.
  By default uses `standard-target->target` to create targets. When an
  `expert-string` is provided, more advanced targets can be created. See
  `target->target-from-file` and the README for more information."}
  [reference-target datasource-uids expert-string]

  (assert (not= [""] datasource-uids)
          (str "datasource-uids are empty:\n"
               datasource-uids
               "\nThis would destroy the structure of your current library panel."
               "\nPlease provide at least one datasource-uid."))
  (assert (apply distinct? datasource-uids)
          (str "datasource-uids are not distinct:\n"
               datasource-uids
               "\nPlease provide only distinct datasource-uids."))

  (if (nil? expert-string)
    (map (partial standard-target->target reference-target) datasource-uids)
    (map (partial target->target-from-file expert-string reference-target) datasource-uids)))

(defn create-patch
  ^{:doc "Creates a patch for a panel, providing the panel version and kind
  (patch-must-haves) and the adjustd model, where the targets are replaced,
  based on the first target of the provided panel, the provided datasource-uids
  and optionally an expert-string."}
  [panel datasource-uids expert-string]
  ;; panel map:
  ;; {"result": { "version": <version>,
  ;;              "kind": 1,
  ;;              "model": { "targets": [ ... ],
  ;;                         ...},
  ;;              ...}}
  ;; A panel patch must have:
  ;;   - the same version as the last in the grafana-instance
  ;;   - the kind information (library panel -> 1; library variables -> 2)
  (let [version           (get-in panel ["result" "version"])
        kind              (get-in panel ["result" "kind"]) ;; expected to be 1
        old-model         (get-in panel ["result" "model"])
        ref-target (first (get-in panel ["result" "model" "targets"]))
        new-targets (create-targets ref-target datasource-uids expert-string)
        new-model   (assoc old-model "targets" new-targets)]
    {"model"   new-model
     "version" version
     "kind"    kind}))

(defn adjust-library-panel
  ^{:doc "Adjust a given library-panel within a grafana-instance, where the
  targets of the panel-model are replaced based on the first target of a given
  library-panel, the provided datasource-uids and optionally an expert-string."}
  [grafana-instance panel-uid datasource-uids expert-string]
  (let [panel (helper/json->clj
               (api/get-library-element-by-uid
                (-> grafana-instance :url)
                (-> grafana-instance :token)
                panel-uid))
        patch (create-patch panel datasource-uids expert-string)]
    (api/update-library-element (-> grafana-instance :url)
                                (-> grafana-instance :token)
                                panel-uid
                                (helper/clj->json patch))))

(defn adjust
  ^{:doc "Adjust a given library-panel within a grafana-instance, where the
  targets of the panel-model are replaced based on the first target of given
  library-panel, the provided datasource-uids and optionally an expert-string.
  args: Provided arguments, as Adjust-Arguments record."}
  [args]
  (adjust-library-panel (-> args :grafana-instance)
                        (-> args :panel-uid)
                        (str/split (-> args :datasource-uids) #",")
                        (-> args :expert-data))
  ;; if we are here, adjusting the panel was successful
  (println "Adjusted."))

;; <<< ADJUST
