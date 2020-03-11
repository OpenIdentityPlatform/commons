# XCite Maven Plugin

The XCite Maven plugin lets you
copy a quote from one text file into another text file.

The primary use case is citing parts of non-XML files in XML documentation.
XML Inclusions, <http://www.w3.org/TR/xinclude/>,
allow you include an entire text file in an XML document.
(Use `parse="text"`.)
XML Inclusions do not however let you include only part of another non-XML file.


## Sample Configuration

Add the configuration in your `pom.xml` to the project > build > plugins list.

The following configuration resolves citations in XML files
under `src/main/docbkx`, writing the new files under `target/xcite`.
When quoting from another file, it escapes XML (e.g. `<` becomes `&lt;`).
It first removes initial spaces to align the quote flush against the left margin,
and then it indents quotes 4 single spaces.

    <plugin>
      <groupId>org.forgerock.maven.plugins</groupId>
      <artifactId>xcite-maven-plugin</artifactId>
      <version>1.0.0-SNAPSHOT</version>
      <inherited>false</inherited>
      <executions>
        <execution>
          <phase>process-sources</phase>
          <goals>
            <goal>cite</goal>
          </goals>
          <configuration>
            <sourceDirectory>${basedir}/src/main/docbkx</sourceDirectory>
            <includes>
              <include>**/*.xml</include>
            </includes>
            <escapeXml>true</escapeXml>
            <reindent>4</reindent><!-- Start quotes indented 4 spaces. -->
            <outputDirectory>${project.build.dir}/xcite</outputDirectory>
          </configuration>
        </execution>
      </executions>
    </plugin>


## Quoting Parts of Other Files

XCite works by replacing citation strings with quotes.

    Hello world.
    This is a file that quotes `/path/to/script.sh.`
    
    Here comes the quote:
    
    [/path/to/script.sh:# start:# end]
    
    And here's the rest of the file.

In the example shown, the citation string is
`[/path/to/script.sh:# start:# end]`.

Suppose `/path/to/script.sh` contains the following content.

     #!/bin/bash
     
     # start
     wall <<EOM
         Hello world
     EOM
     # end
     
     exit

Then running XCite on the file results in the following output.

    Hello world.
    This is a file that quotes `/path/to/script.sh.`
    
    Here comes the quote:
    
     wall <<EOM
         Hello world
     EOM
    
    And here's the rest of the file.

Notice that the citation has the following form,
inspired by [JCite](http://www.arrenbrecht.ch/jcite/):

    [path:start-marker:end-marker]

* The `path` is the path to the file to quote.
* The `start-marker` is the string marking the start of the quote.
* The `end-marker` is the string marking the end of the quote.

The `path` is required, and must reference a readable file (assumed to be text).

If you omit the `start-marker`, XCite includes the entire file.

If you include the `start-marker` but omit the `end-marker`,
XCite assumes that the `start-marker` and `end-marker` are identical.

Neither `path`, nor `start-marker`, nor `end-marker` can contain the delimiter.
The delimiter is `:` by default, optionally `%` instead.

The `start-marker` and `end-marker` are not part of the quote.
The `start-marker` and `end-marker` must be either
on separate lines from the quote,
or on the same line as the (single-line) quote.
When the `start-marker` and `end-marker` are on separate lines from the quote,
the lines containing the `start-marker` and `end-marker`
are not part of the quote.

Suppose `file.txt` contains the following lines:

    (start)XCite can recognize this as a quote.(end)

    // The marker is in this line.
    XCite can recognize this as a quote.
    // The marker is in this line.

    // WRONG
    XCite does not recognize this as a quote. // WRONG

* The citation string `[file.txt:start:end]` resolves to
  `XCite can recognize this as a quote.`
* The citation string `[file.txt%marker]` resolves to
  `XCite can recognize this as a quote.`
* The citation string `[file.txt:WRONG]` resolves to an empty string.


## Limitations

Although XCite does resolve citations recursively, it does not check for loops.
A loop occurs when a quote includes a citation quoting the original citation.

If you create a citation loop,
XCite processes it recursively until it runs out of memory.


## Reference

By default, XCite runs in the `process-sources` phase, `cite` goal.

XCite has the following optional configuration:

* `<escapeXml>true</escapeXml>`: escape XML characters in quotes.<br>
  Default: false.
* `<excludes><exclude>**/exclude/*.*</exclude></excludes>`: ignore `exclude/` dirs.<br>
  Default: ignore image files and default excludes, such as SCM files.
* `<includes><include>**/*.xml</include></includes>`: process only XML.<br>
  Default: replace citations with quotes in all source files.
* `<reindent>int</reindent>`: start quotes at `int` column position.<br>
  Default: start quotes at column 0.
* `<outputDirectory>dir</outputDirectory>`: output files with quotes.<br>
  Default: `${project.build.directory}/xcite`.
* `<sourceDirectory>dir</sourceDirectory>`: input files with citations.<br>
  Default: `${basedir}/src/main`.

*****

    The contents of this file are subject to the terms of the
    Common Development and Distribution License, Version 1.0 only
    (the "License").  You may not use this file except in compliance
    with the License.
    
    You can obtain a copy of the license at legal-notices/CDDLv1_0.txt
    or http://forgerock.org/license/CDDLv1.0.html.
    See the License for the specific language governing permissions
    and limitations under the License.
    
    When distributing Covered Code, include this CDDL HEADER in each
    file and include the License file at legal-notices/CDDLv1_0.txt.
    
    If applicable, add the following below this CDDL HEADER, with the
    fields enclosed by brackets "[]" replaced with your own identifying
    information:
    
        Portions Copyright [yyyy] [name of copyright owner]

    Copyright 2014 ForgeRock AS
