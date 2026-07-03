(ns active-grafana.examples
  (:require [active-grafana.settings :as settings]
            [active-grafana.helper :as helper]
            [clojure.string :as str]))

;; TODO: stuff like the uids and titles need to
;; be in a specific form to work for our tests

(def grafana-a-instance (settings/->Grafana-Instance "http://irrelevant.url-a" "irrelevant-token-a"))

(def grafana-b-instance (settings/->Grafana-Instance "http://irrelevant.url-b" "irrelevant-token-b"))

(def dashboard-title "Simple")

(def dashboard-uid "adv5c5m")

(def folder-title "My folder")

(def folder-uid "dflyuecbw3cw0f")

(defn slug [s]
  (-> s
      (str/trim)
      (str/lower-case)
      (str/replace #" +" "-")))

(defn non-neg-int? [i]
  (or (zero? i) (pos-int? i)))

(defn random-string
  ([] (random-string nil))
  ([prefix]
   (if (nil? prefix)
     (random-uuid)
     (str prefix "-" (random-uuid)))))

(def random-uid (partial random-string "uid"))
(def random-title (partial random-string "title"))
(def random-dashboard-uid (partial random-string "uid"))
(def random-dashboard-title (partial random-string "title"))
(def random-folder-uid (partial random-string "folder-uid"))
(def random-folder-title (partial random-string "folder-title"))
(def random-id (partial rand-int Integer/MAX_VALUE))
(def random-panel-title (partial random-string "panel-title"))
(def random-panel-name (partial random-string "panel-name"))
(def random-panel-uid (partial random-string "panel-uid"))
(def random-version (partial rand-int 20))
(def random-alert-uid (partial random-string "alert-uid"))
(def random-alert-title (partial random-string "alert-title"))

(defn make-compact-dashboard
  ([] (make-compact-dashboard (random-id)))
  ([id] (make-compact-dashboard id {}))
  ([id {:keys [uid
               title
               folder-uid
               folder-id
               folder-title]
        :or   {uid          (random-uid)
               title        (random-title)
               folder-uid   (random-folder-uid)
               folder-id    (random-id)
               folder-title (random-folder-title)}}]
   {:pre [(non-neg-int? id)
          (string? uid)
          (string? title)
          (string? folder-uid)
          (non-neg-int? folder-id)
          (string? folder-title)]}
   {"isStarred"   false,
    "folderTitle" folder-title,
    "url"         (str "/d/" uid "/simple-time-series"),
    "sortMeta"    0,
    "uri"         (str "db/" (slug title))
    "tags"        [],
    "id"          id,
    "uid"         uid,
    "slug"        "",
    "title"       title,
    "type"        "dash-db",
    "isDeleted"   false,
    "folderUrl"   (str "/dashboards/f/"
                       folder-uid
                       "/"
                       (slug folder-title)),
    "folderUid"   folder-uid,
    "folderId"    folder-id,
    "orgId"       1}))

(defn make-dashboard-query-body [titles]
  (vec (map-indexed make-compact-dashboard titles)))

(def get-dashboards-response
  {:headers {},
   :status  200,
   :body    (-> [{:title dashboard-title}
                 {:title "Another Title"}]
                make-dashboard-query-body
                helper/clj->json)})

(defn find-dashboards-by-query-response [dashboards]
  {:headers {},
   :status  200,
   :body    (-> dashboards
                make-dashboard-query-body
                helper/clj->json)})

(defn make-full-dashboard-panel
  ([] (make-full-dashboard-panel (random-id)))
  ([id] (make-full-dashboard-panel id {}))
  ([id {:keys [title name uid]
        :or   {title (random-panel-title)
               name  (random-panel-name)
               uid   (random-panel-uid)}}]
   {:pre [(string? title)
          (string? name)
          (string? uid)]}
   {"gridPos"      {"h" 9, "w" 8, "x" 0, "y" 0},
    "id"           id,
    "libraryPanel" {"name" name, "uid" uid},
    "title"        title}))

(defn make-full-dashboard
  ([] (make-full-dashboard (random-id)))
  ([id] (make-full-dashboard id {}))
  ([id {:keys [uid
               title
               folder-uid
               folder-id
               folder-title
               version]
        :or   {uid          (random-uid)
               title        (random-title)
               folder-uid   (random-folder-uid)
               folder-id    (random-id)
               folder-title (random-folder-title)
               version      (random-version)}}]
   {:pre [(non-neg-int? id)
          (string? uid)
          (string? title)
          (string? folder-uid)
          (non-neg-int? folder-id)
          (string? folder-title)
          (non-neg-int? version)]}
   {"meta"
    {"folderTitle"           folder-title,
     "created"               "2026-05-28T13:37:15Z",
     "url"                   (str "/d/" uid "/" (slug title)),
     "updatedBy"             "admin",
     "hasAcl"                false,
     "isFolder"              false,
     "createdBy"             "admin",
     "slug"                  (slug title),
     "canDelete"             true,
     "expires"               "0001-01-01T00:00:00Z",
     "canAdmin"              true,
     "canEdit"               true,
     "provisionedExternalId" "",
     "type"                  "db",
     "version"               3,
     "folderUrl"             (str "/dashboards/f/" folder-uid "/" folder-title),
     "updated"               "2026-05-28T13:38:06Z",
     "folderUid"             folder-uid,
     "folderId"              folder-id,
     "canSave"               true,
     "canStar"               true,
     "provisioned"           false,
     "apiVersion"            "v0alpha1",
     "annotationsPermissions"
     {"dashboard" {"canAdd" true, "canEdit" true, "canDelete" true}}},
    "dashboard"
    {"editable"             true,
     "liveNow"              false,
     "timezone"             "browser",
     "panels"
     [(make-full-dashboard-panel)],
     "tags"                 [],
     "templating"           {"list" []},
     "id"                   id,
     "uid"                  uid,
     "refresh"              "",
     "graphTooltip"         0,
     "preload"              false,
     "time"                 {"from" "now-6h", "to" "now"},
     "links"                [],
     "annotations"
     {"list"
      [{"builtIn"    1,
        "datasource" {"type" "grafana", "uid" "-- Grafana --"},
        "enable"     true,
        "hide"       true,
        "iconColor"  "rgba(0, 211, 255, 1)",
        "name"       "Annotations & Alerts",
        "type"       "dashboard"}]},
     "title"                title,
     "version"              version,
     "fiscalYearStartMonth" 0,
     "schemaVersion"        42,
     "timepicker"
     {"refresh_intervals"
      ["5s" "10s" "30s" "1m" "5m" "15m" "30m" "1h" "2h" "1d"]}}}))

(defn get-dashboard-by-uid-response [dashboard]
  {:headers {},
   :status 200,
   :body
   (-> (make-full-dashboard 1 dashboard)
       (helper/clj->json))})

(defn make-compact-folder
  ([] (make-compact-folder (random-id)))
  ([id] (make-compact-folder id {}))
  ([id {:keys [uid title]
        :or   {uid   (random-folder-uid)
               title (random-folder-title)}}]
   {:pre [(non-neg-int? id)
          (string? uid)
          (string? title)]}
   {"isStarred" false,
    "url"       (str "/dashboards/f/" uid "/" (slug title)),
    "sortMeta"  0,
    "uri"       (str "db/" (slug title)),
    "tags"      [],
    "id"        id,
    "uid"       uid,
    "slug"      "",
    "title"     title,
    "type"      "dash-folder",
    "isDeleted" false,
    "orgId"     1}))

(defn make-folder-query-body [titles]
  (map-indexed make-compact-folder titles))

(defn find-folders-by-query-response [folders]
  {:headers {},
   :status  200,
   :body    (-> folders
                make-folder-query-body
                helper/clj->json)})

(defn make-full-folder
  ([] (make-full-folder (random-id)))
  ([id] (make-full-folder id {}))
  ([id {:keys [uid title]
        :or   {uid   (random-folder-uid)
               title (random-folder-title)}}]
   {:pre [(non-neg-int? id)
          (string? uid)
          (string? title)]}
   {"created"   "2026-06-11T09:34:01Z",
    "url"       (str "/dashboards/f/" uid "/" (slug title)),
    "updatedBy" "Anonymous",
    "hasAcl"    false,
    "id"        id,
    "createdBy" "Anonymous",
    "uid"       uid,
    "canDelete" true,
    "canAdmin"  true,
    "canEdit"   true,
    "title"     title,
    "version"   1,
    "updated"   "2026-06-11T09:34:01Z",
    "canSave"   true,
    "orgId"     1}))

(def create-folder-response
  {:headers {},
   :status  200,
   :body
   (-> (make-full-folder 1 {:title folder-title})
       (helper/clj->json))})

(defn make-library-element
  ([] (make-library-element (random-id)))
  ([id] (make-library-element id {}))
  ([id {:keys [uid
               name
               version
               folder-id
               folder-uid
               folder-name]
        :or   {uid         (random-panel-uid)
               name        (random-panel-name)
               version     (random-version)
               folder-id   (random-id)
               folder-uid  (random-folder-uid)
               folder-name (random-folder-title)}}]
   {:pre [(non-neg-int? id)
          (string? uid)
          (string? name)
          (non-neg-int? version)
          (non-neg-int? folder-id)
          (string? folder-uid)
          (string? folder-name)]}
   {"model"
    {"libraryPanel"  {"name" name, "uid" uid},
     "fieldConfig"
     {"defaults"
      {"color"    {"mode" "palette-classic"},
       "custom"
       {"drawStyle"         "line",
        "barAlignment"      0,
        "stacking"          {"group" "A", "mode" "none"},
        "lineWidth"         1,
        "axisBorderShow"    false,
        "insertNulls"       false,
        "axisColorMode"     "text",
        "gradientMode"      "none",
        "pointSize"         5,
        "axisCenteredZero"  false,
        "axisLabel"         "",
        "showValues"        false,
        "lineInterpolation" "linear",
        "axisPlacement"     "auto",
        "fillOpacity"       0,
        "barWidthFactor"    0.6,
        "hideFrom"          {"legend" false, "tooltip" false, "viz" false},
        "scaleDistribution" {"type" "linear"},
        "showPoints"        "auto",
        "spanNulls"         false,
        "thresholdsStyle"   {"mode" "off"}},
       "mappings" [],
       "thresholds"
       {"mode"  "absolute",
        "steps" [{"color" "green", "value" nil} {"color" "red", "value" 80}]}},
      "overrides" []},
     "gridPos"       {"h" 8, "w" 12, "x" 0, "y" 0},
     "pluginVersion" "12.3.2",
     "id"            1,
     "datasource"    {"type" "datasource", "uid" "-- Dashboard --"},
     "targets"
     [{"datasource" {"type" "datasource", "uid" "-- Dashboard --"},
       "refId"      "A"}],
     "title"         "New panel",
     "type"          "timeseries",
     "options"
     {"legend"
      {"calcs"       [],
       "displayMode" "list",
       "placement"   "bottom",
       "showLegend"  true},
      "tooltip" {"hideZeros" false, "mode" "single", "sort" "none"}},
     "description"   ""},
    "id"          id,
    "uid"         uid,
    "name"        name,
    "kind"        1,
    "type"        "timeseries",
    "version"     version,
    "meta"
    {"folderName"          folder-name,
     "folderUid"           folder-uid,
     "connectedDashboards" 1,
     "created"             "2026-06-22T15:52:18Z",
     "updated"             "2026-06-22T15:52:18Z",
     "createdBy"
     {"id"        1,
      "name"      "admin",
      "avatarUrl" "/avatar/46d229b033af06a191ff2267bca9ae56"},
     "updatedBy"
     {"id"        1,
      "name"      "admin",
      "avatarUrl" "/avatar/46d229b033af06a191ff2267bca9ae56"}},
    "folderUid"   folder-uid,
    "folderId"    folder-id,
    "orgId"       1,
    "description" ""}))

(defn get-library-panel-by-uid-response
  ([] (get-library-panel-by-uid-response 1))
  ([id] (get-library-panel-by-uid-response id {}))
  ([id panel]
   {:headers {},
    :status  200,
    :body
    (-> {"result" (make-library-element id panel)}
        (helper/clj->json))}))

(defn make-alert-rule
  ([] (make-alert-rule (random-id)))
  ([id] (make-alert-rule id {}))
  ([id {:keys [uid title folder-uid]
        :or   {uid        (random-alert-uid)
               title      (random-alert-title)
               folder-uid (random-folder-uid)}}]
   {:pre [(non-neg-int? id)
          (string? uid)
          (string? title)
          (string? folder-uid)]}
   {"record"                nil,
    "folderUID"             folder-uid,
    "id"                    id,
    "condition"             "B",
    "for"                   "1m",
    "ruleGroup"             "My evaluation group",
    "uid"                   uid,
    "keep_firing_for"       "0s",
    "title"                 title,
    "isPaused"              false,
    "execErrState"          "Error",
    "notification_settings" {"receiver" "grafana-default-email"},
    "noDataState"           "NoData",
    "updated"               "2026-06-22T15:56:27Z",
    "data"
    [{"refId"             "B",
      "queryType"         "",
      "relativeTimeRange" {"from" 0, "to" 0},
      "datasourceUid"     "__expr__",
      "model"
      {"conditions"
       [{"evaluator" {"params" [2], "type" "gt"},
         "operator"  {"type" "and"},
         "query"     {"params" ["B"]},
         "reducer"   {"params" [], "type" "last"},
         "type"      "query"}],
       "datasource"    {"type" "__expr__", "uid" "__expr__"},
       "expression"    "A",
       "intervalMs"    1000,
       "maxDataPoints" 43200,
       "refId"         "B",
       "type"          "threshold"}}
     {"refId"             "A",
      "queryType"         "",
      "relativeTimeRange" {"from" 0, "to" 0},
      "datasourceUid"     "__expr__",
      "model"
      {"conditions"
       [{"evaluator" {"params" [0 0], "type" "gt"},
         "operator"  {"type" "and"},
         "query"     {"params" []},
         "reducer"   {"params" [], "type" "avg"},
         "type"      "query"}],
       "datasource"    {"name" "Expression", "type" "__expr__", "uid" "__expr__"},
       "expression"    "1",
       "hide"          false,
       "intervalMs"    1000,
       "maxDataPoints" 43200,
       "refId"         "A",
       "type"          "math"}}],
    "orgID"                 1}))

(defn get-all-alert-rules-response [alerts]
  {:headers {},
   :status  200,
   :body
   (->> alerts
        (map-indexed make-alert-rule)
        (vec)
        (helper/clj->json))})

(defn get-library-panels-response [panels]
  {:headers {},
   :status  200,
   :body    (-> {"result"
                 {"totalCount" (count panels),
                  "elements"
                  (vec (map-indexed make-library-element panels))
                  "page"       1,
                  "perPage"    100}}
                (helper/clj->json))})

(def create-alert-rule-response
  {:headers {},
   :status  200,
   :body    (->> {:title   "My simple alert rule"
                  :version 1}
                 (make-alert-rule 1)
                 (helper/clj->json))})

(def update-alert-rule-response
  {:headers {},
   :status  200,
   :body    (->> {:title   "My simple alert rule"
                  :version 2}
                 (make-alert-rule 1)
                 (helper/clj->json))})

(def create-library-element-response
  {:headers {},
   :status  200,
   :body (-> {"result" (make-library-element 3 {:name "My simple panel"})}
             (helper/clj->json))})

(def update-library-element-response
  {:headers {},
   :status 200,
   :body (-> {"result" (make-library-element 3 {:name "My updated simple panel"})}
             (helper/clj->json))})

(defn make-create-update-dashboard
  ([] (make-create-update-dashboard (random-id)))
  ([id] (make-create-update-dashboard id {}))
  ([id {:keys [uid
               title
               folder-uid
               version]
        :or   {uid        (random-dashboard-uid)
               title      (random-dashboard-title)
               folder-uid (random-folder-uid)
               version    (random-version)}}]
   {:pre [(non-neg-int? id)
          (string? uid)
          (string? title)
          (string? folder-uid)
          (non-neg-int? version)]}
   {"folderUid" folder-uid,
    "id"        id,
    "slug"      (slug title),
    "status"    "success",
    "uid"       uid,
    "url"       (str "/d/" uid "/" (slug title)),
    "version"   version}))

(defn create-update-dashboard-response [dashboard]
  {:headers {},
   :status 200,
   :body (->> dashboard
              (make-create-update-dashboard 1)
              (helper/clj->json))})

(defn make-dashboard-related-alerts [alerts]
  (->> alerts
       (map-indexed make-alert-rule)
       (vec)))

(defn make-dashboard-related-panels [dashboards]
  (->> dashboards
       (map-indexed make-library-element)
       (vec)))
