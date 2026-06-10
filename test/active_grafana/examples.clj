(ns active-grafana.examples
  (:require [active-grafana.settings :as settings]))

(def grafana-a-instance (settings/->Grafana-Instance "http://irrelevant.url-a" "irrelevant-token-a"))

(def grafana-b-instance (settings/->Grafana-Instance "http://irrelevant.url-b" "irrelevant-token-b"))

(def dashboard-title "Simple Time Series")

(def get-dashboards-response
  {:headers {},
   :status  200,
   :body    "[{\"id\":1061871031304192,\"uid\":\"keycloak-dashboard-old\",\"orgId\":1,\"title\":\"Keycloak Metrics Dashboard\",\"uri\":\"db/keycloak-metrics-dashboard\",\"url\":\"/d/keycloak-dashboard-old/keycloak-metrics-dashboard\",\"slug\":\"\",\"type\":\"dash-db\",\"tags\":[],\"isStarred\":false,\"description\":\"Dashboard of Keycloak metrics exported with Keycloak Metrics SPI\\r\\n\\r\\nhttps://github.com/aerogear/keycloak-metrics-spi\",\"folderId\":1002119528886272,\"folderUid\":\"dflyuco4w6hvkf\",\"folderTitle\":\"keycloak\",\"folderUrl\":\"/dashboards/f/dflyuco4w6hvkf/keycloak\",\"sortMeta\":0,\"isDeleted\":false},{\"id\":1817690896470016,\"uid\":\"adv5c5m\",\"orgId\":1,\"title\":\"Simple Time Series\",\"uri\":\"db/simple-time-series\",\"url\":\"/d/adv5c5m/simple-time-series\",\"slug\":\"\",\"type\":\"dash-db\",\"tags\":[],\"isStarred\":false,\"folderId\":1002250554748928,\"folderUid\":\"dflyuecbw3cw0f\",\"folderTitle\":\"my-second-folder\",\"folderUrl\":\"/dashboards/f/dflyuecbw3cw0f/my-second-folder\",\"sortMeta\":0,\"isDeleted\":false}]"})

(def find-dashboards-by-query-responses
  {:none        {:headers {},
                 :status  200,
                 :body    "[]"}
   :unambiguous {:headers {},
                 :status  200,
                 :body    "[{\"id\":1817690896470016,\"uid\":\"adv5c5m\",\"orgId\":1,\"title\":\"Simple Time Series\",\"uri\":\"db/simple-time-series\",\"url\":\"/d/adv5c5m/simple-time-series\",\"slug\":\"\",\"type\":\"dash-db\",\"tags\":[],\"isStarred\":false,\"folderId\":1002250554748928,\"folderUid\":\"dflyuecbw3cw0f\",\"folderTitle\":\"my-second-folder\",\"folderUrl\":\"/dashboards/f/dflyuecbw3cw0f/my-second-folder\",\"sortMeta\":0,\"isDeleted\":false}]"}
   :ambiguous   {:headers {},
                 :status  200,
                 :body    "[{\"id\":1817690896470016,\"uid\":\"adv5c5m\",\"orgId\":1,\"title\":\"Simple Time Series\",\"uri\":\"db/simple-time-series\",\"url\":\"/d/adv5c5m/simple-time-series\",\"slug\":\"\",\"type\":\"dash-db\",\"tags\":[],\"isStarred\":false,\"folderId\":1002250554748928,\"folderUid\":\"dflyuecbw3cw0f\",\"folderTitle\":\"my-second-folder\",\"folderUrl\":\"/dashboards/f/dflyuecbw3cw0f/my-second-folder\",\"sortMeta\":0,\"isDeleted\":false},{\"id\":1817690896470017,\"uid\":\"bdv5c5n\",\"orgId\":1,\"title\":\"Simple Time Series\",\"uri\":\"db/simple-time-series\",\"url\":\"/d/adv5c5m/simple-time-series\",\"slug\":\"\",\"type\":\"dash-db\",\"tags\":[],\"isStarred\":false,\"folderId\":1002250554748928,\"folderUid\":\"dflyuecbw3cw0f\",\"folderTitle\":\"another-folder\",\"folderUrl\":\"/dashboards/f/dflyuecbw3cw0f/another-folder\",\"sortMeta\":0,\"isDeleted\":false}]"}})
