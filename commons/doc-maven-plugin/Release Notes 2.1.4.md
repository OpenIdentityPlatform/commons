# ForgeRock Documentation Tools 2.1.4 Release Notes

ForgeRock Documentation Tools is a catch all for the doc build artifacts,
sites where we post release documentation,
and the documentation about documentation.

The link to the online issue tracker is
<https://bugster.forgerock.org/jira/browse/DOCS>.

## Compatibility

This release introduces a new configuration setting with the fix for DOCS-194.

It does not introduce any incompatible changes.


## Improvements & New Features

**DOCS-194: Add option to allow merge of generated and docbkx source directories**

You can now use the Maven configuration setting
`<overwriteGeneratedSource>true</overwriteGeneratedSource>`
together the `boilerplate` goal to overwrite files
from `docbkxSourceDirectory` to `docbkxGeneratedSourceDirectory`.

The default value, `false`, maintains compatibility with earlier versions.


## Bugs Fixed

**DOCS-197: Prepending ↪ on mouseover is distracting**

**DOCS-59: Only the draft documents are optimized to appear in Google searches; the final docs need to be SEO too.**

The fix adds a robots meta tag (noindex, nofollow) to site HTML,
that it then removes from release HTML.


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
