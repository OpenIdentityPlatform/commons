# ForgeRock Documentation Tools 2.1.3 Release Notes

ForgeRock Documentation Tools is a catch all for the doc build artifacts,
sites where we post release documentation,
and the documentation about documentation.

The link to the online issue tracker is
<https://bugster.forgerock.org/jira/browse/DOCS>.

This release brings the following changes,
and has the following known issues.

## Compatibility

This maintenance release does not introduce configuration changes since 2.1.2.

You need only to update the plugin version.


## Improvements & New Features

**DOCS-72: Improve widow and orphan control in PDF**

You can now use the processing instruction `<?hard-pagebreak?>`
to force an unconditional page break in the PDF output.

This processing instruction cannot be used inline,
but instead must be used between block elements.


## Bugs Fixed

**DOCS-162: `<replaceable>` tags within `<screen>` tags have no effect in the HTML**

**DOCS-173: Link text too dark in top-right banner showing latest release**


## Known Issues

**DOCS-132: Soft hyphens used to break lines are rendered in PDF as hyphen + space**

Although soft hyphens are not used in this release,
the line break for hyphenation still remains.

See <https://issues.apache.org/jira/browse/FOP-2358>.

Workaround: Fix the content after copy/paste.

**DOCS-150: When a code sample is unwrapped, it it not limited to the width of the page**

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
