# ForgeRock Documentation Tools 2.1.5 Release Notes

ForgeRock Documentation Tools is a catch all for the doc build artifacts,
sites where we post release documentation,
and the documentation about documentation.

The link to the online issue tracker is
<https://bugster.forgerock.org/jira/browse/DOCS>.

## Compatibility

This release introduces a new configuration setting with the fix for DOCS-215.

It does not introduce any incompatible changes.


## Improvements & New Features

**DOCS-215: Add configuration for stopping at pre-processed DocBook**

This improvement introduces a boolean configuration parameter,
`<stopAfterPreProcessing>` (default: `false`).

When `<stopAfterPreProcessing>` is set to `true`,
the build stops when DocBook XML requires no further pre-processing.
The plugin logs a message indicating where to find the pre-processed files:

    [INFO] Pre-processed sources are available under ...


## Bugs Fixed

**DOCS-213: Linktester phase in doc build plugin fails to detect broken links**

**DOCS-200: US phone number for ForgeRock has changed**

**DOCS-206: Webhelp in-progress docs are not clearly marked DRAFT**

**DOCS-171: Make Secondary Sub-Headings More Prominent**


## Known Issues

**DOCS-224: Webhelp format; sections from one document appear temporarily as subsections in a second document**

**DOCS-220: Chapter numbers should be included in WebHelp format**

**DOCS-190: PDF: release date and publication date are not shown**

**DOCS-163: The performance="optional" attr in a step has no effect**

**DOCS-150: When a code sample is unwrapped, it it not limited to the width of the page**

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

Copyright 2014 ForgeRock AS
