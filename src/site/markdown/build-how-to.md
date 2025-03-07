# Build from sources

This document provides instruction to build this plugin from sources.

## Get the sources

Optionally fork the repository (if you plan to submit PR).

Clone the repository

### Clone without forking

```shell]
cd <folder where you want to clone this project>
git clone https://github.com/lgringo/web-resource-deduplicator-maven-plugin.git
```

### Clone your fork

1. Fork the repository (go to https://github.com/lgringo/web-resource-deduplicator-maven-plugin.git, clic Fork)
2. clic on code choose the protocol (let's say ssh)
3. copy the url

```shell
cd <folder where you want to clone this project>
git clone <paste the url>
```

## Build

Go to the folder and type :

```shell
mvn clean verify
```

### mise-en-place

I'm using mise-en-place to set up tool chain. If you're using it too, you can just type ``mise install``

## Build the website

```shell
mvn site:site && mvn site:stage
```
