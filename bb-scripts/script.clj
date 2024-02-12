#!/usr/bin/env bb

(require '[babashka.pods :as pods])
(pods/load-pod "/opt/active-grafana/active-grafana-pod")
(require '[active-grafana.pod :as ag])
(let [first-arg (first *command-line-args*)
      rest-args (rest *command-line-args*)]
  (case first-arg
    "copy"   (apply ag/copy rest-args)
    "adjust" (apply ag/adjust rest-args)
        (do (println "Usage: script.clj copy")
            (println "       script.clj adjust"))))
