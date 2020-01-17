# ForgeRock Doc Maven Plugin

The ForgeRock Doc Maven Plugin builds ForgeRock core documentation
from DocBook XML sources.

This README describes what the plugin expects of core documentation sources,
and shows how to use some of its key features.

The ForgeRock Doc Maven Plugin relies on default branding and common content.
The README shows how to use alternative branding and common content.

_This document covers functionality present in 3.0.0._


# About the ForgeRock Doc Maven Plugin

The Maven plugin centralizes configuration of core documentation,
to ensure that documents are formatted and laid out uniformly.

The project runs multiple plugin executions:

1.  A `pre-site` phase `process` goal to pre-process documents.
2.  A `pre-site` phase `build` goal to format documents (HTML, PDF, etc.)
3.  A `site` phase `site` goal to lay out documents in site format
4.  A `site` phase `release` goal to lay out documents in release format
5.  A `site` phase `backstage` goal to lay out documents in Backstage format

With centralized configuration handled by this Maven plugin,
the overall configuration requires at least these arguments:

*   `<projectName>`: OpenAM, OpenDJ, OpenICF, OpenIDM, OpenIG, and so forth
*   `<projectVersion>`: the version, such as `1.0.0-SNAPSHOT`, or `3.3.1`
*   `<releaseVersion>`: the release version, such as `1.0.0`, or `3.3.1`

Other features are described in this README.


## Plugin Configuration

You call the plugin from your `pom.xml` as in the following example.

    <build>
     <plugins>
      <plugin>
       <groupId>org.forgerock.commons</groupId>
       <artifactId>forgerock-doc-maven-plugin</artifactId>
       <version>${forgerockDocPluginVersion}</version>
       <inherited>false</inherited>
       <configuration>
        <projectName>MyProject</projectName>
        <projectVersion>1.0.0-SNAPSHOT</projectVersion>
        <releaseVersion>1.0.0</releaseVersion>
       </configuration>
       <executions>
        <execution>
         <id>pre-process-doc</id>
         <phase>pre-site</phase>
         <goals>
          <goal>process</goal>
         </goals>
        </execution>
        <execution>
         <id>build-doc</id>
         <phase>pre-site</phase>
         <goals>
          <goal>build</goal>
         </goals>
        </execution>
        <execution>
         <id>layout-site</id>
         <phase>site</phase>
         <goals>
          <goal>site</goal>
         </goals>
        </execution>
        <execution>
         <id>layout-release</id>
         <phase>site</phase>
         <goals>
          <goal>release</goal>
         </goals>
        </execution>
        <execution>
         <id>layout-backstage</id>
         <phase>site</phase>
         <goals>
          <goal>backstage</goal>
         </goals>
        </execution>
       </executions>
      </plugin>
     </plugins>
    </build>

In the example above, `<inherited>false</inherited>` means
only use this plugin configuration for this project or module,
not recursively for all modules that are children of this project.

Unless you want to run the plugin recursively, set `<inherited>false</inherited>`.

## Plugin Output

When you run the `pre-site` phase `build` goal,
the plugin builds the HTML, PDF, etc.,
which you find under `${project.build.directory}/docbkx`,
which is usually `target/docbkx`.

When you run the `site` phase `site` goal,
the plugin copies the documents it built during the `pre-site` phase
under `${project.build.directory}/site/doc` as expected for a Maven project site.

The plugin adds an `index.html` in the `site/doc` directory.
That `index.html` file redirects browsers
to `http://project.forgerock.org/docs.html`,
so be sure to add a `docs.html` to your Maven site.

When you run the `site` phase `release` goal,
the plugin lays out copies of HTML and PDF
under `${project.build.directory}/release/releaseVersion`,
where `releaseVersion` is the release version set in the configuration.

The files in `release/releaseVersion` are those suitable for publication.


## Source Layout Requirements

The plugin assumes that all of your DocBook XML documents
are found under `src/main/docbkx/`.

Documents are expected to be found in folders under that path,
where the folder name is a lowercase version of the document name,
such as `release-notes`, `admin-guide`, `reference`, or similar.

Furthermore, all documents have the same file name
for the file containing the top-level document element, by default `index.xml`.

The plugin expects all images in an `images` folder inside the document folder.

An example project layout looks like this:

     src/main/docbkx/
      legal.xml
      admin-guide/
       images/
       index.xml
       ...other files...
      dev-guide/
       images/
       index.xml
       ...other files...
      install-guide/
       images/
       index.xml
       ...other files...
      reference/
       images/
       index.xml
       ...other files...
      release-notes/
       images/
       index.xml
       ...other files...
      shared/
       sec-accessing-doc-online.xml
       sec-formatting-conventions.xml
       sec-interface-stability.xml
       sec-joining-the-community.xml
       sec-release-levels.xml
       ...other files...

During the build,
the plugin makes a copy of the original sources under the build directory.
It then works on the copy, rather than the original.


# ForgeRock Doc Maven Plugin Features

This section explains how to use key plugin features.

## Generating a Basic Documentation Project

The `forgerock-doc-maven-archetype` can be used 
to generate a basic documentation project.

To generate files for MyProject for example:

    mvn archetype:generate                                  \
      -DarchetypeRepository=http://maven.forgerock.org/repo \
      -DarchetypeGroupId=commons.forgerock.org              \
      -DarchetypeArtifactId=forgerock-doc-maven-archetype   \
      -DarchetypeVersion=3.0.0                              \
      -DgroupId=projectGroupId                              \
      -DartifactId=projectArtifactId                        \
      -DgoogleAnalyticsId=UA-xxxxxxxx-x                     \
      -Dname=MyProject                                      \
      -Dversion=1.0.0-SNAPSHOT

In general, after creating your new documentation set by using the archetype,
go through each of the XML files to determine what you must change.
Your documentation set might not even have an _Admin Guide_ or _Reference_,
though you can still use the files as templates.


## Using Variables in Documents

The `pre-site` phase `build` goal applies the Maven resource filtering.
 
This means you can use Maven properties to add variables to your documentation.

For example, say you have a property defined in your POM:

    <properties>
      <myUrl>http://docs.forgerock.org/</myUrl>
      ...
    </properties>

During the `pre-site` phase `build` goal,
the plugin replaces `${myUrl}`
in the build copy of the documents with `http://docs.forgerock.org`.

This allows you to use Maven properties in attribute values,
such as `xlink:href="${myURL}"`,
whereas the form using a processing instruction,
`xlink:href="<?eval ${myURL}"?>`,
is not valid XML.


## Building Release Documentation

The `site` phase `release` goal prepares documents for publication.

You might choose to include a release profile in your project.
Or you might choose to call the release goal by hand.

When you call the `release` goal, be sure to
set the appropriate dates, turn off draft mode, and override the Google Analytics ID
as shown in the following example.

     mvn \
     -DisDraftMode=no  \
     -D"gaId=UA-23412190-14" \
     -D"releaseDate=Software release date: January 1, 1970" \
     -D"pubDate=Publication date: December 31, 1969" \
     -DbuildReleaseZip=true \
     clean site org.forgerock.commons:forgerock-doc-maven-plugin:release

If the plugin configuration is not inherited,
then also set `-N` (`--non-recursive`) for the release goal.
Run the `site` goal separately if it must be recursive
(because you build Javadoc during the `site` goal for example).

Appropriate dates should be included in the documents to publish.
* The `releaseDate` indicates the date the software was released.
* The `pubDate` indicates the date you published the documentation.

To build a .zip of the release documentation,
you can set `-DbuildReleaseZip=true` as shown in the above example.
The file, `projectName-releaseVersion-docs.zip`,
can be found after the build in the project build directory.
When unzipped, it unpacks the documentation for the release
under `projectName/releaseVersion/`.


## Building Backstage Layout Documentation

The `backstage` goal allows you to generate docs in a layout
suitable for ForgeRock Backstage.

If the documentation set includes pre-built artifacts, such as Javadoc,
then you must specify those artifacts in the configuration.
For example:

     <artifactItems>
      <artifactItem>
       <groupId>org.forgerock.commons</groupId>
       <artifactId>forgerock-doc-maven-plugin</artifactId>
       <version>${project.version}</version>
       <classifier>javadoc</classifier>
       <outputDirectory>javadoc</outputDirectory>
       <title>ForgeRock Doc Maven Plugin Javadoc</title>
      </artifactItem>
     </artifactItems>

Backstage layout is as follows:

* `apidocs/` contains folders of any generated HTML-based documentation,
   such as Javadoc, that is not built from normal documentation sources,
   including a `meta.json` file inside each folder
   to specify the name of the document.
   The artifacts must be specified in the configuration.
   An example `meta.json` file looks like this:


    {
        "title": "OpenAM Javadoc"
    }

* `docbook/` contains the pre-processed DocBook XML sources
  suitable for formatting by a separate program.

* `docset.json` specifies meta information about the documentation set.
  For example:


    {
        "product": "OpenAM",
        "version": "12.0.0",
        "language": "en",
        "released": "2014-12-17"
    }

* `pdf/` contains PDF files corresponding to the DocBook XML sources,
  named as `<Product-from-docset-json>-<Version>-<Doc-name>.pdf`.

By default the product name is from `<productName>`.
You can however use `<backstageProductName>` to set a different product name:

     <backstageProductName>OpenAM Policy Agents</backstageProductName>


## Generating Single-Chapter Output

By default, the plugin generates output for each document
whose root is named `index.xml`.

You can change this by setting `documentSrcName` when you run Maven.

The following example produces pre-site output
only for a chapter named `chap-one.xml`.

    mvn -DdocumentSrcName=chap-one.xml clean pre-site


## Selecting Output Formats

Specify the type of outputs you want using `<formats>`.
On the command line, use `-Dformats` with a comma-separated list.

Supported formats include:
`bootstrap`, `epub`, `html`, `man`, `pdf`, `rtf`, `webhelp`, `xhtml5`.

If you do not specify any formats, the default output formats are
`bootstrap` and `pdf`.

Release builds only include HTML and `pdf`.

The following command generates only PDF output for your single chapter.

    mvn -DdocumentSrcName=chap-one.xml -Dformats=pdf clean pre-site

The following command generates only man pages and XHTML5 output.

    mvn -Dformats=man,xhtml5 clean pre-site


## Checking Links

By default, the plugin checks links found in the DocBook XML source,
including Olinks.

You can find errors in `${project.build.directory}/docbkx/linktester.err`
after the `site` phase `site` goal runs successfully.

To skip the checks for external URLs, pass `-DskipLinkCheck=true` to Maven.

    mvn -DskipLinkCheck=true clean site

To skip only some external URLs, use the `<skipUrlPatterns>` configuration list.

    <skipUrlPatterns>
     <skipUrlPattern>^http(s)?://my-host.*$</skipUrlPattern>
     <skipUrlPattern>^http(s)?://.*siroe.*$</skipUrlPattern>
    </skipUrlPatterns>

This capability is provided by Peter Major's
[linktester](https://github.com/aldaris/docbook-linktester) plugin.
For details on skipping URLs by pattern, see the documentation for his plugin.


## Handling PNG Images

Getting screenshots and other images to look okay in PDF can be a hassle.
The plugin therefore adjusts images to make large PNG images fit in the page,
and adjusts dots-per-inch on PNG images to make them look okay in print.

**Note: Do capture screenshots at 72 DPI. Retina displays can default to 144.**


## Using Syntax Highlighting in HTML

Uses [SyntaxHighlighter](http://alexgorbatchev.com/SyntaxHighlighter/),
rather than DocBook's syntax highlighting capabilities for HTML output.

The highlighting operates only inside `<programlisting>`.

     Source         SyntaxHighlighter   Brush Name
     ---            ---                 ---
     aci            aci                 shBrushAci.js
     csv            csv                 shBrushCsv.js
     html           html                shBrushXml.js
     http           http                shBrushHttp.js
     ini            ini                 shBrushProperties.js
     java           java                shBrushJava.js
     javascript     javascript          shBrushJScript.js
     ldif           ldif                shBrushLDIF.js
     none           plain               shBrushPlain.js
     shell          shell               shBrushBash.js
     xml            xml                 shBrushXml.js

Brush support for `aci`, `csv`, `http`, `ini`, and `ldif` is provided by
[a fork of SyntaxHighlighter](https://github.com/markcraig/SyntaxHighlighter).

To set the language for syntax highlighting use the `language` attribute
on the `<programlisting>` element, as in the following example:

    <programlisting language="java">
    class Test {
        public static void main(String [] args)  {
            System.out.println("This is a program listing.");
        }
    }
    </programlisting>

## Using Conditional Text

During the pre-processing phase the build plugin
makes it possible to use conditional text with
[DocBook profiling](http://www.sagehill.net/docbookxsl/Profiling.html) attributes.

In the source, set profiling attributes on elements to identify their profiles:

    <para os="linux">This is about Linux.</para>
    <para os="windows">This is about Windows.</para>
    <para condition="local-db">Include if local-db is delivered.</para>

In the plugin configuration, specify the inclusions and exclusions.
For example, to include the paragraphs about Linux and Windows,
and to exclude the local-db paragraph:

    <inclusions>
      <os>linux windows</os>
    </inclusions>
    <exclusions>
      <condition>local-db</condition>
    </exclusions>
    
Notice that multiple values for profile attributes are separated by spaces.
    
After pre-processing, the source looks like this:

    <para os="linux">This is about Linux.</para>
    <para os="windows">This is about Windows.</para>
    
For the list of supported profiling attributes,
see <http://www.sagehill.net/docbookxsl/Profiling.html#ProfilingAttribs>.


## Building Documentation from Pre-Processed Sources

When building output directly from pre-processed sources,
use the Maven dependency plugin to retrieve and unpack the sources
before you call this plugin.

      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-dependency-plugin</artifactId>
        <executions>
          <execution>
            <goals>
              <goal>unpack</goal>
            </goals>
            <phase>pre-site</phase>
            <configuration>
              <artifactItems>
                <artifactItem>
                  <groupId>${myGroupId}</groupId>
                  <artifactId>${myArtifactId}</artifactId>
                  <version>${myVersion}</version>
                  <classifier>doc-sources</classifier>
                  <outputDirectory>${project.build.directory}/db-src</outputDirectory>
                </artifactItem>
              </artifactItems>
            </configuration>
          </execution>
        </executions>
      </plugin>

If the DocBook XML sources have already been fully pre-processed,
set `<usePreProcessedSources>true</usePreProcessedSources>`
(or `-DusePreProcessedSources=true`)
and set `<docbkxSourceDirectory>` (or `-DdocbkxSourceDirectory`)
to the file system directory containing the pre-processed sources,
as in the following example that corresponds to the above settings.

      <usePreProcessedSources>true</usePreProcessedSources>
      <docbkxSourceDirectory>${project.build.directory}/db-src</docbkxSourceDirectory>


## Copying Arbitrary Documentation Set Resources

If the documentation set includes resource files such as large samples
that you link to, but that are not cited in the documentation,
those resource files are not automatically copied alongside the output.

Instead you must set `<copyResourceFiles>` to `true`.

The plugin expects to find the resources under `src/main/docbkx/resources`.
You can set this to another directory under `src/main/docbkx`
by using the `<resourcesDirectory>` setting.

These settings have an effect only for the `site` and `release` goals.
The plugin does not copy resources during the `pre-site` phase `build` goal.


## Citing Java Code

[JCite](http://arrenbrecht.ch/jcite/) lets you cite, rather than copy and paste,
Java source code in your documentation,
reducing the likelihood that the developer examples get out of sync
with your documentation.

Code citations should fit inside ProgramListing elements with language set
to `java` to pick up syntax highlighting. Use plain citations as in the
following example:

    <programlisting language="java"
    >[jcp:org.forgerock.doc.jcite.test.Test:--- mainMethod]</programlisting>

See the test project,
[forgerock-doc-maven-plugin-test](https://github.com/markcraig/forgerock-doc-maven-plugin-test)
for an example.


## Citing Text Files

The build now uses the `xcite-maven-plugin` (XCite)
to allow citations from any text file.

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

Although XCite does resolve citations recursively, it does not check for loops.
A loop occurs when a quote includes a citation quoting the original citation.

If you create a citation loop,
XCite processes it recursively until it runs out of memory.


## Resolving OLinks in Shared Content

You can now use `xlink:href="CURRENT.DOCID#linkend"` in your links.
The doc build plugin replaces `CURRENT.DOCID` with the current document name.


## Adding Hard Page Breaks in PDF and RTF

You can use the processing instruction `<?hard-pagebreak?>`
to force an unconditional page break in the PDF (and RTF) output.

This processing instruction cannot be used inline,
but instead must be used between block elements.


## Developing UML Diagrams

For UML diagrams, use PlantUML text files instead of images.

[PlantUML](http://plantuml.sourceforge.net/)
is an open source tool written in Java to create UML diagrams from text files.

Using UML instead of drawing images can be particularly useful
when constructing sequence diagrams.

The text files are rendered as PNG images where they are found,
so put your PlantUML `.txt` files in the `images/` directory for your book.
Then reference the `.png` version as if it existed already.

    <mediaobject xml:id="figure-my-sequence-diagram">
     <alt>Generated sequence diagram</alt>
     <imageobject>
      <imagedata fileref="images/my-sequence-diagram.png" format="PNG" />
     </imageobject>
     <textobject>
      <para>
       The sequence diagram is described in images/my-sequence-diagram.txt.
      </para>
     </textobject>
    </mediaobject>

Your PlantUML text files must have extension `.txt`.

To check your images during development, generate them with PlantUML by hand.

    java -jar plantuml.jar image.txt


# Alternative Branding & Common Content

By default, the ForgeRock doc Maven plugin
uses default ForgeRock branding
and pulls in common ForgeRock shared content.

You can use alternative branding and common content.


## Handling Branding

The plugin relies on a branding module that lets you configure alternatives
as in the following example, taken from the top-level plugin configuration.

If you want _different_ branding from the default,
you can use a different version,
or create your own Maven artifact, and include it in the configuration.

The following example shows the full configuration to use the 2.1.3 version.

    <configuration>
      ...
      <brandingGroupId>org.forgerock.commons</brandingGroupId>
      <brandingArtifactId>forgerock-doc-default-branding</brandingArtifactId>
      <brandingVersion>2.1.3</brandingVersion>
      ...
    </configuration>

If you need to create your own branding artifact,
see the `forgerock-doc-default-branding` project as an example.


## Handling Common Content

By default, the plugin replaces the following common files
in the build copy of the sources,
ensuring your documentation includes the latest versions.

    legal.xml
    shared/sec-accessing-doc-online.xml
    shared/sec-formatting-conventions.xml
    shared/sec-interface-stability.xml
    shared/sec-joining-the-community.xml
    shared/sec-release-levels.xml

The plugin does not replace your copies of the files in the original source.

The plugin does not check that you store the files in the expected location.

To avoid using ForgeRock common content, do not reference these files.

If you want _different_ common content from the default,
you can use a different version,
or create your own Maven artifact, and include it in the configuration.

The following example shows the full configuration to use the 2.1.3 version.

    <configuration>
      ...
      <commonContentGroupId>org.forgerock.commons</commonContentGroupId>
      <commonContentArtifactId>forgerock-doc-common-content</commonContentArtifactId>
      <commonContentVersion>2.1.3</commonContentVersion>
      ...
    </configuration>

If you need to create your own shared content artifact,
see the `forgerock-doc-common-content` project as an example.

* * *

The contents of this file are subject to the terms of the Common Development and
Distribution License (the License). You may not use this file except in compliance with the
License.

You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
specific language governing permission and limitations under the License.

When distributing Covered Software, include this CDDL Header Notice in each file and include
the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
Header, with the fields enclosed by brackets [] replaced by your own identifying
information: "Portions copyright [year] [name of copyright owner]".

Copyright 2012-2015 ForgeRock AS.
