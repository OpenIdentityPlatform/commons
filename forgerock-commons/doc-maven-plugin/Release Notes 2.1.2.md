# ForgeRock Documentation Tools 2.1.2 Release Notes

ForgeRock Documentation Tools is a catch all for the doc build plugin,
sites where we post documentation, and the documentation about
documentation. The link to the online issue tracker is
<https://bugster.forgerock.org/jira/browse/DOCS>.

This release includes improvements & new features, and bug fixes.

## Compatibility

This maintenance release does not introduce configuration changes since 2.1.0.

You need only to update the plugin version.


## Improvements & New Features

**DOCS-28: Improve the cover page for PDF documents**

PDF cover pages now have their own style.
The style is meant to include a logo and the corporate author address.

**DOCS-146: Fix chapter headings styles on PDF**

**DOCS-164: Update common content to mention command-line output may be formatted**

**DOCS-165: Add a mechanism to set the current document ID in olinks**

The `filter` goal now includes the capability
to replace `CURRENT.DOCID` placeholders in `xlink:href` attribute values
with the current document name.

In other words, in a source file `my-book/my-chapter.xml`,
`xlink:href="CURRENT.DOCID#target-id"` becomes `xlink:href="my-book#target-id"`.


## Bugs Fixed

**DOCS-71: Soft hyphens displayed in mid line in PDF**

**DOCS-151: Improve Figure Caption Font (PDF)**

**DOCS-152: Fix Sub-Heading Horizontal Spacing (PDF)**

**DOCS-153: Doc build leaves a stray target.db file**

**DOCS-155: Add Author Names on Verso Page of New PDF Cover (PDF)**

In fact author names remain on the recto cover page.

**DOCS-156: Configuration of docbkx-plugin is cluttered with repeat elements**

**DOCS-157: Split long URLs in command-line examples (PDF)**

**DOCS-158: Fix Default PDF Page Margins**

**DOCS-159: Add Spacing to Procedure Headings (PDF)**

**DOCS-170: Syntax highlighting missing for program listings in release docs**


## Known Issues

**DOCS-132: Soft hyphens used to break lines are rendered in PDF as hyphen + space**

Although soft hyphens are not used in this release,
the line break for hyphenation still remains.

See <https://issues.apache.org/jira/browse/FOP-2358>.

Workaround: Fix the content after copy/paste.

**DOCS-150: When a code sample is unwrapped, it it not limited to the width of the page**

**DOCS-162: `<replaceable>` tags within `<screen>` tags have no effect in the HTML**

**DOCS-163: The performance="optional" attr in a step has no effect**


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

Copyright 2014 ForgeRock AS
