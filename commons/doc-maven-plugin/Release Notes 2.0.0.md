# ForgeRock Documentation Tools 2.0.0 Release Notes

ForgeRock Documentation Tools is a catch all for the doc build plugin,
sites where we post documentation, and the documentation about
documentation. The link to the online issue tracker is
<https://bugster.forgerock.org/jira/browse/DOCS>.

This release includes the following improvements, new features, and bug
fixes.

## Improvements & New Features

**DOCS-35: Integrate support for text-based sequence diagram sources**

Note that the version of PlantUML available for Maven
at the time of this writing is 7940.
Some PlantUML features, such as `<->` in sequence diagrams,
require a more recent version of PlantUML.
So you might want to continue to build some PNGs by hand.

`forgerock-doc-maven-plugin` now lets you provide PlantUML text files
instead of images. [PlantUML](http://plantuml.sourceforge.net/) is an
open source tool written in Java to create UML diagrams from text
specifications.

To take advantage of this feature, you must include a new `pre-site` goal
after the sources have been pre-processed:

    <execution>
     <id>prepare-sources</id>
     <phase>pre-site</phase>
     <goals>
      <goal>prepare</goal>
     </goals>
    </execution>

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

If you are having problems getting the doc build plugin to generate
`.png` files from your `.txt` files, have you made sure that PlantUML
can convert them?

Try `java -jar plantuml.jar file.txt` to make sure generation works fine.

**DOCS-47: Support for olinks in pdf**

External olinks in PDF depend on relative locations of other PDF files.
As a result, if you only download one PDF,
or if you move the files relative to each other,
then the external olinks break.

**DOCS-51: Automatically set DPI appropriately on raster graphics**

The fix affects only PNG files. It requires a new `pre-site` goal after the
sources have been pre-processed:

    <execution>
     <id>prepare-sources</id>
     <phase>pre-site</phase>
     <goals>
      <goal>prepare</goal>
     </goals>
    </execution>

**DOCS-65: Move to mojo-executor 2.1.0 to take advantage of support for dependencies**

**DOCS-78: Include hyphen when splitting URL across lines**

If you have a `<link xlink:href="http://some/url">http://some/url</link>`,
write it `<link xlink:href="http://some/url" />` instead.

**DOCS-85: Make it easy to get the link to other titled block elements**

Added ↪ on mouseover in HTML for all titles with anchors.

**DOCS-86: Leave more space between table cells in PDF**

**DOCS-87: Allow soft hyphens at commas in long literals**

This is aimed to allow hyphenation of LDAP DNs,
such as `uid=bjensen,ou=People,dc=example,dc=com`.

**DOCS-88: Style more width in HTML table rendering of simplelist**

When you use a `<simplelist>` with multiple columns,
the stylesheets render the list as a table.
This change spreads the label across the page.

**DOCS-89: Simplify use of Maven properties in XML attribute values**

The fix requires that you insert a new execution `pre-site` goal
after the goal to copy common content:

    <execution>
        <id>filter-sources</id>
        <phase>pre-site</phase>
        <goals>
            <goal>filter</goal>
        </goals>
    </execution>

**DOCS-100: Decouple/isolate branding from the build plugin to make it easier to generate unbranded docs**

The plugin now uses a branding module that lets you configure alternatives
as in the following example, taken from the top-level plugin configuration.

     <brandingGroupId>org.forgerock.commons</brandingGroupId>
     <brandingArtifactId>forgerock-doc-default-branding</brandingArtifactId>
     <brandingVersion>1.0.1</brandingVersion>

If you need to create your own branding,
consider the `forgerock-doc-default-branding` module as an example.

**DOCS-102: Consider moving common content into a separate module**

The plugin now uses a common content module that lets you configure alternatives
as in the following example, showing the `boilerplate` execution configuration.

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

**DOCS-105: Color link text in printable formats**

Link text is now blue.

**DOCS-107: Make release documentation transportable**

To build a .zip of the released documentation, you can set
`-DbuildReleaseZip=true` when running the release goal on the command line,
or `<buildReleaseZip>true</buildReleaseZip>` in the execution configuration.

The file, `projectName-releaseVersion-docs.zip`, can be found
after the build in the project build directory. When unzipped, it unpacks
the documentation for the release under `projectName/releaseVersion/`.

At present this builds a .zip only of the release documents
for the current module.
In other words, only HTML and PDF output,
and only corresponding to the DocBook XML sources built in the current module.
As a result, if your documentation set requires documents from multiple modules,
you must still build the final release .zip yourself.


## Bugs Fixed

**DOCS-75: Wide programlisting shading extends to the right edge of the page in PDF**

The fix helps, but for page-wide listings, use this suggestion from Bob Stayton:

    <informalexample>
    <?dbfo pgwide="1"?>
    <programlisting>Wide listing that needs full-page width ...</programlisting>
    </informalexample>

**DOCS-91: Doc build plugin fails when project names include numbers**

**DOCS-92: Doc build plugin NPE when source directory contains no directories**

**DOCS-93: Doc build plugin does not properly use configuration settings**

This lets you use a different `${project.build.directory}` than `target`.

**DOCS-94: Version numbers on draft docs are confusing**

**DOCS-95: Do not set publication date on in-progress documentation**

Having a publication date on in-progress documentation can be confusing.
Publication date should be set only on release documentation.

This is fixed in the doc build plugin, but requires changes to the product POM:

      <!--
        Release date and publication date are set at release build time.
          -D"releaseDate=Software release date: January 1, 1970"
          -D"pubDate=Publication date: December 31, 1969"
        At all other times, the dates should be empty.
      -->
      <releaseDate />
      <softwareReleaseDate>${releaseDate}</softwareReleaseDate>
      <pubDate />
      <publicationDate>${pubDate}</publicationDate>

And to the top level document files, such as `index.xml`:

      <date>${publicationDate}</date>
      <pubdate>${publicationDate}</pubdate>
      <releaseinfo>${softwareReleaseDate}</releaseinfo>

Once these are changed in the product docs and this version of the doc build
plugin is used, the publication date only appears in output when set.

**DOCS-109: CC image not included in HTML legal notice page**

**DOCS-110: Verbatim text renders too large in Opera**

**DOCS-114: Table layout in HTML results in large tables being hard to read**

**DOCS-121: Maven resource filtering should happen after common content is copied**

## Known Issues

**DOCS-71: Soft hyphens displayed in mid line in PDF**

See <https://issues.apache.org/jira/browse/FOP-2239>.

Workaround: The problem might arise when you are documenting a synopsis
manually, as the markup is not available in the context where you want
to add a synopsis.

First, you can use `&#8230;` for horizontal ellipsis rather than `...`.

Second, if you have a construction like `.]` where brackets mean
optional, then add an extra space. It's technically wrong, but readers
will have to interpret the optional characters anyway.

**DOCS-76: Cannot copy/paste examples from PDF**

`<screen>` content is formatted for readability, but without backslashes
before newlines, so cannot be copy/pasted directly from the PDF.

Workaround: Access the HTML version, click on the [-] icon to flatten
the formatted example, and then copy the resulting content. (not
accessible)

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
