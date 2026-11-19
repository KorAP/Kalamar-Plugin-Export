# Kalamar-Plugin-Export

![Kalamar search results with the built-in export form open](misc/kalamar-export-screenshot.png)

## Description

Kalamar-Plugin-Export provides **RTF**, **CSV**, and **JSON** exports of search
results from the [KorAP Corpus Analysis Platform](https://korap.ids-mannheim.de/).
With [Kalamar](https://github.com/KorAP/Kalamar) 0.67 or later, it serves as the
backend for the built-in export interface. The iframe plugin interface remains
available for legacy integrations.


## Integration with Kalamar

### Docker Compose with Kalamar 0.67 or later

In your existing `compose.yaml`, add or update `services.kalamar-export`. Add
`KALAMAR_EXPORT_SERVICE` to the `environment:` of the Kalamar service that
serves the UI. In KorAP-Docker, that service is `kalamar` for the `open` profile
or `kalamar-auth` for the `auth` profile.

Merge this excerpt into your existing `services:` block and keep the other
settings for your Kalamar and Kustvakt services.

```yaml
services:
  kalamar:
    environment:
      KALAMAR_EXPORT_SERVICE: "http://kalamar-export:7777"
  kalamar-export:
    image: "docker.io/korap/kalamar-plugin-export:0.5.2"
```

This is the **zero-configuration Docker setup** for the export container. It
listens on port `7777` and reaches Kustvakt at `http://kustvakt:8089` by default.
Keep all three services on a shared Compose network. If you rename
`kalamar-export`, use that name in `KALAMAR_EXPORT_SERVICE`. When upgrading an
existing export container, remove any API URL, host, port, scheme and path
overrides to use the default backend.

Kalamar provides the export interface and forwards requests, including
authentication, through its `/export/` proxy. Kalamar reaches the export
container over the shared network without a `ports:` entry on `kalamar-export`.
Recreate the services whose configuration changed with `docker compose up -d`
using your usual profiles. On a results page, open the export form and try a
small CSV download.

### When Kustvakt uses a different address

The export service should use the same Kustvakt backend as Kalamar. If your
backend still listens on port `8089`, you can give it the `kustvakt` network
alias and keep the export defaults. For example, in a setup where the backend
service for the `auth` profile is named `kustvakt-auth` and uses the default
Compose network, add `kustvakt` to that backend service's existing aliases:

```yaml
services:
  kustvakt-auth:
    networks:
      default:
        aliases:
          - kustvakt
```

Alternatively, set `KALAMAR_EXPORT_API_URL` in the export service's
`environment:` block. For example, if the backend is named `kustvakt-full`
and listens on port `9000`:

```yaml
services:
  kalamar-export:
    image: "docker.io/korap/kalamar-plugin-export:0.5.2"
    environment:
      KALAMAR_EXPORT_API_URL: "http://kustvakt-full:9000"
```

Use the backend's base URL. The export service appends `/api/v1.0/search`,
including when the base URL has a path prefix. Version 0.5.2 introduced the
single URL setting. Earlier images use the separate host, port and scheme
variables listed in the [configuration reference](#api-configuration).

### Kalamar configuration file

If you configure Kalamar through `kalamar.conf`, set `export` in its `Kalamar`
section. Use the address that Kalamar can reach, such as the Compose service
address in this example:

```perl
{
  Kalamar => {
    export => 'http://kalamar-export:7777'
  }
}
```

Restart Kalamar after changing its configuration. For deployments using the
iframe plugin interface, see [Legacy integration](#legacy-integration).

## Docker

The Docker image uses a compact Java 25 runtime linked from Corretto
25.0.5.1.2, which includes the fix for CVE-2026-114938 (Java4Shell).
Build and runtime base images are pinned by digest. Maven tests run on the
linked runtime's module set before the application is packaged.

The final image uses glibc. Alpine-based Java images have caused compatibility
problems on older Docker hosts, including the CoRoLa CentOS 7 server; keep
Alpine confined to the build stage. The runtime retains TLS, authentication,
locale and diagnostic modules, but does not include a compiler or Maven.

To run the service locally for Kalamar's built-in export interface, using the
public KorAP API as the backend:

```shell
docker run --rm --name korap-export \
  -p 127.0.0.1:7777:7777 \
  -e KALAMAR_EXPORT_API_URL=https://korap.ids-mannheim.de \
  korap/kalamar-plugin-export:0.5.2
```

The image listens on port `7777`. This command publishes the port on the
Docker host's loopback interface.
If Kalamar runs directly on that host, use
`KALAMAR_EXPORT_SERVICE=http://127.0.0.1:7777`.

When Kalamar also runs in Docker, use a shared Compose network and the export
service's Docker service name, such as `http://kalamar-export:7777`, instead of
`localhost`. The export service must listen on `0.0.0.0` inside its container;
a host port mapping is not needed for communication within the Compose network.

See the [configuration reference](#configuration-reference) for backend settings
and export limits. Bind-mounted export directories must be writable by the
container's service user (UID `100`, GID `101`).

## Prerequisites

For building from source, install a **Java 21 or newer JDK**,
[Git](https://git-scm.com/), and [Maven 3](https://maven.apache.org/).
Further dependencies are resolved using Maven. The published Docker image
provides its own Java 25 runtime.


## Build

To build the latest version of Kalamar-Plugin-Export, do ...

```shell
git clone https://github.com/KorAP/Kalamar-Plugin-Export
cd Kalamar-Plugin-Export
```

... and build the jar file ...


```shell
mvn clean package
```

The JAR is located in `target/` and can be run with Java 21 or newer.
Replace `VERSION` with the built version:

```shell
java -jar target/KalamarExportPlugin-VERSION.jar
```

By default, this starts a server at `http://localhost:7777`. Export files are
stored in Java's temporary directory; set `conf.file_dir` to use another location.

## Legacy integration

For Kalamar deployments using the iframe plugin interface, register the export
form with the following JSON definition:

```json
{
  "name" : "Export",
  "desc" : "Exports Kalamar results",
  "embed" : [{
    "panel" : "result",
    "title" : "exports KWICs and snippets",
    "icon" : "\uf019",
    "classes" : ["button-icon", "plugin" ],
    "onClick" : {
      "action" : "addWidget",
      "template" : "http://localhost:7777/export",
      "permissions":["forms","scripts","downloads"]
    }
  }]
}
```

This template will also be displayed when starting the jar with the argument [-h|--help]. Alternatively it is returned by:

```shell
http://localhost:7777/export/template
```

At the moment the simplest way to register the plugin is by storing the json
blob in a file (the blob needs to be in a list, i.e. surrounded by `[...]`).
The file then can be loaded using the `Plugins` addon in Kalamar using the
Kalamar configuration file.

```perl
{
  Kalamar => {
    plugins => ['Plugins'],
  },
  'Kalamar-Plugins' => {
    default_plugins => 'plugins.json'
  }
}
```

For example, with this configuration addition, the content
of `plugins.json` will be registered on every Kalamar page load.

An [example demo](https://github.com/KorAP/Kalamar/blob/master/dev/demo/export.html)
showcases the embedded plugin.

The legacy iframe integration can reuse the session cookie of the host service.
For this setup, the [Content Security Policy](https://developer.mozilla.org/en-US/docs/Web/HTTP/CSP)
of Kalamar needs to be extended to include the whole domain that
hosts both services (at the moment it's not possible to host the service on a different domain).
If the domain is, e.g., `ids-mannheim.de`, the configuration in the Kalamar configuration
file needs to be:

```perl
{
  CSP => {
    'frame-src' => 'self',
    'frame-ancestors' => ['self','https://*.ids-mannheim.de/']
  }
}
```

## Customization

The basic configuration file is stored in `src/main/resources/exportPlugin.conf`.
To change the configuration create a new config file and run the jar with the
according filename as argument:

```shell
java -jar target/KalamarExportPlugin-VERSION.jar myconf_exportPlugin.conf
```

You can also define only single properties in the new config file. In this case only these properties overwrite the properties in the basic configuration file.

Alternatively a file named `exportPlugin.conf` can be stored in the
working directory from which the service is started.

### Environment Variables

Configuration can also be set via environment variables, which take precedence over
configuration file values. In Compose, place the variables listed below under
`services.kalamar-export.environment`, substituting your export service name if
it differs. Docker images from version 0.5.2 listen on `0.0.0.0` by default and
use `http://kustvakt:8089` as the backend. The standalone JAR
listens on `localhost` and defaults to `https://korap.ids-mannheim.de:443`.
The API defaults apply when there is no local `exportPlugin.conf` replacing
the bundled configuration. Custom settings and environment overrides still work.

`KALAMAR_EXPORT_API_URL` accepts an absolute HTTP(S) base URL, such as
`http://kustvakt:8089` or `https://backend.example/instance/wiki`. It takes
precedence over all four separate API scheme, host, port and path settings.
An omitted port means 80 for HTTP or 443 for HTTPS, and an omitted path clears
any configured path prefix. Credentials, query strings and fragments are not
accepted. The separate variables remain supported when the URL is unset or empty.

The priority order is (highest to lowest):
1. Environment variables
2. Custom configuration file (if provided as argument)
3. Default configuration file (`exportPlugin.conf`)

### Configuration Reference

The following configuration options are available. Each can be set either in a
configuration file (using the property name) or via an environment variable:

#### Server Configuration

- `server.port` or `KALAMAR_EXPORT_SERVER_PORT`: Port of the export server (default: `7777`)
- `server.host` or `KALAMAR_EXPORT_SERVER_HOST`: Host address of the server (default: `localhost`)
- `server.scheme` or `KALAMAR_EXPORT_SERVER_SCHEME`: URL scheme for generated
  legacy plugin URLs (default: `https`). Set to `http` for direct HTTP use of the
  legacy form; this setting does not enable TLS and is not needed for Kalamar's
  built-in integration.
- `server.origin` or `KALAMAR_EXPORT_SERVER_ORIGIN`: CORS origin for SSE responses (default: `*`)

#### API Configuration

The defaults below are for the standalone JAR.

- `api.port` or `KALAMAR_EXPORT_API_PORT`: Port of the KorAP API backend (default: `443`)
- `api.host` or `KALAMAR_EXPORT_API_HOST`: Host address of the KorAP API backend (default: `korap.ids-mannheim.de`)
- `api.scheme` or `KALAMAR_EXPORT_API_SCHEME`: URL scheme for the API backend (default: `https`)
- `api.source` or `KALAMAR_EXPORT_API_SOURCE`: Source string for exports, useful when running behind a proxy (optional)
- `api.path` or `KALAMAR_EXPORT_API_PATH`: Additional path prefix for the API (default: empty)

#### Asset Configuration

The built-in export interface uses Kalamar's own assets.

For the legacy export form accessed through Kalamar's `/plugin/export/` proxy,
JavaScript and CSS are loaded from the same Kalamar frontend using relative URLs.
No asset configuration is needed for this proxy setup, including with HTTPS,
a different port, or a reverse-proxy path prefix. The settings below apply to
standalone use of the export form.

- `asset.host` or `KALAMAR_EXPORT_ASSET_HOST`: Host address for assets/stylesheets (default: `korap.ids-mannheim.de`)
- `asset.port` or `KALAMAR_EXPORT_ASSET_PORT`: Port for assets (default: empty, uses default port)
- `asset.scheme` or `KALAMAR_EXPORT_ASSET_SCHEME`: URL scheme for assets (default: `https`)
- `asset.path` or `KALAMAR_EXPORT_ASSET_PATH`: Path prefix for assets (default: empty)

#### Export Configuration

- `conf.page_size` or `KALAMAR_EXPORT_PAGE_SIZE`: Number of matches to fetch per API request (default: `5`)
- `conf.max_exp_limit` or `KALAMAR_EXPORT_MAX_EXP_LIMIT`: Maximum number of matches allowed per export (default: `10000`)
- `conf.file_dir` or `KALAMAR_EXPORT_FILE_DIR`: Directory for temporary export files (default: system temp directory)
- `conf.default_hitc` or `KALAMAR_EXPORT_DEFAULT_HITC`: Default number of hits in the export form (default: `100`)

#### Cookie Configuration

- `cookie.name` or `KALAMAR_EXPORT_COOKIE_NAME`: Name of the Kalamar session cookie (default: `kalamar`)

## License

Copyright (c) 2020-2026, [IDS Mannheim](https://www.ids-mannheim.de/), Germany

Kalamar-Plugin-Export is developed as part of the [KorAP](https://korap.ids-mannheim.de/)
Corpus Analysis Platform at the Leibniz Institute for the German Language
([IDS](https://www.ids-mannheim.de/)).

Kalamar-Plugin-Export is published under the
[BSD-2 License](https://raw.githubusercontent.com/KorAP/Kalamar-Plugin-Export/master/LICENSE).
