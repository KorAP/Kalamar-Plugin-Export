# Kalamar-Plugin-Export

![Kalamar-Plugin-Export Screenshot](https://raw.githubusercontent.com/KorAP/Kalamar-Plugin-Export/master/misc/kalamar-export-screenshot.png)

## Description

Kalamar-Plugin-Export is a web service that integrates in the plugin framework of
[Kalamar](https://github.com/KorAP/Kalamar), to export matches for the
[KorAP Corpus Analysis Platform](http://korap.ids-mannheim.de/) in various formats.
Currently supported are **RTF**, **CSV**, and **JSON**.

Kalamar-Plugin-Export is meant to be a basic export plugin and should
demonstrate and evaluate the plugin capabilities of Kalamar.


## Prerequisites

Java 8 (OpenJDK or Oracle JDK),
[Git](http://git-scm.com/),
[Maven 3](https://maven.apache.org/).
Further dependencies are resolved using Maven.


## Build

To build the latest version of Kalamar-Plugin-Export, do ...

```shell
$ git clone https://github.com/KorAP/Kalamar-Plugin-Export
$ cd Kalamar-Plugin-Export
```

... and build the jar file ...


```shell
$ mvn clean package
```

Afterwards the jar file is located in the `target/` folder and can
be started with ...

```shell
$ java -jar KalamarExportPlugin-[VERSION].jar
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
      "template" : "http://localhost:7777/export"
    }
  }]
}
```

An [example demo](https://github.com/KorAP/Kalamar/blob/master/dev/demo/export.html)
showcases the embedded plugin.


## Customization

The default configuration file is stored in `src/main/resources/exportPlugin.conf`.
This file can be overwritten, when a file of that name is stored in the
same directory as the jar file.


## License

Copyright (c) 2020, [IDS Mannheim](http://ids-mannheim.de/), Germany

Kalamar-Plugin-Export is developed as part of the [KorAP](http://korap.ids-mannheim.de/)
Corpus Analysis Platform at the Leibniz Institute for the German Language
([IDS](http://ids-mannheim.de/)).

Kalamar-Plugin-Export is published under the
[BSD-2 License](https://raw.githubusercontent.com/KorAP/Kalamar-Plugin-Export/master/LICENSE).
