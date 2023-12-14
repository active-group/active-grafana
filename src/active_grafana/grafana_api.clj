(ns active-grafana.grafana-api
  (:require [clj-http.client       :as client]
            [active-grafana.helper :as helper]))

;; GET /api/search/?type=dash-db
;; https://grafana.com/docs/grafana/latest/developers/http_api/folder_dashboard_search/#search-folders-and-dashboards
;; Note the following query-parameter:
;; limit – Limit the number of returned results (max is 5000; default is 1000)
(defn get-dashboards
  [base-url token]
  (let [api-url (str base-url "/api/search?type=dash-db")]
    (helper/log (str "Api-url: " api-url))
    (client/get api-url {:oauth-token token})))

;; GET /api/search/?type=dash-folder
;; https://grafana.com/docs/grafana/latest/developers/http_api/folder_dashboard_search/#search-folders-and-dashboards
;; Note the following query-parameter:
;; limit – Limit the number of returned results (max is 5000; default is 1000)
#_(defn get-folders
  [base-url token]
  (let [api-url (str base-url "/api/search?type=dash-folder")]
    (helper/log (str "Api-url: " api-url))
    (client/get api-url {:oauth-token token})))

;; GET /api/folders
;; https://grafana.com/docs/grafana/latest/developers/http_api/folder/#get-all-folders
;; Default limit: 1000 results
(defn get-folders
  [base-url token]
  (let [api-url (str base-url "/api/folders")]
    (helper/log (str "Api-url: " api-url))
    (client/get api-url {:oauth-token token})))

;; GET /api/dashboards/uid/:uid
;; https://grafana.com/docs/grafana/latest/developers/http_api/dashboard/#get-dashboard-by-uid
(defn get-dashboard-by-uid
  [base-url token uid]
  (let [api-url (str base-url "/api/dashboards/uid/" uid)]
    (helper/log (str "Api-url: " api-url))
    (client/get api-url {:oauth-token token})))

;; GET /api/v1/provisioning/alert-rules
;; https://grafana.com/docs/grafana/latest/developers/http_api/alerting_provisioning/#route-get-alert-rules
(defn get-all-alert-rules
  [base-url token]
  (let [api-url (str base-url "/api/v1/provisioning/alert-rules")]
    (helper/log (str "Api-url: " api-url))
    (client/get api-url {:oauth-token token})))

;; POST /api/dashboards/db
;; https://grafana.com/docs/grafana/latest/developers/http_api/dashboard/#create--update-dashboard
(defn create-update-dashboard
  [base-url token dashboard]
  (let [api-url (str base-url "/api/dashboards/db")]
    (helper/log (str "Api-url: " api-url))
    (client/post api-url {:oauth-token  token
                          :body         dashboard
                          :accept       :json
                          :content-type :json})))

;; POST /api/v1/provisioning/alert-rules
;; https://grafana.com/docs/grafana/latest/developers/http_api/alerting_provisioning/#route-post-alert-rule
(defn create-alert-rule
  [base-url token alert-rule]
  (let [api-url (str base-url "/api/v1/provisioning/alert-rules")]
    (helper/log (str "Api-url: " api-url))
    (client/post api-url {:oauth-token  token
                          ;; without this header the rule is not editable in the gui
                          :headers      {"X-Disable-Provenance" "true"}
                          :body         alert-rule
                          :accept       :json
                          :content-type :json})))

;; PUT /api/v1/provisioning/alert-rules/{UID}
;; https://grafana.com/docs/grafana/latest/developers/http_api/alerting_provisioning/#span-idroute-put-alert-rulespan-update-an-existing-alert-rule-_routeputalertrule_
(defn update-alert-rule
  [base-url token alert-rule-uid alert-rule-update]
  (let [api-url (str base-url "/api/v1/provisioning/alert-rules/" alert-rule-uid)]
    (helper/log (str "Api-url: " api-url))
    (client/put api-url {:oauth-token  token
                         ;; without this header the rule is not editable in the gui
                         :headers      {"X-Disable-Provenance" "true"}
                         :body         alert-rule-update
                         :accept       :json
                         :content-type :json})))

;; GET /api/folders/:uid
;; https://grafana.com/docs/grafana/latest/developers/http_api/folder/#get-folder-by-uid
(defn get-folder-by-folder-uid
  [base-url token folder-uid]
  (let [api-url (str base-url "/api/folders/" folder-uid)]
    (helper/log (str "Api-url: " api-url))
    (client/get api-url {:oauth-token token})))
