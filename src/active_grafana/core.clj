(ns active-grafana.core
  (:require [active-grafana.grafana-api :as api]
            [active-grafana.helper      :as helper]
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
                  (-> grafana-instance :url  )
                  (-> grafana-instance :token)))]
    (println "First 1000 folders:")
    (pprint/print-table ["title" "uid"] folders)))


(defn show-library-panels
  ^{:doc "Show for a given grafana-instance the name, uid and folder-uid of the
          first 100 library panels"}
  [grafana-instance]
  (let [library-panels (helper/json->clj
                        (api/get-library-panels
                         (-> grafana-instance :url  )
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
                      (-> grafana-instance :url  )
                      (-> grafana-instance :token)))]
    ;; rule-structure:
    ;; { ..., "uid" "my-uid", "annotations" { ..., "__dashboardUid__" "dash-uid", ...}, ...}
    ;; note: there are several other fields within "annotations" where you could add a dashboard-uid,
    ;; but this is the expected one
    ;; alternative: search within all values in the annotations map for the dashboard-uid
    (filter (fn [rule] (= dashboard-uid
                          (-> rule
                              (get "annotations")
                              (get "__dashboardUid__" ))))
            alert-rules)))

(defn show-dashboard-rules
  ^{:doc "Show alert-rules related to a specific dashboard.

          grafana-instance: url and token as GrafanaInstance record.
          dashboard-uid:    to search for in the alert-rules."}
  [grafana-instance board-uid]
  (let [alert-rules (find-dashboard-related-alert-rules grafana-instance board-uid)]
    (println (str "Alert rule-uids related to dashboard: " board-uid))
    (pprint/print-table ["uid" "title" ] alert-rules)))

;; FIXME: Is there any better way to find dashboard related library panels

;; alternative: get all library-panel - for each library-panel, search all connections, check whether connection is dashboard-uid
;; note: you only get the first 100 library-panels
(defn find-dashboard-related-panels
  ^{:doc "Find library panels that are related to a specific dashboard.

          grafana-instance: url and token as GrafanaInstance record.
          dashboard-uid:    to search for in the panels."}
  [grafana-instance dashboard-uid]
  (let [dashboard (helper/json->clj
                   (api/get-dashboard-by-uid (-> grafana-instance :url  )
                                             (-> grafana-instance :token)
                                             dashboard-uid))

        ;; go through all panels and search for "libraryPanel" entries
        ;; (contains nil for every non-library-panel)
        panel-uids (remove nil? (map (fn [panel]
                                       (get-in panel ["libraryPanel" "uid"]))
                                     (get-in dashboard ["dashboard" "panels"])))]
    ;; get cannot be nil - since the dashboard points to this library-panel
    (map (fn [panel-uid] (get (helper/json->clj
                               (api/get-library-element-by-uid (-> grafana-instance :url  )
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
    (pprint/print-table ["uid" "name" ] panels)))

(defn copy-show
  ^{:doc "Based on the given arguments, print information about the first 1000
          dashboards and/or the first 1000 folders and/or the first 100 library panels.

          args: Provided arguments, as Copy-Arguments record. "}
  [args]
  (when (and (-> args :show-boards) (-> args :from))
    (helper/log "show from-dashboards")
    (show-dashboards (-> args :from-instance)))
  (when (and (-> args :show-boards) (-> args :to))
    (helper/log "show to-dashboards")
    (show-dashboards (-> args :to-instance)))

  (when (and (-> args :show-folders) (-> args :from))
    (helper/log "show from-folders")
    (show-folders (-> args :from-instance)))
  (when (and (-> args :show-folders) (-> args :to))
    (helper/log "show to-folders")
    (show-folders (-> args :to-instance)))

  (when (and (-> args :show-panels) (-> args :from))
    (helper/log "show from-panels")
    (show-library-panels (-> args :from-instance)))
  (when (and (-> args :show-panels) (-> args :to))
    (helper/log "show to-panels")
    (show-library-panels (-> args :to-instance)))

  (when (and (-> args :show-board-rules) (-> args :from))
    (helper/log "show from-dashboard related rules")
    (show-dashboard-rules (-> args :from-instance)
                          (-> args :board-uid)))
  (when (and (-> args :show-board-rules) (-> args :to))
    (helper/log "show to-dashboard related rules")
    (show-dashboard-rules (-> args :to-instance)
                          (-> args :board-uid)))

  (when (and (-> args :show-board-panels) (-> args :from))
    (helper/log "show from-dashboard related library panels")
    (show-dashboard-panels (-> args :from-instance)
                           (-> args :board-uid)))
  (when (and (-> args :show-board-panels) (-> args :to))
    (helper/log "show to-dashboard related library panels")
    (show-dashboard-panels (-> args :to-instance)
                           (-> args :board-uid))))

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
          folder-uid:    uid of a folder within the 'to'-instance,
                         where the dashboard will be copied/moved to.
                         If `nil` the General-folder of the
                         'to'-instance will be used.
          message:       The change-message. "}
  [from-grafana to-grafana dashboard-uid folder-uid message]
  (let [dashboard        (helper/json->clj
                          (api/get-dashboard-by-uid
                           (-> from-grafana :url  )
                           (-> from-grafana :token)
                           dashboard-uid))
        clean-board-data (-> dashboard
                             (get "dashboard")
                             ;; alternative: check for changes before overwriting
                             (dissoc "version")
                             (dissoc "id"))]
    (api/create-update-dashboard (-> to-grafana :url  )
                                 (-> to-grafana :token)
                                 (helper/clj->json {"dashboard" clean-board-data
                                                    "message"   message
                                                    ;; alternative: check for changes before overwriting
                                                    "overwrite" true
                                                    ;; folder must exist, otherwise it throws an exception
                                                    "folderUid" folder-uid}))))

(defn copy-rule
  ^{:doc "Copy (create/update) a rule to a given folder.

          instance:     url and token as GrafanaInstance record.
          rule-to-copy: the rule to copy.
          folder-uid:   the folder-uid where the rule should be copied to."}
  ;; Note: inefficient to run the available-rules within copy-rule for every
  ;; rule within copy-rules
  [instance folder-uid rule-to-copy-with-id]
  (let [available-rules (helper/json->clj
                         (api/get-all-alert-rules (-> instance :url  )
                                                  (-> instance :token)))
        ;; the id within a grafana-instance needs to be unique
        ;; if the rule-to-copy contains an already existing "id" the copy fails
        ;; be aware: we have id, uid, title as some identifiers
        rule-to-copy (dissoc rule-to-copy-with-id "id")]
    (if (some (fn [available-rule]
                (= (get available-rule "uid")
                   (get rule-to-copy   "uid")))
                available-rules)
      (api/update-alert-rule (-> instance :url  )
                             (-> instance :token)
                             (get rule-to-copy "uid")
                             (helper/clj->json
                              (assoc rule-to-copy "folderuid" folder-uid)))
      (api/create-alert-rule (-> instance :url  )
                             (-> instance :token)
                             (helper/clj->json
                              (assoc rule-to-copy "folderuid" folder-uid))))))

(defn copy-rules
  ^{:doc "Copy (create/update) all alert-rules associated with a dashboard.

          from-instance: url and token as GrafanaInstance record.
          to-instance:   url and token as GrafanaInstance record.
          dashboard-uid: uid of a dashboard in the 'from'-instance
          folder-uid:    uid of a folder in the 'to'-instance, where the
                         alert-rules should be copied to."}
  [from-instance to-instance dashboard-uid folder-uid]
  (let [alert-rules (find-dashboard-related-alert-rules from-instance dashboard-uid)]
      ;; Note: folder must exist (otherwise rule will be added but cannot be
      ;; seen in the gui) the call will fail with an exception if the folder-uid
      ;; is not available
      (api/get-folder-by-folder-uid (-> to-instance :url  )
                                    (-> to-instance :token)
                                    folder-uid)
      (run! (fn [rule] (copy-rule to-instance folder-uid rule)) alert-rules)))

(defn copy-panel
  [grafana-instance panel]
  ^{:doc "Copy (create/update) a library-panel.

          instance: url and token as GrafanaInstance record.
          panel:    the panel to copy."}
    ;; Note: inefficient to run the available-panels within copy-panel for every
    ;; panel within copy-panels
  (let [available-panel-uids (map (fn [panel] (get panel "uid"))
                                  (get-in (helper/json->clj
                                           (api/get-library-panels (-> grafana-instance :url  )
                                                                   (-> grafana-instance :token)))
                                          ["result" "elements"]))
        panel-uid (get panel "uid")]
    (if (some #(= panel-uid %) available-panel-uids)
      ;; before we can update the panel
      ;; we need to have its most recent version and put this version in the patch
      (let [panel-version (get-in (helper/json->clj
                                   (api/get-library-element-by-uid (-> grafana-instance :url  )
                                                                   (-> grafana-instance :token)
                                                                   panel-uid))
                                  ["result" "version"])]
        (api/update-library-element (-> grafana-instance :url  )
                                    (-> grafana-instance :token)
                                    (get panel "uid")
                                    (helper/clj->json (assoc panel "version" panel-version))))
      (api/create-library-element (-> grafana-instance :url  )
                                  (-> grafana-instance :token)
                                  (helper/clj->json panel)))))

(defn copy-panels
  [from-instance to-instance dashboard-uid]
  (let [panels (find-dashboard-related-panels from-instance dashboard-uid)]
    (run! (fn [panel] (copy-panel to-instance panel)) panels)))

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
                   (-> args :board-uid)))
  (when (-> args :board)
      (helper/log "copy dashboard")
      (copy-dashboard (-> args :from-instance   )
                      (-> args :to-instance     )
                      (-> args :board-uid       )
                      (-> args :board-folder-uid)
                      (-> args :message         )))
  (when (-> args :rules)
      (helper/log "copy alert-rules")
      (copy-rules (-> args :from-instance   )
                  (-> args :to-instance     )
                  (-> args :board-uid       )
                  (-> args :rules-folder-uid))))

;; <<< COPY

;; >>> ADJUST

(defn create-targets
  ^{:doc "Create targets from a reference-target.
          Use datasource-uids also as refId."}
  [reference-target datasource-uids]
  ;; target { datasource { "uid" <datasource-uid>, ...},
  ;;          refId <ref-id>,
  ;;          ...}
  (assert (contains? (get reference-target "datasource") "uid")
          (str "reference-target does not have the expected structure { datasource { \"uid\" ... }}\n"
               "current reference-target:\n"
               (println reference-target)))
  (assert (contains? reference-target "refId")
          (str "target does not have the expected structure { refId ... }\n"
               "current reference-target:\n"
               (println reference-target)))

  (map (fn [uid]
         (assoc (assoc-in reference-target ["datasource" "uid"] uid)
                "refId" uid)) datasource-uids))

(defn create-patch
  ^{:doc "Creates a patch for a panel, providing the panel version and kind
          (patch-must-haves)
          and the adjustd model, where the targets are replaced, based on the
          first target of the provided panel, and the provided datasource-uids."}
  [panel datasource-uids]
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
        new-targets (create-targets ref-target datasource-uids)
        new-model   (assoc old-model "targets" new-targets)]
    {"model"   new-model
     "version" version
     "kind"    kind}))

(defn adjust-library-panel
  ^{:doc "Adjust a given library-panel within a grafana-instance, where the
          targets of the panel-model are replaced based on the first target
          of given library-panel and the provided datasource-uids."}
  [grafana-instance panel-uid datasource-uids]
  (let [panel (helper/json->clj
                (api/get-library-element-by-uid
                 (-> grafana-instance :url  )
                 (-> grafana-instance :token)
                 panel-uid))
        patch (create-patch panel datasource-uids)]
    (api/update-library-element (-> grafana-instance :url  )
                                (-> grafana-instance :token)
                                panel-uid
                                (helper/clj->json patch))))

(defn adjust
  ^{:doc "Adjust a given library-panel within a grafana-instance, where the
          targets of the panel-model are replaced based on the first target
          of given library-panel and the provided datasource-uids.

          args: Provided arguments, as Adjust-Arguments record."}
  [args]
   (adjust-library-panel (-> args :grafana-instance)
                         (-> args :panel-uid       )
                         (str/split (-> args :datasource-uids) #" "))
  ;; if we are here, adjusting the panel was successful
  (println "Adjusted."))

;; <<< ADJUST
