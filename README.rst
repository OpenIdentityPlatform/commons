************************************
Akiban Persistit
************************************

Important Note
==============
This is a fork of Akiban Persistit™ since it's not maintained anymore by their authors.

Overview
========

Persistit is a fast and reliable key/value data storage library written in Java™. Key features include:

- Support for highly concurrent transaction processing with multi-version concurrency control
- Optimized serialization and deserialization mechanism for Java primitives and objects
- Multi-segment (compound) keys to enable a natural logical key hierarchy
- Support for long records (megabytes)
- Implementation of a persistent SortedMap
- Extensive management capability including command-line and GUI tools

Building From Source
=====================================
Use Maven (http://maven.apache.org) to build Persistit.

To build::

  mvn install

The resulting jar files are in the ``target`` directory. To build the Javadoc::

  mvn javadoc:javadoc

The resulting Javadoc HTML files are in ``target/site/apidocs``.

Building and Running the Examples
---------------------------------

Small examples are located in the ``examples`` directory. Each has a short README file describing the example, and an Ant build script (http://ant.apache.org). After building the main akiban-persistit jar file using Maven, you may run::

  ant run

in each of the examples subdirectories to build and run the examples.

Licensing
---------
This version of Persistit is licensed under the Apache License, Version 2.0. By installing, copying or otherwise using the Software contained in the distribution kit, you agree to be bound by the terms of the license agreement. If you do not agree to these terms, remove and destroy all copies of the software in your possession immediately.
