# active-grafana

A Clojure script/project designed to help dealing with Grafana.

## Features

- Copying a grafana dashboard from one instance to another instance.
- Copying alert-rules associated with this grafana dashboard.

- Adjusting library panels where a specific target structure needs repition
  with different datasources.

## Usage

### Babashka

This project is mainly designed to run as a
[babashka](https://book.babashka.org/) script.

Check out this repository.

This project contains two separated main-functions:

```
active-grafana.main-copy
active-grafana.main-adjust
```

To start, type at the root of the project:

```
active-grafana $ bb -m active-grafana.main-copy -- -h
```
or
```
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
                 -c -r --board-uid=b3b41ced-1237-45a1-9f63-08d8b4191c57 \
                 --board-folder-uid=afb10bf4-f0b1-4e2b-af04-1061844be119 \
                 --rules-folder-uid=afb10bf4-f0b1-4e2b-af04-1061844be119
```

All variables can be found in `active-grafana.settings`.

### Leiningen

If you check out this repository you can use lein.

This project contains two separated main-functions:

```
active-grafana.main-copy
active-grafana.main-adjust
```

To starty, type at the root of the repository:

```
active-grafana $ lein run -m active-grafana.main-copy -- -h
```
or
```
active-grafana $ lein run -m active-grafana.main-adjust -- -h
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
                 -c -r --board-uid=b3b41ced-1237-45a1-9f63-08d8b4191c57 \
                 --board-folder-uid=afb10bf4-f0b1-4e2b-af04-1061844be119 \
                 --rules-folder-uid=afb10bf4-f0b1-4e2b-af04-1061844be119
```

All variables can be found in `active-grafana.settings`.

## Known Issues

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
