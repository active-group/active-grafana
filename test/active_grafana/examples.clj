(ns active-grafana.examples
  (:require [active-grafana.settings :as settings]))

(def grafana-a-instance (settings/->Grafana-Instance "http://irrelevant.url-a" "irrelevant-token-a"))
(def source-url (:url grafana-a-instance))
(def source-token (:token grafana-a-instance))

(def grafana-b-instance (settings/->Grafana-Instance "http://irrelevant.url-b" "irrelevant-token-b"))
(def target-url (:url grafana-b-instance))
(def target-token (:token grafana-b-instance))

(def dashboard-title "Simple dashboard title")
(def another-dashboard-title "Another dashboard title")
(def dashboard-uid "adv5c5m")
(def another-dashboard-uid "XYM3U2L")
(def dashboard {:uid   dashboard-uid
                :title dashboard-title})

(def folder-title "My folder title")
(def another-folder-title "Another folder title")
(def folder-uid "dflyuecbw3cw0f")
(def another-folder-uid "abcduecbw00000")
(def folder {:uid   folder-uid
             :title folder-title})

(def panel-uid "my-panel")
(def panel-name "My panel name")
(def another-panel-uid "another-panel")
(def another-panel-name "Another panel name")
(def panel {:uid  panel-uid
            :name panel-name})
(def another-panel {:uid  another-panel-uid
                    :name another-panel-name})
(def panels [panel another-panel])

(def alert-uid "my-alert")
(def alert-title "My alert name")
(def another-alert-uid "another-alert")
(def another-alert-title "Another alert name")
(def alert {:uid   alert-uid
            :title alert-title})
(def another-alert {:uid   another-alert-uid
                    :title another-alert-title})
(def alerts [alert another-alert])

(def change-dashboard-message "I changed the dashboard :)")

(defn- random-string
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
