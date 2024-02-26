# active-grafana

A Clojure script/project designed to help dealing with Grafana.

## Features

- Copying a grafana dashboard from one instance to another instance.
- Copying alert-rules associated with this grafana dashboard.
- Copying library-panels associated with this grafana dashboard.

- Adjusting library panels where a specific target structure needs repition
  with different datasources.

## Usage

### Babashka

If you check out this repository you can use [babashka](https://book.babashka.org/).

This project contains two main-functions:

```
active-grafana.main-copy
active-grafana.main-adjust
```

With babaskha-tasks (see `bb.edn` or https://book.babashka.org/#tasks) you can start at the root of the project with:

```
active-grafana $ bb copy -h
```
or

```
active-grafana $ bb adjust -h
```

If this does not work try:

```
active-grafana $ bb -m active-grafana.main-copy -- -h
active-grafana $ bb -m active-grafana.main-adjust -- -h
```

The help shows variables like `URL`, `FROM_URL` or `MESSAGE`, which can
alternatively be provided by environment variables. For example:

```
$ export FROM_URL=http://localhost:3000
$ export FROM_TOKEN=glsa_FcOPTbFuJ9ZO0q6AzdTSoKjVLHaxsBw5_4ece1975
active-grafana $ TO_URL=http://localhost:3001 \
                 TO_TOKEN=glsa_sr9e9M1JI0ARODVP347uVKm7L1wqKvGa_3543afe4 \
                 MESSAGE="Changes to the speed-check-panel." \
                 bb -m active-grafana.main-copy -- \
                 -b -a --board-uid=b3b41ced-1237-45a1-9f63-08d8b4191c57 \
                 --board-folder-uid=afb10bf4-f0b1-4e2b-af04-1061844be119 \
                 --alerts-folder-uid=afb10bf4-f0b1-4e2b-af04-1061844be119
```

All variables can be found in `active-grafana.settings`.

### Leiningen

If you check out this repository you can use lein.

This project contains two main-functions:

```
active-grafana.main-copy
active-grafana.main-adjust
```

To start, type at the root of the repository:

For main-copy:
```
active-grafana $ lein run -m active-grafana.main-copy -- -h
```
or
```
active-grafana $ lein with-profile as-copy run -- -h
```

For main-adjust:
```
active-grafana $ lein run -m active-grafana.main-adjust -- -h
```
or

```
active-grafana $ lein with-profile as-adjust run -- -h
```

The help shows variables like `URL`, `FROM_URL` or `MESSAGE`, which can
alternatively be provided by environment variables. For example:

```
$ export FROM_URL=http://localhost:3000
$ export FROM_TOKEN=glsa_FcOPTbFuJ9ZO0q6AzdTSoKjVLHaxsBw5_4ece1975
active-grafana $ TO_URL=http://localhost:3001 \
                 TO_TOKEN=glsa_sr9e9M1JI0ARODVP347uVKm7L1wqKvGa_3543afe4 \
                 MESSAGE="Changes to the speed-check-panel." \
                 lein run -m active-grafana.main-copy -- \
                 -b -a --board-uid=b3b41ced-1237-45a1-9f63-08d8b4191c57 \
                 --board-folder-uid=afb10bf4-f0b1-4e2b-af04-1061844be119 \
                 --alerts-folder-uid=afb10bf4-f0b1-4e2b-af04-1061844be119
```

All variables can be found in `active-grafana.settings`.

### Babashka-Pod

This project can also be used as a [babashka-pod](https://github.com/babashka/pods).
The pod-interface can be found in `active-grafana.pod`.

A usage example with [docker](https://www.docker.com/).
Check out this repository as `active-grafana`.`

```
active-grafana $ docker build -t active-grafana -f Dockerfile-pod .
active-grafana $ docker run active-grafana
active-grafana $ docker run active-grafana copy --help
active-grafana $ docker run active-grafana adjust --help
```

### Standalone Applications

With the help of [GraalVM](https://www.graalvm.org/) we can create a [native
image](https://www.graalvm.org/latest/reference-manual/native-image/).

A usage example with [docker](https://www.docker.com/).
Check out this repository as `active-grafana`.`

#### For copy:

```
active-grafana $ docker build -t active-grafana-copy -f Dockerfile-standalone-copy .
active-grafana $ docker run -it active-grafana-copy sh
# ./active-grafana-copy --help
```

Note: The Dockerfile contains this line:

```
RUN wget https://github.com/active-group/active-grafana/releases/download/v0.1/active-grafana-copy.jar
```

Check https://github.com/active-group/active-grafana/releases for other releases.

#### For adjust:

```
active-grafana $ docker build -t active-grafana-adjust -f Dockerfile-standalone-adjust .
active-grafana $ docker run -it active-grafana-adjust sh
# ./active-grafana-adjust --help
```

Note: The Dockerfile contains this line:

```
RUN wget https://github.com/active-group/active-grafana/releases/download/v0.1/active-grafana-adjust.jar
```

Check https://github.com/active-group/active-grafana/releases for other releases.


## Known Issues and 'good to know'

- The application uses the environment variable `BABASHKA_POD` (as described
  [here](https://github.com/babashka/pods?tab=readme-ov-file#environment)) to
  determine, whether the application should behave as a pod.

- Error handling:
  If something goes wrong, you are immediately provided with the plain
  grafana-api-error-messages. Currently, there is no fallback or further help
  provided.

- Testing:
  There is no testing within the project.

- Logging and Debugging:
  The current logging and debugging system is rudimentary and might even be
  confusing.

## License

Copyright © 2023-2024 Active Group GmbH

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
