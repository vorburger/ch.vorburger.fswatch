# ch.vorburger.fswatch

Java lib for monitoring directories or individual files based on the `java.nio.file.WatchService`.

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/ch.vorburger/fswatch/badge.svg)](https://maven-badges.herokuapp.com/maven-central/ch.vorburger/fswatch)
[![Javadocs](http://www.javadoc.io/badge/ch.vorburger/fswatch.svg)](http://www.javadoc.io/doc/ch.vorburger/fswatch)
[![Build Status](https://travis-ci.org/vorburger/ch.vorburger.fswatch.svg?branch=master)](https://travis-ci.org/vorburger/ch.vorburger.fswatch)

## Usage

[Get it from Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22ch.vorburger%22%20AND%20a%3A%22fswatch%22) and see [the ExampleMain class](src/test/java/ch/vorburger/fswatch/test/ExampleMain.java) for how to use this library.



## History

This code was originally (in 2015) written for https://github.com/vorburger/HoTea,
and factored out of it in 2018 for re-use in project https://github.com/vorburger/ch.vorburger.osgi.gradle.

## Related projects

* https://github.com/google/guava/issues/2030
* [commons-io](https://commons.apache.org/proper/commons-io/) with its [File Monitor](https://commons.apache.org/proper/commons-io/javadocs/api-release/index.html?org/apache/commons/io/monitor/package-summary.html): Polling, not using Java 7+ NIO WatchService.
* https://github.com/Hindol/commons
* https://github.com/SenorArchitect/fswatcher: NOK due to https://github.com/SenorArchitect/fswatcher/issues/2 :-(
* https://github.com/azemm/FsWatcher
* https://github.com/encima/FSWatcher7
* https://www.teamdev.com/jxfilewatcher: Commercial, are they serious?!
