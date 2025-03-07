# web-resource-deduplicator-maven-plugin

![Build & Tests](https://github.com/lgringo/web-resource-deduplicator-maven-plugin/actions/workflows/buildTests.yml/badge.svg?branch=main)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=lgringo_web-resource-deduplicator-maven-plugin&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=lgringo_web-resource-deduplicator-maven-plugin)

The **web-resource-deduplicator-maven-plugin** allows you to remove resources that are not referenced by the HTML or CSS files of a website. It is designed to work on the website produced by the maven-site-plugin. Depending on the skin used, several resource files, such as font files, may be included but not used. This plugin will list all resource files (images, fonts, CSS), then check if any CSS or HTML file references them. If not, the resource will be deleted.

Additionally, in a multi-module Maven project, the maven-site-plugin generates a website for each module. During the stage goal, these sites are aggregated into the target/staging directory of the parent module to ensure that the links work correctly. However, each site contains the same resources (font files, images, etc.). The **web-resource-deduplicator-maven-plugin** searches the staging directory for duplicate resources, removes those that are lower in the hierarchy, and updates the references in the HTML and CSS files to point to the resources in the parent site.

## Features

- **web-resource-deduplicator-maven-plugin**: Automatically scans your project for duplicate resources such as images, scripts, and stylesheets.
- **Safe Removal**: Safely removes duplicates without affecting your site's functionality.
- **Configuration**: The resources to be deduplicated are configurable by their extensions.

## Installation

To use the **web-resource-deduplicator-maven-plugin**, add the following configuration to your `pom.xml` file:

```xml
<build>
    <plugins>
            <plugin>
                <groupId>eu.gyfz</groupId>
                <artifactId>web-resource-deduplicator-maven-plugin</artifactId>
                <version>1.0.0-SNAPSHOT</version>
            </plugin>
    </plugins>
</build>
```

## Usage

Once installed, you can run the plugin using the following Maven command:
```shell
mvn eu.gyfz:web-resource-deduplicator-maven-plugin:deduplicate
```

This will scan your project for duplicate resources and remove them.

## Configuration

You can customize the behavior of the plugin by adding configuration options in your pom.xml:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>eu.gyfz</groupId>
            <artifactId>web-resource-deduplicator-maven-plugin</artifactId>
            <version>1.0.0-SNAPSHOT</version>
                <configuration>
                    <skip>true</skip>
                    <stagingDirectory>/where/is/store/your/website</stagingDirectory>
                    <resourcesExtensions>
                        <resourcesExtension>.css</resourcesExtension>
                        <resourcesExtension>.ttf</resourcesExtension>
                    </resourcesExtensions>
                </configuration>
        </plugin>
    </plugins>
</build>
```

 * **skip** : Skip the plugin. Can be use with a property.
   * default: `false`
 * **stagingDirectory** : root folder of website. Typically, the staging directory used by the maven-site-plugin plugin. 
     * default: `${project.build.directory}/staging`
 * **resourcesExtensions** : List of resource extensions to deduplicate
     * default: `".css", ".js", ".png", ".jpg", ".jpeg", ".webp", ".gif", ".svg", ".woff2", ".woff", ".ttf", ".eot", ".otf"`

## Disclaimer

This plugin is designed to reduce the size of a site generated by the maven-site-plugin. Theoretically, it could work with any simple site. However, that is not its purpose, and I reserve the right not to implement certain feature requests if they are unrelated to the primary goal of this plugin.

## Contributing

Contributions are welcome! Please open an issue or submit a pull request.

## License

This project is licensed under the Apache License, Version 2. See the license file here: [LICENSE](https://www.apache.org/licenses/LICENSE-2.0).
