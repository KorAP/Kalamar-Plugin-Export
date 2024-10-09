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

This template will be also be displayed then starting the jar with the argument [-h|--help]. Alternatively it is returned by:

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

Alternatively a file named `exportPlugin.conf` can be stored in the
same directory as the java jar.

## License

Copyright (c) 2020-2024, [IDS Mannheim](https://www.ids-mannheim.de/), Germany

Kalamar-Plugin-Export is developed as part of the [KorAP](https://korap.ids-mannheim.de/)
Corpus Analysis Platform at the Leibniz Institute for the German Language
([IDS](https://www.ids-mannheim.de/)).

Kalamar-Plugin-Export is published under the
[BSD-2 License](https://raw.githubusercontent.com/KorAP/Kalamar-Plugin-Export/master/LICENSE).
