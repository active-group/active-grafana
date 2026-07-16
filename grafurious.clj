#!/usr/bin/env bb
(require '[active-grafana.cli :as cli])
(apply cli/-main *command-line-args*)
