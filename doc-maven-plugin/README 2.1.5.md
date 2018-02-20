# ForgeRock Doc Build Maven Plugin

This Maven plugin centralizes configuration of core documentation, to ensure
that documents are formatted uniformly.

_This document covers functionality present in 2.1.5._

With centralized configuration handled by this Maven plugin, the core
documentation-related project configuration takes at least three arguments:

*   `<projectName>`: the short name for the project such as OpenAM, OpenDJ,
    OpenICF, OpenIDM, OpenIG, and so forth
*   `<projectVersion>`: the version, such as `1.0.0-SNAPSHOT`, or `3.3.1`
*   `<googleAnalyticsId>`: to add Google Analytics JavaScript to HTML output

The project runs multiple plugin executions:

1.  A `boilerplate` goal to copy common content
2.  A `filter` goal for Maven resource filtering on source files
3.  An optional `jcite` goal to cite Java source files
4.  A `prepare` goal to prepare sources for the build
5.  A `build` goal in the `pre-site` phase to build and massage output
6.  A `layout` goal in the `site` phase to copy content under `site/doc`
7.  A `release` goal to prepare site documentation for release

## Table of Contents

* [Example Plugin Specification](#Example_Plugin_Specification)
* [Source Layout Requirements](#Source_Layout_Requirements)
* [Resolving Maven Properties](#Resolving_Maven_Properties)
* [Using Shared Content](#Using_Shared_Content)
* [PNG Image Manipulation](#PNG_Image_Manipulation)
* [Pre-processing Sources Only](#Pre-processing_Sources_Only)
* [Link Checking](#Link_Checking)
* [Excluding Output Formats](#Excluding_Output_Formats)
* [Generating Single-Chapter Output](#Generating_Single-Chapter_Output)
* [Generating Only One Format](#Generating_Only_One_Format)
* [Alternate Branding](#Alternate_Branding)
* [Expected Results](#Expected_Results)
* [Release Layout](#Release_Layout)
* [Hard Page Breaks in PDF and RTF](#Hard_Page_Breaks_in_PDF_and_RTF)
* [Zip of Release Documentation](#Zip_of_Release_Documentation)
* [Notes on Syntax Highlighting](#Notes_on_Syntax_Highlighting)
* [JCite Integration](#JCite_Integration)
* [Links in Shared Content](#Links_in_Shared_Content)


## Example Plugin Specification

You call the plugin from your `pom.xml` as follows. This example uses a
POM property called `gaId`, whose value is the Google Analytics ID.

        <build>
         <plugins>
          <plugin>
           <groupId>org.forgerock.commons</groupId>
           <artifactId>forgerock-doc-maven-plugin</artifactId>
           <version>${frDocPluginVersion}</version>
           <inherited>false</inherited>
           <configuration>
            <projectName>MyProject</projectName>
            <projectVersion>1.0.0-SNAPSHOT</projectVersion>
            <googleAnalyticsId>${gaId}</googleAnalyticsId>
           </configuration>
           <executions>
            <execution>
             <id>copy-common</id>
             <phase>pre-site</phase>
             <goals>
              <goal>boilerplate</goal>
             </goals>
            </execution>
            <execution>
             <id>filter-sources</id>
             <phase>pre-site</phase>
             <goals>
              <goal>filter</goal>
             </goals>
            </execution>
            <execution>
             <id>run-jcite</id>
             <phase>pre-site</phase>
             <goals>
              <goal>jcite</goal>
             </goals>
            </execution>
            <execution>
             <id>prepare-sources</id>
             <phase>pre-site</phase>
             <goals>
              <goal>prepare</goal>
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
             <id>layout-doc</id>
             <phase>site</phase>
             <goals>
              <goal>layout</goal>
             </goals>
            </execution>
           </executions>
          </plugin>
         </plugins>
        </build>

## Source Layout Requirements

The assumption is that all of your DocBook XML documents are found under
`src/main/docbkx/` relative to the `pom.xml` file in which you call the
plugin. Documents are expected to be found in folders under that path, where
the folder name is a lowercase version of the document name, such as
release-notes, install-guide, admin-guide, reference, or similar. Furthermore,
all documents have the same file name for the file containing the top-level
document element, by default `index.xml`. The plugin expects to find all
images in an `images` folder inside the document folder.

An example project layout looks like this:

     src/main/docbkx/
      legal.xml
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

## Resolving Maven Properties

The `pre-site` goal of `filter` uses the Maven resources plugin to replace
Maven properties like `${myProperty}` with their values. This allows you to
use Maven properties in attribute value, such as `xlink:href="${myURL}"`
where the form using a processing instruction `xlink:href="<?eval ${myURL}"?>`
is not valid XML.

The execution of the `filter` goal must be the first in the list of `pre-site`
phase goals for this plugin as shown in the example above.

## Using Shared Content

By default the plugin replaces the following common files at build time,
ensuring your documentation includes the latest versions.

    legal.xml
    shared/sec-accessing-doc-online.xml
    shared/sec-formatting-conventions.xml
    shared/sec-interface-stability.xml
    shared/sec-joining-the-community.xml
    shared/sec-release-levels.xml

The plugin does not replace your copies of the files. Instead it copies
common files to the expected locations in the generated sources.

Be aware that the plugin merely replaces the files in the generated sources,
and then uses the generated sources to build the documentation. Specifically
the plugin does not check that you store the files in the expected location.

It also does not check whether you got your executions out of order. Make sure
the `boilerplate` goal immediately precedes the `build` goal.

To avoid using common content, turn off the feature:

    <useSharedContent>false</useSharedContent> <!-- true by default -->

If you want different shared content from the default,
you can use a different version,
or create your own Maven module for shared content,
and include it in the configuration
for the `<goal>boilerplate</goal>` execution.

The following example shows the configuration to use the 1.0.0 version.

     <execution>
      <id>copy-common</id>
      <phase>pre-site</phase>
      <configuration>
       <commonContentGroupId>org.forgerock.commons</commonContentGroupId>
       <commonContentArtifactId>forgerock-doc-common-content</commonContentArtifactId>
       <commonContentVersion>1.0.0</commonContentVersion>
      </configuration>
      <goals>
       <goal>boilerplate</goal>
      </goals>
     </execution>

## PNG Image Manipulation

Getting screenshots and other images to look okay in PDF can be a hassle.
The plugin therefore adjusts the XML to make large PNG images fit in the page,
and adjusts dots-per-inch on PNG images to make them look okay in print.

**Note: Do capture screenshots at 72 DPI. Retina displays can default to 144.**

To take advantage of this feature, you must include a new `pre-site` goal
after the sources have been pre-processed:

    <execution>
     <id>prepare-sources</id>
     <phase>pre-site</phase>
     <goals>
      <goal>prepare</goal>
     </goals>
    </execution>

Furthermore, you can provide PlantUML text files instead of images.
[PlantUML](http://plantuml.sourceforge.net/)
is an open source tool written in Java to create UML diagrams from text files.
Using UML instead of drawing images can be particularly useful
when constructing complicated sequence diagrams.

The text files are rendered as PNG images where they are found,
so put your PlantUML `.txt` files in the `images/` directory for your book.
Then reference the `.png` version as if it existed already.

    <mediaobject xml:id="figure-openid-connect-basic">
     <alt>Generated sequence diagram</alt>
     <imageobject>
      <imagedata fileref="images/openid-connect-basic.png" format="PNG" />
     </imageobject>
     <textobject>
      <para>
       The sequence diagram is described in images/openid-connect-basic.txt.
      </para>
     </textobject>
    </mediaobject>

Your PlantUML text files must have extension `.txt`.

While creating images, you can generate them with PlantUML by hand.

    java -jar plantuml.jar image.txt


## Pre-processing Sources Only

When you set `<stopAfterPreProcessing>` to `true`,
the build stops once DocBook XML requires no further pre-processing.
The plugin logs a message indicating where to find the pre-processed files:

    [INFO] Pre-processed sources are available under ...


## Link Checking

By default, the plugin checks links found in the DocBook XML source, including
Olinks. You can find errors in the `target/docbkx/linktester.err` file.

If you want to skip the checks for external URLs, pass `-DskipLinkCheck=true`
to Maven, as in the following example:

    mvn -DskipLinkCheck=true clean site

The check is run at the end of the site layout phase. This capability is
provided by Peter Major's [linktester](https://github.com/aldaris/docbook-linktester)
plugin.

## Excluding Output Formats

To exclude formats from the build, you can use the optional
`<excludes>` configuration element. The following example
excludes all formats but HTML from the build.

     <excludes>
      <exclude>epub</exclude>
      <exclude>man</exclude>
      <exclude>pdf</exclude>
      <exclude>rtf</exclude>
      <exclude>webhelp</exclude>
     </excludes>

## Generating Single-Chapter Output

By default, the plugin generates output for each document whose root is named
`index.xml`. You can change this by setting `documentSrcName` when you run
Maven. For example, if you want to produce pre-site output only for a chapter
named `chap-one.xml`, then you would set `documentSrcName` as follows.

    mvn -DdocumentSrcName=chap-one.xml clean pre-site

## Generating Only One Format

If you want only one type of output, then specify that using `include`.
The following command generates only PDF output for your single chapter.

    mvn -DdocumentSrcName=chap-one.xml -Dinclude=pdf clean pre-site

Formats include `epub`, `html`, `man`, `pdf`, `rtf`, and `webhelp`.

## Alternate Branding

The plugin uses a branding module that lets you configure alternatives
as in the following example, taken from the top-level plugin configuration.

     <brandingGroupId>org.forgerock.commons</brandingGroupId>
     <brandingArtifactId>forgerock-doc-default-branding</brandingArtifactId>
     <brandingVersion>1.0.1</brandingVersion>

If you need to create your own branding,
consider the `forgerock-doc-default-branding` module as an example.

## Expected Results

When you run the plugin with `mvn pre-site`, it builds the output formats,
which you find under `target/docbkx`.

When you run the plugin with `mvn site`, it takes what was constructed during
the `pre-site` phase and moves it under `target/site/doc` as expected for a
Maven project site. The plugin adds an `index.html` in that directory that
redirects to `http://project.forgerock.org/docs.html`, so you do need one of
those in your Maven site. The plugin also runs the link check.

The plugin also adds a `.htaccess` file under `target/site/doc` indicating to
Apache HTTPD server to compress text files like HTML and CSS.
If the server is configured to ignore `.htaccess`,
consult with the server administrator to update server settings as necessary.

## Release Layout

You can call the `release` goal in the site phase to prepare a doc layout
for release on docs.forgerock.org. When you call the release goal, be sure to
turn off draft mode, add a release version, and override the Google Analytics
ID using the property.

     mvn -DisDraftMode=no -DreleaseVersion=1.0.0 -D"gaId=UA-23412190-14" \
     -D"releaseDate=Software release date: January 1, 1970" \
     -D"pubDate=Publication date: December 31, 1969" \
     -DbuildReleaseZip=true \
     clean site org.forgerock.commons:forgerock-doc-maven-plugin:release

Both dates are reflected in the documents to publish.
* The `releaseDate` indicates the date the software was released.
* The `pubDate` indicates the date you published the documentation.

If the plugin configuration is not inherited,
then also set `-N` (`--non-recursive`) for the release goal.
Run the `site` goal separately if it must be recursive
(because you build Javadoc during the `site` goal for example).

## Hard Page Breaks in PDF and RTF

You can now use the processing instruction `<?hard-pagebreak?>`
to force an unconditional page break in the PDF (and RTF) output.

This processing instruction cannot be used inline,
but instead must be used between block elements.

## Zip of Release Documentation

To build a .zip of the release documentation, you can further set
`-DbuildReleaseZip=true` when running the `release` goal on the command line,
or `<buildReleaseZip>true</buildReleaseZip>` in the execution configuration.
Also set `-DprojectName=MyProject` if you perform the `release` goal separately.

The file, `projectName-releaseVersion-docs.zip`, can be found
after the build in the project build directory. When unzipped, it unpacks
the documentation for the release under `projectName/releaseVersion/`.

At present this builds a .zip only of the release documents
for the current module.
In other words, only HTML and PDF output,
and only corresponding to the DocBook XML sources built in the current module.
As a result, if your documentation set requires documents from multiple modules,
you must still build the final release .zip yourself.

## Notes on Syntax Highlighting

Uses [SyntaxHighlighter](http://alexgorbatchev.com/SyntaxHighlighter/),
rather than DocBook's syntax highlighting capabilities for HTML output, as
SyntaxHighlighter includes handy features for selecting and numbering lines
in HTML.

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

## JCite Integration

[JCite](http://arrenbrecht.ch/jcite/) lets you cite, rather than copy and paste,
Java source code in your documentation, reducing the likelihood that the
developer examples get out of sync with your documentation.

To run JCite, add an execution like this prior to your pre-site `boilerplate`
goal:

    <execution>
     <id>run-jcite</id>
     <phase>pre-site</phase>
     <goals>
      <goal>jcite</goal>
     </goals>
    </execution>

Also make sure that your build uses the sources processed by JCite:

    <useGeneratedSources>true</useGeneratedSources> <!-- true by default -->

Code citations should fit inside ProgramListing elements with language set
to `java` to pick up syntax highlighting. Use plain citations as in the
following example:

    <programlisting language="java"
    >[jcp:org.forgerock.doc.jcite.test.Test:--- mainMethod]</programlisting>

See the `forgerock-doc-maven-plugin-test` project for an example.

## Links in Shared Content

You can now use `xlink:href="CURRENT.DOCID#linkend"` in your links.
The doc build plugin replaces `CURRENT.DOCID` with the current document name.


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

Copyright 2012-2014 ForgeRock AS
