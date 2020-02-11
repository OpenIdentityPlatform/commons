# PRE-RELEASE DRAFT

# ForgeRock Documentation Tools 3.1.0 Release Notes

ForgeRock Documentation Tools is a catch all for the doc build artifacts,
sites where we post release documentation,
and the documentation about documentation.

The link to the online issue tracker is
<https://bugster.forgerock.org/jira/browse/DOCS>.

## Compatibility

This release does not introduce any incompatible changes.


## Improvements & New Features

**DOCS-318: Skip I-D Urls by default when checking links**

**DOCS-300: The copyResources flag should take effect during the pre-site phase**

**DOCS-292: Keep short `<programlisting>` and `<screen>` on same page**

**DOCS-241: Add support for Asciidoc**

Asciidoc books can now be added alongside DocBook XML books.
The Asciidoc format books are converted to DocBook XML during pre-processing.
The current implementation does not support mixing
Asciidoc and DocBook XML sources for the same book.


## Bugs Fixed

**DOCS-308: The backstage goal leaves an extra docbkx-sources directory under docbook**

**DOCS-290: Man pages not generated if project directory name contains a space**


## Known Issues

This section lists the main known issues at the time of release.

**DOCS-270: Robot-indexed links to in-progress docs lose their fragments**

**DOCS-269: Webhelp does not include a link for reporting doc bugs**

**DOCS-268: Strange pdf output when using bridgeheads**

**DOCS-236: Webhelp stylesheets incorrectly resolve `<xref>` links**

This issue occurs in one case that we know of.

**DOCS-230: Bad line breaks in the middle of literals**

Workaround: Check the PDF before you publish.

**DOCS-226: XPointer resolution fails in XIncluded document**

Workaround: Create a separate file to Xinclude containing the desired block element.

**DOCS-190: PDF: release date and publication date are not shown**

**DOCS-163: The performance="optional" attr in a step has no effect**

**DOCS-132: Soft hyphens used to break lines are rendered in PDF as hyphen + space**

Although soft hyphens are not used in this release,
the line break for hyphenation still remains.

See <https://issues.apache.org/jira/browse/FOP-2358>.

Workaround: Fix the content after copy/paste.


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

Copyright 2015 ForgeRock AS.
