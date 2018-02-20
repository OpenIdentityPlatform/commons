# ForgeRock Doc Build Maven Plugin Design

The ForgeRock Doc Build Maven Plugin implementation grew organically
with little thought to any overall design.

Initially the plugin merely replaced POM-based configuration
with a uniform mechanism to apply across ForgeRock projects.
The organization followed the structure of the POM-based configuration.

Later additional features were added to the existing structure.

The haphazard arrangement of code made the implementation
difficult to learn and to maintain.

A more tractable implementation follows an easy-to-understand design,
one that resembles a pipeline having the following stages.

## Pre-Processing Source Files

The first stage pre-processes DocBook XML source files.

First, this stage prepares for processing.

*  Unpack the branding elements. (`Branding`)
*  Make a modifiable copy of the original sources. (`ModifiableCopy`)
*  Apply profiling to the modifiable copy of the DocBook XML sources. (`ConditionalText`)

By default, pre-processing tasks are performed in the following order:

*  Augment the copy with common content. (`CommonContent`)
*  Process Asciidoc sources to DocBook XML. (`AsciidocToDocBook`)
*  Include Java code by applying the JCite plugin. (`JCite`)
*  Include quotes from other text files by applying the XCite plugin. (`XCite`)
*  Perform Maven resource filtering on the copy to replace variables. (`Filter`)
*  Edit `<imagedata>` elements in the resource filtered files. (`ImageData`)
*  Add color to `<thead><row>` in the resource filtered files. (`HeaderColor`)
*  Perform image generation on the resource filtered files. (`PlantUml`)
*  For PDF and RTF, add PIs to avoid page breaks in short listings. (`KeepTogether`)
*  Set DPI on .png images in the resource filtered files. (`Dpi`)
*  Perform additional pre-processing on the resource filtered files. (`CurrentDocId`)
*  Add custom CSS to the sources. (`CustomCss`)

*  If configured, build a Maven artifact from pre-processed sources. (`ArtifactBuilder`)

This stage is performed during the `pre-site` phase, `process` goal. (`PreProcessMojo`)

## Processing Sources to Generate Output

The next stage generates output formats.

Like the pre-processing stage it also unpacks the branding elements
in case the build operates on pre-processed files. (`Branding`)

When preparing to generate PDF or RTF,
this stage starts by preparing fonts for use with Apache FOP. (`Fop`)

A build class named by output format encapsulates generation of each format,
including olink generation:

*  Chunked HTML (`ChunkedHtml`)
*  EPUB (`Epub`)
*  Man pages (`Manpage`)
*  PDF (`Pdf`, which is a wrapper for `Fo`)
*  RTF (`Rtf`, which is a wrapper for `Fo`)
*  Single-page HTML (`SingleHtml`)
*  Webhelp (`Webhelp`)
*  XHTML5 (`Xhtml5`)

This stage is performed during the `pre-site` phase, `build` goal. (`PreSiteMojo`)

## Post-Processing Generated Output

The next stage post-processes generated output,
clearly separating post-processing from output generation.

Some formats such as HTML require fairly extensive post-processing
to include JavaScript, change CSS, and so forth.
Most formats currently require no post-processing.

*  HTML post-processing (`Html`)
*  Man page post-processing (`ManpagePost`)
*  No post-processing (`NoOp`)
*  Webhelp post-processing (`WebhelpPost`)
*  XHTML post-processing (`Xhtml`)

This stage is performed during the `pre-site` phase, `build` goal. (`PreSiteMojo`)

## Copying Arbitrary Resources to Generated Output

If the documents include arbitrary resources possibly referenced in source files,
the plugin copies those to HTML (and similar) output folders. (`ArbitraryResourceCopier`)

## Copying Output to a Site Layout

The next stage copies output to the site directory,
following the standard layout for core documentation.

Documents show up under `${project.build.directory}/site/doc`.

This stage also includes link testing,
which is to avoid running potentially lengthy link tests in the `pre-site` phase.

*  Lay out built docs (`Layout`)
*  Add `.htaccess` for Apache HTTP Server (`Htaccess`)
*  Redirect `/doc/index.html` to `docs.html` (`Redirect`)
*  Link test (`LinkTest`)

This stage is performed during the `site` phase, `site` goal. (`SiteMojo`)

## Copying Output to a Release Layout

The release stage copies output to a release directory.

*  Lay out release docs (`Layout`)
*  Add an index.html to release docs (`IndexHtml`)
*  Rename PDFs in release docs to include the version number (`PdfNames`)
*  Fix favicon links in release HTML (`Favicon`)
*  Replace CSS in release HTML (`Css`)
*  Replace robots meta tag in release HTML (`Robots`)
*  Zip release docs (`Zip`)

This stage is performed during the `site` phase, `release` goal. (`ReleaseMojo`)

## Preparing Output for

The Backstage stage prepares a layout suitable as input to Backstage.

* Unpack pre-built doc artifacts to the Backstage layout (`ArtifactDocs`)
* Copy pre-processed sources to the Backstage layout (`BackstageMojo`)
* Write a `docset.json` file for the Backstage layout (`BackstageMojo`)
* Copy PDFs to the Backstage layout (`Pdf`)

This stage is performed during the `site` phase, `backstage` goal.


## Configuration

Configuration parameters and closely related methods
are handled through the `AbstractDocbkxMojo` class.

The other Mojo classes inherit from this abstract class.


## About the Java Packages

*  `org.forgerock.doc.maven`: top-level classes for configuration & overall operation
*  `org.forgerock.doc.maven.backstage`: classes for preparing Backstage layout
*  `org.forgerock.doc.maven.build`: classes for building output formats
*  `org.forgerock.doc.maven.post`: post-processing classes
*  `org.forgerock.doc.maven.pre`: pre-processing classes
*  `org.forgerock.doc.maven.release`: classes for preparing the release documents
*  `org.forgerock.doc.maven.site`: classes for preparing the project site
*  `org.forgerock.doc.maven.utils`: utility classes


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

Copyright 2014-2015 ForgeRock AS.
