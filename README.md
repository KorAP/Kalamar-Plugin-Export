# Kalamar-Plugin-Export

![Kalamar-Plugin-Export Screenshot](https://raw.githubusercontent.com/KorAP/Kalamar-Plugin-Export/master/misc/kalamar-export-screenshot.png)

## Description

Kalamar-Plugin-Export is a web service that integrates in the plugin framework of
[Kalamar](https://github.com/KorAP/Kalamar), to export matches for the
[KorAP Corpus Analysis Platform](https://korap.ids-mannheim.de/) in various formats.
Currently supported are **RTF**, **CSV**, and **JSON**.

Kalamar-Plugin-Export is meant to be a basic export plugin and should
demonstrate and evaluate the plugin capabilities of Kalamar.


## Prerequisites

Java 17 (OpenJDK or Oracle JDK),
[Git](http://git-scm.com/),
[Maven 3](https://maven.apache.org/).
Further dependencies are resolved using Maven.


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

Afterwards the jar file is located in the `target/` folder and can
be started with ...

```shell
java -jar KalamarExportPlugin-[VERSION].jar [-h|--help]
```

Per default, this will start a server at `http://localhost:7777`.
It will also create a subfolder `files` to temporarily store created
exports.

Registration of the plugin in Kalamar is not yet officially supported.
Registration works by passing the following JSON blob
to the plugin registration handler.

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

At the moment the integration reuses the session of the host service.
To make this possible, the [Content Security Policy](https://developer.mozilla.org/en-US/docs/Web/HTTP/CSP)
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
java -jar KalamarExportPlugin-[VERSION].jar myconf_exportPlugin.conf
```

You can also define only single properties in the new config file. In this case only these properties overwrite the properties in the basic configuration file.

Alternatively a file named `exportPlugin.conf` can be stored in the
same directory as the java jar.

### Environment Variables

Configuration can also be set via environment variables, which take precedence over
configuration file values. This is particularly useful for Docker deployments and
containerized environments.

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
- `server.scheme` or `KALAMAR_EXPORT_SERVER_SCHEME`: URL scheme for the server (default: `https`)
- `server.origin` or `KALAMAR_EXPORT_SERVER_ORIGIN`: CORS origin for SSE responses (default: `*`)

#### API Configuration

- `api.port` or `KALAMAR_EXPORT_API_PORT`: Port of the KorAP API backend (default: `443`)
- `api.host` or `KALAMAR_EXPORT_API_HOST`: Host address of the KorAP API backend (default: `korap.ids-mannheim.de`)
- `api.scheme` or `KALAMAR_EXPORT_API_SCHEME`: URL scheme for the API backend (default: `https`)
- `api.source` or `KALAMAR_EXPORT_API_SOURCE`: Source string for exports, useful when running behind a proxy (optional)
- `api.path` or `KALAMAR_EXPORT_API_PATH`: Additional path prefix for the API (default: empty)

#### Asset Configuration

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

### Docker Example

When running in Docker, you can set environment variables:

```shell
docker run -e KALAMAR_EXPORT_SERVER_PORT=8080 \
           -e KALAMAR_EXPORT_API_HOST=api.example.com \
           -e KALAMAR_EXPORT_MAX_EXP_LIMIT=50000 \
           kalamar-export-plugin
```

## License

Copyright (c) 2020-2026, [IDS Mannheim](https://www.ids-mannheim.de/), Germany

Kalamar-Plugin-Export is developed as part of the [KorAP](https://korap.ids-mannheim.de/)
Corpus Analysis Platform at the Leibniz Institute for the German Language
([IDS](https://www.ids-mannheim.de/)).

Kalamar-Plugin-Export is published under the
[BSD-2 License](https://raw.githubusercontent.com/KorAP/Kalamar-Plugin-Export/master/LICENSE).
