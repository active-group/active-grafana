(ns active-grafana.core
  (:require [active-grafana.grafana-api :as api]
            [active-grafana.helper :as helper]
            [active-grafana.settings :as settings]))

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
                           (settings/grafana-instance-url   from-grafana)
                           (settings/grafana-instance-token from-grafana)
                           dashboard-uid))
        clean-board-data (-> dashboard
                             (get "dashboard")
                             ;; alternative: check for changes before overwriting
                             (dissoc "version")
                             (dissoc "id"))]
    (api/create-update-dashboard (settings/grafana-instance-url   to-grafana)
                                 (settings/grafana-instance-token to-grafana)
                                 (helper/clj->json {"dashboard" clean-board-data
                                                    "message"   message
                                                    ;; alternative: check for changes before overwriting
                                                    "overwrite" true
                                                    ;; folder must exist, otherwise it throws an exception
                                                    "folderUid" folder-uid}))))

(defn find-dashboard-related-alert-rules
  ^{:doc "Find alert-rules that are related to a specific dashboard.
          Note: searches for the uid within `annotations` > `__dashboardUid__`.

          grafana-instance: url and token as GrafanaInstance record.
          dashboard-uid:    to search for in the alert-rules."}
  [grafana-instance dashboard-uid]
  (let [alert-rules (helper/json->clj
                     (api/get-all-alert-rules
                      (settings/grafana-instance-url   grafana-instance)
                      (settings/grafana-instance-token grafana-instance)))]
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

(defn copy-rule
  [instance folder-uid rule-to-copy]
  ^{:doc "Copy (create/update) a rule to a given folder.

          instance:     url and token as GrafanaInstance record.
          rule-to-copy: the rule to copy.
          folder-uid:   the folder-uid where the rule should be copied to."}
  ;; Note: inefficient to run the available-rules within copy-rule for every
  ;; rule within copy-rules
  (let [available-rules (helper/json->clj
                         (api/get-all-alert-rules (settings/grafana-instance-url   instance)
                                                  (settings/grafana-instance-token instance)))]
    (if (some (fn [available-rule]
                (= (get available-rule "uid")
                   (get rule-to-copy   "uid")))
                available-rules)
      (api/update-alert-rule (settings/grafana-instance-url   instance)
                             (settings/grafana-instance-token instance)
                             (get rule-to-copy "uid")
                             (helper/clj->json
                              (assoc rule-to-copy "folderuid" folder-uid)))
      (api/create-alert-rule (settings/grafana-instance-url   instance)
                             (settings/grafana-instance-token instance)
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
    (do
      ;; Note: folder must exist (otherwise rule will be added but cannot be
      ;; seen in the gui) the call will fail with an exception if the folder-uid
      ;; is not available
      (api/get-folder-by-folder-uid (settings/grafana-instance-url   to-instance)
                                    (settings/grafana-instance-token to-instance)
                                    folder-uid)
      (run! (fn [rule] (copy-rule to-instance folder-uid rule)) alert-rules))))

(defn copy
  ^{:doc "Based on the given arguments, copy a dashboard and, if requested, its
          associated alert-rules.

          args: Provided arguments, as Arguments record. "}
  [args]
  (when (settings/arguments-board args)
    (do
      (helper/log "copy dashboard")
      (copy-dashboard (settings/arguments-from-instance    args)
                      (settings/arguments-to-instance      args)
                      (settings/arguments-board-uid        args)
                      (settings/arguments-board-folder-uid args)
                      (settings/arguments-message          args))))
  (when (settings/arguments-rules args)
    (do
      (helper/log "copy alert-rules")
      (copy-rules (settings/arguments-from-instance    args)
                  (settings/arguments-to-instance      args)
                  (settings/arguments-board-uid        args)
                  (settings/arguments-rules-folder-uid args)))))
