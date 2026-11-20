(ns active-grafana.grafana-api
  (:require [active-grafana.helper :as helper]
            [active-grafana.settings :as settings]
            [babashka.http-client  :as http]
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

(defn jsonize-body [body]
  (if (helper/json-string? body)
    body
    (helper/clj->json body)))

(defn make-request-map [& {:keys [token body]}]
  {:pre  [(string? token)]
   :post [#(contains? % :client)
          #(contains? % :headers)
          #(if body
             (helper/json-string? (:body %))
             true)]}
  (cond-> {:client insecure-client}
    (not body) (assoc :headers (make-query-client-headers :token token))
    body       (assoc :headers (make-mutation-client-headers :token token
                                                             :disable-provenance? true)
                      :body (jsonize-body body))))

(defn sanitize-base-url [endpoint-url]
  {:pre  [(string? endpoint-url)]
   :post [(string? endpoint-url)]}
  (-> endpoint-url
      (str/replace #"/+$" "")
      (str/replace #"[/]{3,}" "//")))

(defn sanitize-endpoint-url [endpoint-url]
  {:pre  [(string? endpoint-url)]
   :post [(string? endpoint-url)]}
  (-> endpoint-url
      (str/replace #"/+$" "")
      (str/replace #"^/+" "")
      (str/replace #"[/]{2,}" "/")))

(defn make-request-url [base-url endpoint-url & more-fragments]
  {:pre  [(string? base-url)
          (or (str/starts-with? base-url "http://")
              (str/starts-with? base-url "https://"))
          (string? endpoint-url)
          (not (str/blank? endpoint-url))
          (not (re-find #"[/]{2,}" endpoint-url))
          (every? (comp not coll?) more-fragments)]
   :post [#(string? %)]}
  (let [sane-endpoint-url (sanitize-endpoint-url endpoint-url)
        sane-base-url     (sanitize-base-url base-url)

        [fragments [_ & query-params]] (split-with #(not (= "?" %)) more-fragments)]
    (->> (str (->> fragments
                   (map #(URLEncoder/encode (str %) "UTF-8"))
                   (concat [(str sane-base-url "/" sane-endpoint-url)])
                   (str/join "/"))
              (when (not-empty query-params)
                (->> query-params
                     (map #(URLEncoder/encode (str %) "UTF-8"))
                     (partition 2)
                     (map (partial interpose "="))
                     (interpose "&")
                     (flatten)
                     (str/join)
                     (str "?"))))
         ;; lets validate using java.net.URL
         (URL/new)
         (.toURI)
         (.toString))))

(defn method->crud-verb [method]
  (case method
    :get    "read"
    :post   "create"
    :put    "update using put"
    :patch  "update using patch"
    :delete "delete"
    :head   "head"))

(defn endpoint->resource-name [endpoint]
  (case (re-find #"alert|dash-folder|dash|folder|library-elements" endpoint)
    "alert"            "Alert Rule(s)"
    "dash"             "Dashboard(s)"
    "dash-folder"      "Folder(s)"
    "folder"           "Folder(s)"
    "library-elements" "Library Panel(s)"))

(defn request-name [grafana-instance endpoint method]
  (let [crud-verb (method->crud-verb method)
        instance-name (settings/grafana-instance-name grafana-instance)
        resource-name (endpoint->resource-name endpoint)]
    (str (str/capitalize crud-verb)
         " "
         resource-name
         " on "
         instance-name
         ".")))

(defn log-request [log-fn name {:keys [uri method body query-params]}]
  (log-fn name)
  (log-fn (str (str/upper-case (name method)) " `" uri "`: "))
  (log-fn (if body
            (str "\n  -Using body: " (pr-str body) ".")
            "\n  -Using no body."))
  (log-fn (if query-params
            (str "\n  -Using query parameters: " (pr-str query-params) ".")
            "\n  -Using no query parameters.")))

;; TODO: after the refactoring using make-request,
;; also refactor request-map above to be named request-opts
;; and return opts, which are a subset of the opts expected by
;; http/request...
(defn make-request [endpoint method & {:keys [static-query-params]}]
  (fn request [grafana-instance & {:keys [body query-params]}]
    (let [request-opts (merge (make-request-map :token (settings/grafana-instance-token grafana-instance)
                                                :body body)
                              {:uri          (make-request-url (settings/grafana-instance-url grafana-instance) endpoint)
                               :method       method
                               :query-params (merge static-query-params
                                                    query-params)})]
      (log-request helper/log
                   (request-name grafana-instance endpoint method)
                   request-opts)
      (http/request request-opts))))

;; >>> ALERTS

;; GET /api/v1/provisioning/alert-rules
;; https://grafana.com/docs/grafana/latest/developers/http_api/alerting_provisioning/#route-get-alert-rules
(defn get-all-alert-rules
  [base-url token]
  (let [endpoint-url "/api/v1/provisioning/alert-rules"
        request-url  (make-request-url base-url endpoint-url)
        request-map  (make-request-map :token token)]
    (helper/log (str "Going to request: " request-url))
    (http/get request-url request-map)))

(defn get-alert-rule-by-uid
  [base-url token alert-uid]
  {:pre [(string? alert-uid)]}
  (let [endpoint-url "/api/v1/provisioning/alert-rules/"
        request-url (make-request-url base-url endpoint-url alert-uid)
        request-map (make-request-map :token token)]
    (helper/log (str "Going to request: " request-url))
    (http/get request-url request-map)))

;; POST /api/v1/provisioning/alert-rules
;; https://grafana.com/docs/grafana/latest/developers/http_api/alerting_provisioning/#route-post-alert-rule
(defn create-alert-rule
  [base-url token alert-rule]
  (let [endpoint-url "/api/v1/provisioning/alert-rules"
        request-url (make-request-url base-url endpoint-url)
        request-map (make-request-map :token token :body alert-rule)]
    (helper/log (str "Going to request: " request-url))
    (http/post request-url request-map)))

;; PUT /api/v1/provisioning/alert-rules/{UID}
;; https://grafana.com/docs/grafana/latest/developers/http_api/alerting_provisioning/#span-idroute-put-alert-rulespan-update-an-existing-alert-rule-_routeputalertrule_
(defn update-alert-rule
  [base-url token alert-uid alert-rule-update]
  {:pre [(string? alert-uid)]}
  (let [endpoint-url "/api/v1/provisioning/alert-rules"
        request-url (make-request-url base-url endpoint-url alert-uid)
        request-map (make-request-map :token token :body alert-rule-update)]
    (helper/log (str "Going to request: " request-url))
    (http/put request-url request-map)))

;; <<< ALERTS

;; >>> DASHBOARDS

;; GET /api/search/?type=dash-db
;; https://grafana.com/docs/grafana/latest/developers/http_api/folder_dashboard_search/#search-folders-and-dashboards
;; Note the following query-parameter:
;; limit – Limit the number of returned results (max is 5000; default is 1000)
(defn get-dashboards
  [base-url token]
  (let [endpoint-url "/api/search"
        request-url (make-request-url base-url endpoint-url
                                      "?"
                                      "type" "dash-db")
        request-map (make-request-map :token token)]
    (helper/log (str "Going to request: " request-url))
    (http/get request-url request-map)))

;; GET /api/dashboards/uid/:uid
;; https://grafana.com/docs/grafana/latest/developers/http_api/dashboard/#get-dashboard-by-uid
(defn get-dashboard-by-uid
  [base-url token uid]
  {:pre [(string? uid)]}
  (let [endpoint-url "/api/dashboards/uid/"
        request-url (make-request-url base-url endpoint-url uid)
        request-map (make-request-map :token token)]
    (helper/log (str "Going to request: " request-url))
    (http/get request-url request-map)))

;; POST /api/dashboards/db
;; https://grafana.com/docs/grafana/latest/developers/http_api/dashboard/#create--update-dashboard
(defn create-update-dashboard
  [base-url token dashboard]
  (let [endpoint-url "/api/dashboards/db"
        request-url (make-request-url base-url endpoint-url)
        request-map (make-request-map :token token :body dashboard)]
    (helper/log (str "Going to request: " request-url))
    (http/post request-url request-map)))

(defn find-dashboards-by-query
  "Find dashboards using a search [[query]] from grafanas legacy http api.
   The query matches against the dashboard titles."
  [base-url token query]
  {:pre [(string? query)]}
  (let [endpoint-url "/api/search"
        request-url (make-request-url base-url endpoint-url
                                      "?"
                                      "query" query
                                      "type" "dash-db")
        request-map (make-request-map :token token)]
    (helper/log (str "Going to request: " request-url))
    (http/get request-url request-map)))

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
  (let [endpoint-url "/api/folders"
        request-url (make-request-url base-url endpoint-url)
        request-map (make-request-map :token token)]
    (helper/log (str "Going to request: " request-url))
    (http/get request-url request-map)))

;; GET /api/folders/:uid
;; https://grafana.com/docs/grafana/latest/developers/http_api/folder/#get-folder-by-uid
(defn get-folder-by-folder-uid
  [base-url token folder-uid]
  {:pre [(string? folder-uid)]}
  (let [endpoint-url "/api/folders/"
        request-url (make-request-url base-url endpoint-url folder-uid)
        request-map (make-request-map :token token)]
    (helper/log (str "Going to request: " request-url))
    (http/get request-url request-map)))

(defn find-folders-by-query
  "Find (dashboard-)folders using a search [[query]] from grafanas legacy http api.
   The query matches against the (dashboard-)folder titles."
  [base-url token query]
  {:pre [(string? query)]}
  (let [endpoint-url "/api/search"
        request-url (make-request-url base-url endpoint-url
                                      "?"
                                      "query" query
                                      "type" "dash-folder")
        request-map (make-request-map :token token)]
    (helper/log (str "Going to request: " request-url))
    (http/get request-url request-map)))

;; POST /api/folders
;; https://grafana.com/docs/grafana/latest/developers/http_api/folder/#create-folder
(defn create-folder
  "Create a folder named [[title]] using a POST against \"[[base-url]]/api/folders\". "
  [base-url token title]
  {:pre [(string? title)]}
  (let [endpoint-url "/api/folders"
        request-url (make-request-url base-url endpoint-url)
        request-map (make-request-map :token token :body {:title title})]
    (helper/log (str "Going to request: " request-url))
    (http/post request-url request-map)))

;; <<< FOLDERS

;; >>> LIBRARY-ELEMENTS

;; GET /api/library-elements
;; https://grafana.com/docs/grafana/latest/developers/http_api/library_element/#get-all-library-elements

;; Note the following query-parameter:
;; kind - Kind of element to search for. Use 1 for library panels or 2 for library variables.
;; perPage - The number of results per page; default is 100.
(defn get-library-panels
  [base-url token]
  (let [endpoint-url "/api/library-elements"
        request-url (make-request-url base-url endpoint-url "?" "kind" "1")
        request-map (make-request-map :token token)]
    (helper/log (str "Going to request: " request-url))
    (http/get request-url request-map)))

;; GET /api/library-elements/:uid
;;  https://grafana.com/docs/grafana/latest/developers/http_api/library_element/#get-library-element-by-uid
(defn get-library-element-by-uid
  [base-url token uid]
  {:pre [(string? uid)]}
  (let [endpoint-url "/api/library-elements/"
        request-url (make-request-url base-url endpoint-url uid)
        request-map (make-request-map :token token)]
    (helper/log (str "Going to request: " request-url))
    (http/get request-url request-map)))

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
  (let [endpoint-url "/api/library-elements"
        request-url (make-request-url base-url endpoint-url)
        request-map (make-request-map :token token :body panel)]
    (helper/log (str "Going to request: " request-url))
    (http/post request-url request-map)))

;; PATCH /api/library-elements/:uid
;; https://grafana.com/docs/grafana/latest/developers/http_api/library_element/#update-library-element
(defn update-library-element
  [base-url token uid patch]
  {:pre [(string? uid)]}
  (let [endpoint-url "/api/library-elements"
        request-url  (make-request-url base-url endpoint-url uid)
        request-map  (make-request-map :token token :body patch)]
    (helper/log (str "Going to request: " request-url))
    (http/patch request-url request-map)))

;; <<< LIBRARY-ELEMENTS
