# active-grafana

A Clojure library designed to help dealing with Grafana.

## Features

- Copying a grafana dashboard from one instance to another instance.
- Copying alert-rules associated with this grafana dashboard.

## Usage

### Installing active-grafana

FIXME: Way to go, not available yet.

```
$ active-grafana -h
```

The help shows variables like `FROM_URL` or `MESSAGE`, which can
alternatively be provided by environment variables. For example:

```
$ FROM_URL=http://localhost:3000 \
  FROM_TOKEN=glsa_FcOPTbFuJ9ZO0q6AzdTSoKjVLHaxsBw5_4ece1975 \
  TO_URL=http://localhost:3001 \
  TO_TOKEN=glsa_sr9e9M1JI0ARODVP347uVKm7L1wqKvGa_3543afe4 \
  MESSAGE="Changes to the speed-check-panel." \
  active-grafana -c -r --board-uid=b3b41ced-1237-45a1-9f63-08d8b4191c57 \
  --board-folder-uid=afb10bf4-f0b1-4e2b-af04-1061844be119 \
  --rules-folder-uid=afb10bf4-f0b1-4e2b-af04-1061844be119
```

### Using Leiningen

If you check out this repository you can use lein. For example, to see the help
type at the root of the repository:

```
$ lein run -- -h
```

The help shows variables like `FROM_URL` or `MESSAGE`, which can
alternatively be provided by environment variables. For example:

```
$ FROM_URL=http://localhost:3000 \
  FROM_TOKEN=glsa_FcOPTbFuJ9ZO0q6AzdTSoKjVLHaxsBw5_4ece1975 \
  TO_URL=http://localhost:3001 \
  TO_TOKEN=glsa_sr9e9M1JI0ARODVP347uVKm7L1wqKvGa_3543afe4 \
  MESSAGE="Changes to the speed-check-panel." \
  lein run -- -c -r --board-uid=b3b41ced-1237-45a1-9f63-08d8b4191c57 \
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
  confusting.

## License

Copyright © 2023 Active Group GmbH

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
