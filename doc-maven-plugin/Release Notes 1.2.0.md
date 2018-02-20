# ForgeRock Documentation Tools 1.2.0 Release Notes

ForgeRock Documentation Tools is a catch all for the doc build plugin,
sites where we post documentation, and the documentation about
documentation. The link to the online issue tracker is
<https://bugster.forgerock.org/jira/browse/DOCS>.

This release includes the following improvements, new features, and bug
fixes.

<div style="background-color: #f8f8f8; border: 1px solid #ddd; margin: 6px; padding: 6px 10px;">
<p>
<b>Important</b>
</p>

<p>
To take advantage of the improvements like including common content and
JCite for code citations, you must let the doc build plugin use
generated doc sources for the build in a new build goal.
</p>

<pre>
    &lt;execution&gt;
     &lt;id&gt;copy-common&lt;/id&gt;
     &lt;phase&gt;pre-site&lt;/phase&gt;
     &lt;goals&gt;
      &lt;goal&gt;boilerplate&lt;/goal&gt;
     &lt;/goals&gt;
    &lt;/execution&gt;
</pre>

<p>
As a result you must also adjust any XIncludes referencing generated
content so that they reference the content in the proper relative
location.
</p>

<p>
The default directory for generated sources during the build is
`target/generated-docbkx`.
</p>
</div>


## Improvements & New Features

**DOCS-3: Add single-sourcing for code citations in Javadoc, core doc**

[JCite](http://arrenbrecht.ch/jcite/) lets you cite, rather than copy
and paste, Java source code in your documentation, reducing the
likelihood that the developer examples get out of sync with your
documentation.

To run JCite, add an execution like this prior to your pre-site build
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

Code citations should fit inside ProgramListing elements with language
set to `java` to pick up syntax highlighting. Use plain citations as in
the following example:

    <programlisting language="java"
    >[jcp:org.forgerock.doc.jcite.test.Test:--- mainMethod]</programlisting>

See the `forgerock-doc-maven-plugin-test` project for an example.

**DOCS-27: Document ForgeRock interface stability classification**

Definitions for ForgeRock release levels and interface stability are now
provided as sections in the shared content and also posted on the
Developer Wiki.

**DOCS-43: Include common content in the build plugin**

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

Be aware that the plugin merely replaces the files in the generated
sources, and then uses the generated sources to build the documentation.
Specifically the plugin does not check that you store the files in the
expected location.

It also does not check whether you got your executions out of order.
Make sure the `boilerplate` goal immediately precedes the `build` goal.

To avoid using common content, turn off the feature:

	<useSharedContent>false</useSharedContent> <!-- true by default -->

**DOCS-45: Improve visibility of admonitions in PDF**

This outlines admonitions (notes, tips, etc.) and sets a background
color to make the admonitions stand out from the surrounding text.

**DOCS-46: Indicate latest version available in published documentation**

This manual step has been done on docs.forgerock.org to existing docs.
Some JavaScript adds a small, diagonal banner at the upper right of the
page with a link to the latest version.

When you publish docs for a release, see
<https://github.com/markcraig/docs.forgerock.org#pointing-to-latest-docs>
for instructions on updating earlier docs.

**DOCS-52: Create guidance for users who want to contribute documentation**

Start with the ForgeRock developer wiki page,
<https://wikis.forgerock.org/confluence/display/devcom/Documentation>.

**DOCS-54: Upgrade docbook-linktester to pick up skipUrlPatterns feature**

This has `docbook-linktester` skip link checking for JIRA issue URLs,
and also for RFC URLs (as tools.ietf.org often seems slow to respond).

**DOCS-57: Add collapse/expand chapter in single-page HTML**

Until we have nice looking Webhelp, the doc build plugin adds the orange
[+]/[-] icons that we also use to expand/collapse screen listings to let
you click next to chapter, reference, appendix, preface, glossary,
index, and book titles in the HTML to expand or to collapse the content.


## Bugs Fixed

**DOCS-39: PDF layout needs some love**

The fixes remove bizarre hyphenation, and add hyphenation at '.' for
long literal elements, such as properties names.

**DOCS-55: Link test should run only after all generated documentation has been unpacked**

The doc build plugin now runs docbook-linktester in the site phase, when
the content is definitely in place and all XIncludes should resolve
properly.

**DOCS-69: Linktester encounters error writing output file**

**DOCS-70: [-]/[+] causes titles to be indented**


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

**DOCS-75: Wide programlisting shading extends to the right edge of the page in PDF**

When `<programlisting>` content is shaded and the `<programlisting>` width
is set to make it page-wide, then the background shading extends into
the right margin to the edge of the page.

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
