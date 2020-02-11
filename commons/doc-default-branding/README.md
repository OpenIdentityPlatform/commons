# ForgeRock Doc Default Branding Module

This branding module acts as a customization layer on top of the default
Docbook XSL stylesheets, and can be used to modify the look and feel
of the output documentation.

ForgeRock-customized stylesheets are provided in
`main/resources/docbkx-stylesheets` for the following output
targets:

 * bootstrap
 * epub
 * fo (PDF)
 * html
 * man
 * webhelp
 * xhtml5

For more information on Docbook customization layers, see:
[Customization methods](http://www.sagehill.net/docbookxsl/CustomMethods.html)

## Upgrading Underlying Docbook Stylesheets

The XSL customizations in this module override the transformations present in
the default Docbook XSL files.

If the default Docbook XSL stylesheets change, you may need to update the
customizations in this module to take advantage of those changes in the output.

Updating an XSL customization generally involves:

1. Locate the default Docbook XSL stylesheet that transforms the tag you want
   to customize.

2. Make a copy of the XSL file in the customization layer.

3. Apply the changes you want to make to the copy of the XSL file.

4. Remove any `xsl:template` blocks from the copy of the XSL file that have not
   been modified. The build process will fall-back to the content of the
   default Docbook XSL stylesheets if there is not an applicable template in
   the customization layer.

5. Ensure the copy of the XML file is included in the root XSL file in the
   customization layer for the target you are modifying, for example:
   `docbkx-stylesheets/html/coredoc.xsl`.

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
