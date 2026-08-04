TODO: fine tune the readme and test the examples.

# active-grafana

A [babashka](https://book.babashka.org/) script designed to help dealing with [Grafana](https://grafana.com/).

## Features

- Copy a dashboard and its associated alerts and library panels from one instance to another.

- Showing folders, dashboards, library panels and alerts of a given grafana instance.

- Adjusting library panels where a specific target structure needs repetition
  with different data sources.

## Usage

active-grafana provides commands to ease the execution of recurring tasks on
running grafana instances.  Use `./grafurious --help` to display a message
explaining these commands. `--help` or `-h` work on any (sub-)command.

Help entries show environment variable names like `URL`, `FROM_URL` or `TO_MESSAGE`,
use those environment variables alternatively to command options.

Find all environment variables names in `active-grafana.settings`.

### The copy command

To copy a dashboard identified by its title use the following command in the
root of this project:

```
./grafurious copy dashboard "My example Dashboard" --source-url <source-url> --source-token <source-token> --target-url <target-url> --target-token <target-token>
```

### The legacy commands

#### Adjusting library panels using the `--i-am-an-expert` command option

The standard way of adjusting a library panel is:
- get the library panel with the given `PANEL_UID`
- take the first `target` as reference target
- create the new target list by repeating the reference target adjusted for the
  data source UID and the reference id based on the provided `DATASOURCE-UIDS`.
- update the library panel with the newly created target list

Sometimes one or more targets need more adjustments than the data source UID and
the reference-id. Use the `--i-am-an-expert` argument to achieve more advanced
adjustments. Example:

```
./grafurious legacy adjust --url=<grafana-url> --token=<grafana-token> --panel-uid=<panel-uid> \
       --datasource-uids="<datasource-uid-1>,<datasource-uid-2>,...,<datasource-uid-n>" \
       --i-am-an-expert="{:path \"<path-to-f-target->target>\" :data <data>}"
```

The `i-am-an-expert` command option gets transformed to a map using
`clojure.edn/read-string`. The `path` is processed with `(load-string (slurp
path))`. That is, the path should lead to a file containing a function like the
following:

```
(defn f-target->target
  [reference-target uid data]
  (assoc (assoc-in reference-target ["datasource" "uid"] uid) "refId" (get data uid)))
```
The `data` is provided to this loaded function, e.g.:

```
{"<datasource-uid-1>" "ref-id-1"
 "<datasource-uid-2>" "ref-id-2"
 "..."                "..."
 "<datasource-uid-n>" "ref-id-n"}
```

The result of this example is, that the target does not use the data source UID
as reference id, but a reference id provided as {"uid" "ref-id"} map via the
command-line.

Note: make sure to escape strings in the `--i-am-an-expert`
command option in the proper way, e.g.:
```
./grafurious adjust <... other command options ...> --i-am-an-expert="{:path \"file.path\" :data {\"uid-1\" \"my-uid-1-data\"}}"
```

## Known Issues and 'good to know'

- Error handling:
  If something goes wrong, you are immediately provided with the plain
  grafana API error messages. Currently, there is no fallback or further help
  provided.

- Testing:
  Only some core functions are tested yet.

- Logging and Debugging:
  The current logging and debugging system is rudimentary and might even be
  confusing.

## License

Copyright © 2023-2026 Active Group GmbH

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
