(ns active-grafana.grafana-api
  (:require [active-grafana.helper :as helper]
            [babashka.http-client  :as http]
            [clj-http.lite.client  :as client]
            [clojure.string :as str])
  (:import [java.net URLEncoder URL]))

(set! *warn-on-reflection* true)

(def root-folder-name "General")
(defn root-folder? [folder-name]
  (= root-folder-name folder-name))

(def auxiliary-root-folder-title "Dashboards (no folder / root folder)")
(def auxiliary-root-folder-uid "")
(defn auxiliary-root-folder-uid? [uid]
  (= uid auxiliary-root-folder-uid))

(def insecure-client (http/client (assoc-in http/default-client-opts [:ssl-context :insecure] true)))

(def bearer-token-prefix "Bearer ")
(def json-accept-header "application/json") ;; Format of the response
(def json-content-type-header "application/json") ;; Format of the body

;; NOTE: The X-Disable-Provenance header is a custom HTTP header in Grafana
;; used to allow resources created or updated through the Grafana
;; provisioning API to remain editable directly within the Grafana user
;; interface.
(def grafana-disable-provenance-header "X-Disable-Provenance")

(defn bearer-token? [token]
  {:post [#(boolean? %)]}
  (boolean (and (string? token)
                (str/starts-with? token bearer-token-prefix)
                (-> token
                    (str/replace bearer-token-prefix "")
                    (str/blank?)
                    (not)))))

(defn make-bearer-token [token]
  {:pre  [(string? token)
          (not (str/blank? token))]
   :post [bearer-token?]}
  (str bearer-token-prefix token))

;; TODO:
;; dsiable-provenance and also
;; content-type headers only make sense on
;; mutating requests like post, put or patch
(defn make-query-client-headers
  "Contruct client headers for GET HTTP requests as a clojure map."
  [& {:keys [token]}]
  {:post [#(contains? % "Authorization")
          #(bearer-token? (get % "Authorization"))
          #(contains? % "Accept")
          #(= json-accept-header (get % "Accept"))
          #(contains? % "Content-Type")
          #(= json-content-type-header (get % "Content-Type"))]}
  {"Authorization" (make-bearer-token token)
   "Accept"        json-accept-header})

(defn make-mutation-client-headers
  "Contruct client headers for POST/PUT/PATCH HTTP requests as a clojure map."
  [& {:keys [token disable-provenance?]
      :or   {disable-provenance? false}}]
  {:pre  [(boolean? disable-provenance?)]
   :post [#(contains? % "Authorization")
          #(bearer-token? (get % "Authorization"))
          #(contains? % "Accept")
          #(= json-accept-header (get % "Accept"))
          #(contains? % "Content-Type")
          #(= json-content-type-header (get % "Content-Type"))
          #(if disable-provenance?
             (= "true" (get % grafana-disable-provenance-header))
             true)]}
  (cond-> {"Authorization" (make-bearer-token token)
           "Accept"        json-accept-header
           "Content-Type"  json-content-type-header}
    disable-provenance? (assoc grafana-disable-provenance-header "true")))

(defn make-request-map [& {:keys [token body]}]
  {:pre  [(helper/json-string? body)]
   :post [#(contains? % :client)
          #(contains? % :headers)
          #(if (contains? % :body)
             (helper/json-string? (:body %))
             true)]}
  (cond-> {:client insecure-client}
    (not body) (assoc :headers (make-query-client-headers :token token))
    body       (assoc :headers (make-mutation-client-headers :token token :disable-provenance? true))
    body       (assoc :body body)))

(defn make-request-url [base-url endpoint-url & more-fragments]
  {:pre  [(string? base-url)
          (or (str/starts-with? base-url "http://")
              (str/starts-with? base-url "https://"))
          (string? endpoint-url)
          (str/starts-with? endpoint-url "/")
          (not (str/ends-with? endpoint-url "/"))
          (not (re-find #"[/]{2,}" endpoint-url))
          (every? (comp not coll?) more-fragments)]
   :post [#(string? %)]}
  (->> more-fragments
       (map #(URLEncoder/encode (str %) "UTF-8"))
       (concat [(str base-url endpoint-url)])
       (str/join "/")
       (URL/new)
       ;; lets validate using java.net.URL
       (.toURI)
       (.toString)))

;; >>> ALERTS

;; GET /api/v1/provisioning/alert-rules
;; https://grafana.com/docs/grafana/latest/developers/http_api/alerting_provisioning/#route-get-alert-rules
(defn get-all-alert-rules
  [base-url token]
  (let [endpoint-url "api/v1/provisioning/alert-rules"
        request-url (make-request-url base-url endpoint-url)]
    (helper/log (str "Going to request: " request-url))
    (http/get request-url (make-request-map :bearer-token token))))

(defn get-alert-rule-by-uid
  [base-url token alert-uid]
  (let [api-url (str base-url "/api/v1/provisioning/alert-rules/" alert-uid)]
    (helper/log (str "Api-url: " api-url))
    (client/get api-url {:insecure?   true
                         :oauth-token token})))

;; POST /api/v1/provisioning/alert-rules
;; https://grafana.com/docs/grafana/latest/developers/http_api/alerting_provisioning/#route-post-alert-rule
(defn create-alert-rule
  [base-url token alert-rule]
  {:pre [(helper/json-string? alert-rule)]}
  (let [api-url (str base-url "/api/v1/provisioning/alert-rules")]
    (helper/log (str "Api-url: " api-url))
    (client/post api-url {:insecure?    true
                          :oauth-token  token
                          ;; without this header the rule is not editable in the gui
                          ;; see: https://grafana.com/docs/grafana/v12.3/developer-resources/api-reference/http-api/alerting_provisioning/#edit-resources-in-the-grafana-ui
                          :headers      {"X-Disable-Provenance" "true"}
                          :body         alert-rule
                          :accept       :json
                          :content-type :json})))

;; PUT /api/v1/provisioning/alert-rules/{UID}
;; https://grafana.com/docs/grafana/latest/developers/http_api/alerting_provisioning/#span-idroute-put-alert-rulespan-update-an-existing-alert-rule-_routeputalertrule_
(defn update-alert-rule
  [base-url token alert-rule-uid alert-rule-update]
  {:pre [(string? alert-rule-uid)
         (helper/json-string? alert-rule-update)]}
  (let [api-url (str base-url "/api/v1/provisioning/alert-rules/" alert-rule-uid)]
    (helper/log (str "Api-url: " api-url))
    (client/put api-url {:insecure?    true
                         :oauth-token  token
                         ;; without this header the rule is not editable in the gui
                         ;; TODO: only endpoint POST - not PUT - but shouldn't harm
                         ;; see: https://grafana.com/docs/grafana/v12.3/developer-resources/api-reference/http-api/alerting_provisioning/#edit-resources-in-the-grafana-ui
                         :headers      {"X-Disable-Provenance" "true"}
                         :body         alert-rule-update
                         :accept       :json
                         :content-type :json})))

;; <<< ALERTS

;; >>> DASHBOARDS

;; GET /api/search/?type=dash-db
;; https://grafana.com/docs/grafana/latest/developers/http_api/folder_dashboard_search/#search-folders-and-dashboards
;; Note the following query-parameter:
;; limit – Limit the number of returned results (max is 5000; default is 1000)
(defn get-dashboards
  [base-url token]
  (let [api-url (str base-url "/api/search?type=dash-db")]
    (helper/log (str "Api-url: " api-url))
    (client/get api-url {:insecure?   true
                         :oauth-token token})))

;; GET /api/dashboards/uid/:uid
;; https://grafana.com/docs/grafana/latest/developers/http_api/dashboard/#get-dashboard-by-uid
(defn get-dashboard-by-uid
  [base-url token uid]
  {:pre [(string? uid)]}
  (let [api-url (str base-url "/api/dashboards/uid/" (URLEncoder/encode uid "UTF-8"))]
    (helper/log (str "Api-url: " api-url))
    (client/get api-url {:insecure?   true
                         :oauth-token token})))

;; POST /api/dashboards/db
;; https://grafana.com/docs/grafana/latest/developers/http_api/dashboard/#create--update-dashboard
(defn create-update-dashboard
  [base-url token dashboard]
  {:pre [(helper/json-string? dashboard)]}
  (let [api-url (str base-url "/api/dashboards/db")]
    (helper/log (str "Api-url: " api-url))
    (client/post api-url {:insecure?    true
                          :oauth-token  token
                          :body         dashboard
                          :accept       :json
                          :content-type :json})))

(defn find-dashboards-by-query
  "Find dashboards using a search [[query]] from grafanas legacy http api.
   The query matches against the dashboard titles."
  [base-url token query]
  {:pre [(string? query)]}
  (let [api-url (str base-url "/api/search?query=" (URLEncoder/encode query "UTF-8") "&type=dash-db")]
    (helper/log (str "Api-url: " api-url))
    (client/get api-url {:insecure?   true
                         :oauth-token token})))

;; <<< DASHBOARDS

;; >>> FOLDERS

;; GET /api/search/?type=dash-folder
;; https://grafana.com/docs/grafana/latest/developers/http_api/folder_dashboard_search/#search-folders-and-dashboards
;; Note the following query-parameter:
;; limit – Limit the number of returned results (max is 5000; default is 1000)
#_(defn get-folders
    [base-url token]
    (let [api-url (str base-url "/api/search?type=dash-folder")]
      (helper/log (str "Api-url: " api-url))
      (client/get api-url {:insecure?    true
                           :oauth-token token})))

;; GET /api/folders
;; https://grafana.com/docs/grafana/latest/developers/http_api/folder/#get-all-folders
;; Default limit: 1000 results
(defn get-folders
  [base-url token]
  (let [api-url (str base-url "/api/folders")]
    (helper/log (str "Api-url: " api-url))
    (client/get api-url {:insecure?   true
                         :oauth-token token})))

;; GET /api/folders/:uid
;; https://grafana.com/docs/grafana/latest/developers/http_api/folder/#get-folder-by-uid
(defn get-folder-by-folder-uid
  [base-url token folder-uid]
  {:pre [(string? folder-uid)]}
  (let [api-url (str base-url "/api/folders/" folder-uid)]
    (helper/log "get-folder-by-folder-uid: " (str "Api-url: " api-url))
    (client/get api-url {:insecure?   true
                         :oauth-token token})))

(defn find-folders-by-query
  "Find (dashboard-)folders using a search [[query]] from grafanas legacy http api.
   The query matches against the (dashboard-)folder titles."
  [base-url token query]
  {:pre [(string? query)]}
  (let [api-url (str base-url "/api/search?query=" (URLEncoder/encode query "UTF-8") "&type=dash-folder")]
    (helper/log (str "Api-url: " api-url))
    (client/get api-url {:insecure?   true
                         :oauth-token token})))
;; POST /api/folders
;; https://grafana.com/docs/grafana/latest/developers/http_api/folder/#create-folder
(defn create-folder
  "Create a folder named [[title]] using a POST against \"[[base-url]]/api/folders\". "
  [base-url token title]
  {:pre [(string? title)]}
  (let [api-url (str base-url "/api/folders")]
    (helper/log (str "Api-url: " api-url))
    (client/post api-url {:insecure?    true
                          :oauth-token  token
                          :body         (helper/clj->json {:title title})
                          :accept       :json
                          :content-type :json})))

;; <<< FOLDERS

;; >>> LIBRARY-ELEMENTS

;; GET /api/library-elements
;; https://grafana.com/docs/grafana/latest/developers/http_api/library_element/#get-all-library-elements

;; Note the following query-parameter:
;; kind - Kind of element to search for. Use 1 for library panels or 2 for library variables.
;; perPage - The number of results per page; default is 100.
(defn get-library-panels
  [base-url token]
  (let [api-url (str base-url "/api/library-elements?kind=1")]
    (helper/log (str "Api-url: " api-url))
    (client/get api-url {:insecure?   true
                         :oauth-token token})))

;; GET /api/library-elements/:uid
;;  https://grafana.com/docs/grafana/latest/developers/http_api/library_element/#get-library-element-by-uid
(defn get-library-element-by-uid
  [base-url token uid]
  {:pre [(string? uid)]}
  (let [api-url (str base-url "/api/library-elements/" uid)]
    (helper/log (str "Api-url: " api-url))
    (client/get api-url {:insecure?   true
                         :oauth-token token})))

;; GET /api/library-elements/:uid/connections
;; https://grafana.com/docs/grafana/latest/developers/http_api/library_element/#get-library-element-connections
#_(defn get-library-element-connections
    [base-url token uid]
    (let [api-url (str base-url "/api/library-elements/" uid "/connections")]
      (helper/log (str "Api-url: " api-url))
      (client/get api-url {:insecure?   true
                           :oauth-token token})))

;; POST /api/library-elements
;; https://grafana.com/docs/grafana/latest/developers/http_api/library_element/#create-library-element
(defn create-library-element
  [base-url token panel]
  {:pre [(helper/json-string? panel)]}
  (let [api-url (str base-url "/api/library-elements")]
    (helper/log (str "Api-url: " api-url))
    (client/post api-url {:insecure?    true
                          :oauth-token  token
                          :body         panel
                          :accept       :json
                          :content-type :json})))

;; PATCH /api/library-elements/:uid
;; https://grafana.com/docs/grafana/latest/developers/http_api/library_element/#update-library-element
(defn update-library-element
  [base-url token uid patch]
  {:pre [(string? uid)
         (helper/json-string? patch)]}
  (let [api-url (str base-url "/api/library-elements/" uid)]
    (helper/log (str "Api-url: " api-url))
    (http/patch api-url {:client  (http/client (assoc-in http/default-client-opts [:ssl-context :insecure] true))
                         :headers {:authorization (str "Bearer " token)
                                   :accept        "application/json"
                                   :content-type  "application/json"}
                         :body    patch})))

;; <<< LIBRARY-ELEMENTS
