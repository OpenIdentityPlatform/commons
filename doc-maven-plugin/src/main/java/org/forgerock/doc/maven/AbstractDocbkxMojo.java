/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2012-2015 ForgeRock AS.
 */

package org.forgerock.doc.maven;

import static org.twdata.maven.mojoexecutor.MojoExecutor.element;
import static org.twdata.maven.mojoexecutor.MojoExecutor.name;

import org.apache.commons.io.FilenameUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.forgerock.doc.maven.backstage.ArtifactItem;
import org.forgerock.doc.maven.utils.NameUtils;
import org.twdata.maven.mojoexecutor.MojoExecutor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * AbstractMojo implementation for building core documentation from <a
 * href="http://www.docbook.org/tdg51/en/html/docbook.html">DocBook XML</a>
 * using <a href="http://code.google.com/p/docbkx-tools/">docbkx-tools</a>.
 */
abstract public class AbstractDocbkxMojo extends AbstractMojo {

    /**
     * Versions of plugins driven by this plugin.
     */
    private Properties versions;

    /**
     * Load versions of plugins driven by this plugin.
     */
    private void loadVersions() {
        versions = new Properties();
        InputStream inputStream = null;

        try {
            inputStream = getClass().getResourceAsStream("/versions.properties");
            if (inputStream == null) {
                throw new IOException("Could not read properties resource");
            }
            versions.load(inputStream);
        } catch (IOException e) {
            getLog().error("Failed to read plugin version properties", e);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    // Ignore exception.
                }
            }
        }
    }

    /**
     * Get a version property based on the version properties file.
     *
     * <br>
     *
     * Prefer the current value if it is already set.
     *
     * @param currentValue  If not null or empty, then return this value.
     * @param key           Otherwise return the value for this key.
     * @return              The currentValue if set, otherwise the property.
     */
    private String getVersionProperty(String currentValue, String key) {
        if (currentValue != null && !currentValue.isEmpty()) {
            return currentValue;
        }

        if (versions == null) {
            loadVersions();
        }

        return versions.getProperty(key);
    }

    /**
     * Whether to use WinAnsi encoding for embedded fonts.
     * If {@code true}, then some UTF-8 characters cannot be used.
     */
    private String ansi = "false";

    /**
     * Whether to use WinAnsi encoding for embedded fonts.
     *
     * <br>
     *
     * Value: {@code false}
     *
     * @return Whether WinAnsi encoded should be used for embedded fonts.
     */
    public final String useAnsi() {
        return ansi;
    }

    /**
     * Should sections have numeric labels?
     *
     * <br>
     *
     * docbkx-tools element: &lt;sectionAutolabel&gt;
     */
    private String areSectionsAutolabeled = "true";

    /**
     * Whether sections should have numeric labels.
     *
     * <br>
     *
     * Value: {@code true}
     *
     * <br>
     *
     * docbkx-tools element: &lt;sectionAutolabel&gt;
     *
     * @return Whether sections should have numeric labels.
     */
    public final String areSectionsAutolabeled() {
        return areSectionsAutolabeled;
    }

    /**
     * Base directory for Asciidoc source files.
     */
    @Parameter(defaultValue = "${basedir}/src/main/asciidoc")
    private File asciidocSourceDirectory;

    /**
     * Get the base directory for Asciidoc source files.
     * These files remain unchanged during processing.
     *
     * <br>
     *
     * Default: {@code ${basedir}/src/main/asciidoc}.
     *
     * @return The base directory for Asciidoc source files.
     */
    public File getAsciidocSourceDirectory() {
        return asciidocSourceDirectory;
    }

    /**
     * Version of the Asciidoctor Maven plugin to use.
     */
    @Parameter
    private String asciidoctorPluginVersion;

    /**
     * Returns the version of the Asciidoctor Maven plugin to use.
     *
     * @return The version of the Asciidoctor Maven plugin to use.
     */
    public String getAsciidoctorPluginVersion() {
        return getVersionProperty(asciidoctorPluginVersion, "asciidoctorPluginVersion");
    }

    /**
     * File system directory for Backstage layout output
     * relative to the build directory.
     */
    @Parameter(defaultValue = "backstage")
    private String backstageDirectory;

    /**
     * Get the file system directory for Backstage layout output.
     *
     * <br>
     *
     * Default: {@code ${project.build.directory}/backstage}
     *
     * @return The file system directory for Backstage layout output.
     */
    public File getBackstageDirectory() {
        return new File(getBuildDirectory(), backstageDirectory);
    }

    /**
     * Product name as shown on Backstage.
     */
    @Parameter(property = "backstageProductName")
    private String backstageProductName;

    /**
     * Get the product name as shown on Backstage.
     *
     * <br>
     *
     * Default: {@code projectName}
     *
     * @return The product name as shown on Backstage.
     */
    public String getBackstageProductName() {
        return backstageProductName != null ? backstageProductName : projectName;
    }

    /**
     * Get the base configuration applicable to all builds with the docbkx-tools plugin.
     *
     * @return The configuration applicable to all builds.
     */
    public ArrayList<MojoExecutor.Element> getBaseConfiguration() {
        ArrayList<MojoExecutor.Element> cfg = new ArrayList<>();

        cfg.add(element(name("draftMode"), isDraftMode()));
        cfg.add(element(name("draftWatermarkImage"), getDraftWatermarkURL()));
        cfg.add(element(name("highlightSource"), useSyntaxHighlighting()));
        cfg.add(element(name("sectionAutolabel"), areSectionsAutolabeled()));
        cfg.add(element(name("sectionLabelIncludesComponentLabel"),
                doesSectionLabelIncludeComponentLabel()));
        cfg.add(element(name("xincludeSupported"), isXincludeSupported()));
        cfg.add(element(name("sourceDirectory"), path(getDocbkxModifiableSourcesDirectory())));

        return cfg;
    }

    /**
     * Project base directory, needed to workaround bugs with *target.db and webhelp.
     */
    @Parameter(defaultValue = "${basedir}")
    private File baseDir;

    /**
     * Project base directory, needed to workaround bugs with *target.db and webhelp.
     *
     * <br>
     *
     * Default: {@code ${basedir}}
     *
     * @return The project base directory.
     */
    public File getBaseDir() {
        return baseDir;
    }

    /**
     * The artifactId of the branding to use.
     */
    @Parameter(defaultValue = "forgerock-doc-default-branding")
    private String brandingArtifactId;

    /**
     * Gets the branding artifactId to use.
     *
     * <br>
     *
     * Default: {@code forgerock-doc-default-branding}.
     *
     * @return The branding artifactId.
     */
    public String getBrandingArtifactId() {
        return brandingArtifactId;
    }

    /**
     * The groupId of the branding to use.
     */
    @Parameter(defaultValue = "org.forgerock.commons")
    private String brandingGroupId;

    /**
     * Gets the groupId of the branding artifact to use.
     *
     * <br>
     *
     * Default: {@code org.forgerock.commons}
     *
     * @return The branding groupId.
     */
    public String getBrandingGroupId() {
        return brandingGroupId;
    }

    /**
     * Version of the branding artifact to use.
     */
    @Parameter
    private String brandingVersion;

    /**
     * Gets the version of the branding artifact to use.
     *
     * @return The branding artifact version.
     */
    public String getBrandingVersion() {
        return getVersionProperty(brandingVersion, "brandingVersion");
    }

    /**
     * The project build directory.
     *
     * <br>
     *
     * Default: {@code ${project.build.directory}}.
     */
    @Parameter(defaultValue = "${project.build.directory}")
    private File buildDirectory;

    /**
     * Get the project build directory for this plugin.
     *
     * <br>
     *
     * Default: {@code ${project.build.directory}}.
     *
     * @return The build directory.
     */
    public File getBuildDirectory() {
        return buildDirectory;
    }

    /**
     * Whether to build a .zip of the release content.
     */
    @Parameter(defaultValue = "false", property = "buildReleaseZip")
    private boolean buildReleaseZip;

    /**
     * Whether to build a .zip containing the release content.
     *
     * <br>
     *
     * Default: {@code false}
     *
     * @return true if the .zip should be built.
     */
    public final boolean doBuildReleaseZip() {
        return buildReleaseZip;
    }

    /**
     * Location of the chunked HTML XSL stylesheet customization file,
     * relative to the build directory.
     *
     * <br>
     *
     * docbkx-tools element: &lt;htmlCustomization&gt;
     */
    private String chunkedHTMLCustomization = "docbkx-stylesheets/html/chunked.xsl";

    /**
     * Get the location of the chunked HTML XSL stylesheet customization file.
     *
     * <br>
     *
     * Default: {@code ${project.build.directory}/docbkx-stylesheets/html/chunked.xsl}
     *
     * <br>
     *
     * docbkx-tools element: &lt;htmlCustomization&gt;
     *
     * @return The location of the chunked HTML XSL stylesheet.
     */
    public final File getChunkedHTMLCustomization() {
        return new File(getBuildDirectory(), chunkedHTMLCustomization);
    }

    /**
     * The {@code artifactId} of the common content artifact.
     */
    @Parameter(defaultValue = "forgerock-doc-common-content")
    private String commonContentArtifactId;

    /**
     * Get the {@code artifactId} of the common content artifact.
     *
     * <br>
     *
     * Default: {@code forgerock-doc-common-content}.
     *
     * @return The {@code artifactId} of the common content artifact.
     */
    public String getCommonContentArtifactId() {
        return commonContentArtifactId;
    }

    /**
     * The {@code groupId} of the common content artifact.
     */
    @Parameter(defaultValue = "org.forgerock.commons")
    private String commonContentGroupId;

    /**
     * Get the {@code groupId} of the common content artifact.
     *
     * <br>
     *
     * Default: {@code org.forgerock.commons}.
     *
     * @return The {@code groupId} of the common content artifact.
     */
    public String getCommonContentGroupId() {
        return commonContentGroupId;
    }

    /**
     * Version of the common content artifact to use.
     */
    @Parameter
    private String commonContentVersion;

    /**
     * Get the version of the common content artifact to use.
     *
     * @return the version of the common content artifact to use.
     */
    public String getCommonContentVersion() {
        return getVersionProperty(commonContentVersion, "commonContentVersion");
    }

    /**
     * Whether to copy resource files alongside docs for site, release.
     */
    @Parameter(defaultValue = "false")
    private boolean copyResourceFiles;

    /**
     * Whether to copy resource files alongside docs for site, release.
     *
     * <br>
     *
     * Default: false
     *
     * @return true if resource files should be copied.
     */
    public boolean doCopyResourceFiles() {
        return copyResourceFiles;
    }

    /**
     * Whether to build artifacts from pre-processed sources.
     */
    @Parameter(defaultValue = "true")
    private boolean createArtifacts;

    /**
     * Whether to build artifacts from pre-processed sources.
     *
     * <br>
     *
     * Default: true
     *
     * @return true if artifacts should be build from pre-processed sources.
     */
    public boolean doCreateArtifacts() {
        return createArtifacts;
    }

    /**
     * Doc artifacts to unpack when preparing Backstage layout.
     */
    @Parameter
    private List<ArtifactItem> artifactItems;

    /**
     * Get the doc artifacts to unpack when preparing Backstage layout.
     *
     * @return The doc artifacts to unpack when preparing Backstage layout.
     */
    public List<ArtifactItem> getArtifactItems() {
        return artifactItems != null ? artifactItems : new LinkedList<ArtifactItem>();
    }

    /**
     * Base directory for the modifiable copy of DocBook XML source files,
     * relative to the build directory.
     */
    private String docbkxModifiableSourcesDirectory = "docbkx-sources";

    /**
     * Get the base directory for the modifiable copy of DocBook XML source files.
     * This copy is modified during preparation for processing.
     *
     * <br>
     *
     * Value: {@code ${project.build.directory}/docbkx-sources}
     *
     * @return The base directory for the modifiable copy of DocBook XML source files.
     */
    public File getDocbkxModifiableSourcesDirectory() {
        return new File(getBuildDirectory(), docbkxModifiableSourcesDirectory);
    }

    /**
     * Base directory for built documentation, relative to the build directory.
     */
    private String docbkxOutputDirectory = "docbkx";

    /**
     * Base directory for built documentation.
     *
     * <br>
     *
     * Value: {@code ${project.build.directory}/docbkx}
     *
     * @return The base directory for built documentation.
     */
    public File getDocbkxOutputDirectory() {
        return new File(buildDirectory, docbkxOutputDirectory);
    }

    /**
     * Base directory for DocBook XML source files.
     */
    @Parameter(defaultValue = "${basedir}/src/main/docbkx", property = "docbkxSourceDirectory")
    private File docbkxSourceDirectory;

    /**
     * Get the base directory for DocBook XML source files.
     * These files remain unchanged during processing.
     *
     * <br>
     *
     * Default: {@code ${basedir}/src/main/docbkx}.
     *
     * @return The base directory for DocBook XML source files.
     */
    public File getDocbkxSourceDirectory() {
        return docbkxSourceDirectory;
    }

    /**
     * Docbkx Tools plugin version to use.
     */
    @Parameter
    private String docbkxVersion;

    /**
     * Get the docbkx-tools plugin version to use.
     *
     * @return The docbkx-tools plugin version to use
     */
    public String getDocbkxVersion() {
        return getVersionProperty(docbkxVersion, "docbkxVersion");
    }

    /**
     * Supported DocBook profile attributes.
     */
    public enum ProfileAttributes {
        /** Computer or chip architecture, such as i386. */
        arch,

        /** Intended audience of the content, such as instructor. Added in DocBook version 5.0. */
        audience,

        /** General purpose conditional attribute, with no preassigned semantics. */
        condition,

        /** Standards conformance, such as lsb (Linux Standards Base). */
        conformance,

        /** Language code, such as de_DE. */
        lang,

        /** Operating system. */
        os,

        /** Editorial revision, such as v2.1. */
        revision,

        /** Revision status of the element, such as changed. This attribute has a fixed set of values to choose from. */
        revisionflag,

        /** General purpose attribute, with no preassigned semantics. Use with caution for profiling. */
        role,

        /** Security level, such as high. */
        security,

        /** Editorial or publication status, such as InDevelopment or draft. */
        status,

        /** Level of user experience, such as beginner. */
        userlevel,

        /** Product vendor, such as apache. */
        vendor,

        /** Word size (width in bits) of the computer architecture, such as 64bit. Added in DocBook version 4.4. */
        wordsize
    }

    /**
     * Returns true if the attribute is one of the expected ProfileAttributes.
     * @param attribute     The attribute to check.
     * @return true if the attribute is one of the expected ProfileAttributes.
     */
    private boolean isProfileAttribute(final String attribute) {
        try {
            ProfileAttributes.valueOf(attribute);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Include elements with these
     * <a href="http://www.sagehill.net/docbookxsl/Profiling.html">DocBook profile</a> settings.
     * <br>
     * See <a href="http://www.sagehill.net/docbookxsl/Profiling.html#ProfilingAttribs"
     * >the list of profile attributes specified for DocBook</a>.
     * <br>
     * Separate multiple attribute values for the same attribute with spaces.
     * <br>
     * For example, to include all elements with {@code os="linux"} or {@code os="unix"}
     * and all elements with no {@code os} attribute,
     * use the following configuration:
     * <pre>
     * &lt;inclusions>
     *  &lt;os>linux unix&lt;/os>
     * &lt;/inclusions>
     * </pre>
     */
    @Parameter
    private Map<String, String> inclusions;

    /**
     * Returns a map of DocBook profile settings to include elements.
     * <br>
     * This implementation ignores unexpected profile settings.
     *
     * @return A map of DocBook profile settings to include elements,
     *         or null if none are set.
     */
    public Map<String, String> getInclusions() {
        if (inclusions == null) {
            return null;
        }

        Map<String, String> result = new HashMap<>();
        for (String attribute : inclusions.keySet()) {
            if (isProfileAttribute(attribute)) {
                result.put(attribute, inclusions.get(attribute));
            }
        }
        return result;
    }

    /**
     * Exclude elements with these
     * <a href="http://www.sagehill.net/docbookxsl/Profiling.html">DocBook profile</a> settings.
     * <br>
     * See <a href="http://www.sagehill.net/docbookxsl/Profiling.html#ProfilingAttribs"
     * >the list of profile attributes specified for DocBook</a>.
     * <br>
     * Separate multiple attribute values for the same attribute with spaces.
     * <br>
     * For example, to exclude all elements with {@code os="linux"} and {@code os="unix"},
     * use the following configuration:
     * <pre>
     * &lt;exclusions>
     *  &lt;os>linux unix&lt;/os>
     * &lt;/exclusions>
     * </pre>
     */
    @Parameter
    private Map<String, String> exclusions;

    /**
     * Returns a map of DocBook profile settings to exclude elements.
     * <br>
     * This implementation ignores unexpected profile settings.
     *
     * @return A map of DocBook profile settings to exclude elements,
     *         or null if none are set.
     */
    public Map<String, String> getExclusions() {
        if (exclusions == null) {
            return null;
        }

        Map<String, String> result = new HashMap<>();
        for (String attribute : exclusions.keySet()) {
            if (isProfileAttribute(attribute)) {
                result.put(attribute, exclusions.get(attribute));
            }
        }
        return result;
    }

    /**
     * Get document names for the current project.
     *
     * @return The document names for the current project.
     * @throws MojoExecutionException No document names found.
     */
    public Set<String> getDocNames() throws MojoExecutionException {

        Set<String> docNames = NameUtils.getDocumentNames(
                getDocbkxModifiableSourcesDirectory(), getDocumentSrcName());

        if (docNames.isEmpty()) {
            throw new MojoExecutionException("No document names found.");
        }
        return docNames;
    }

    /**
     * URL to site for published documentation.
     */
    @Parameter(defaultValue = "http://docs.forgerock.org/")
    private String docsSite;

    /**
     * Get the URL to the site for published documentation.
     *
     * <br>
     *
     * Default: {@code http://docs.forgerock.org/}
     *
     * @return The URL to the site for published documentation.
     */
    public String getDocsSite() {
        return docsSite;
    }

    /**
     * Top-level DocBook XML source document name.
     */
    @Parameter(defaultValue = "index.xml", property = "documentSrcName")
    private String documentSrcName;

    /**
     * Get the top-level DocBook XML source document name.
     *
     * <br>
     *
     * Default: {@code index.xml}.
     *
     * <br>
     *
     * Documents included in the documentation set
     * such as books, articles, and references share a common entry point,
     * which is a file having the name specified by this element.
     *
     * <br>
     *
     * For example, if your documentation set has
     * Release Notes, an Installation Guide, an Admin Guide, a Dev Guide, and a Reference,
     * your source layout under the base DocBook XML source directory
     * might look like the following:
     *
     * <pre>
     * src/main/docbkx/
     *  admin-guide/
     *   index.xml
     *   ...other files...
     *  dev-guide/
     *   index.xml
     *   ...other files...
     *  install-guide/
     *   index.xml
     *   ...other files...
     *  reference/
     *   index.xml
     *   ...other files...
     *  release-notes/
     *   index.xml
     *   ...other files...
     *  shared/
     *   ...other files...
     * </pre>
     *
     * <br>
     *
     * The {@code ...other files...} can have whatever names you want,
     * as long as the name does not match the file name you configure.
     * For example, if you were to hand-code an index file
     * you could name it {@code ix.xml}.
     *
     * @return File name of top-level DocBook XML source document.
     */
    public String getDocumentSrcName() {
        return documentSrcName;
    }

    /**
     * Whether section labels should include parent numbers,
     * like 1.1, 1.2, 1.2.1, 1.2.2.
     *
     * <br>
     *
     * docbkx-tools element: &lt;sectionLabelIncludesComponentLabel&gt;
     */
    private String doesSectionLabelIncludeComponentLabel = "true";

    /**
     * Whether section labels should include parent numbers,
     * like 1.1, 1.2, 1.2.1, 1.2.2.
     *
     * <br>
     *
     * Value: {@code true}
     *
     * <br>
     *
     * docbkx-tools element: &lt;sectionLabelIncludesComponentLabel&gt;
     *
     * @return Whether section labels should include parent numbers.
     */
    public final String doesSectionLabelIncludeComponentLabel() {
        return doesSectionLabelIncludeComponentLabel;
    }

    /**
     * For draft mode, URL to the background watermark image.
     *
     * <br>
     *
     * docbkx-tools element: &lt;draftWatermarkImage&gt;
     */
    @Parameter(defaultValue = "http://docbook.sourceforge.net/release/images/draft.png")
    private String draftWatermarkURL;

    /**
     * For draft mode, URL to the background watermark image.
     *
     * <br>
     *
     * Default: {@code http://docbook.sourceforge.net/release/images/draft.png}
     *
     * <br>
     *
     * docbkx-tools element: &lt;draftWatermarkImage&gt;
     *
     * @return The URL to the background watermark image.
     */
    public final String getDraftWatermarkURL() {
        return draftWatermarkURL;
    }

    /**
     * URL to JSON object showing EOSL versions for each project.
     */
    @Parameter(defaultValue = "http://docs.forgerock.org/eosl.json")
    private String eoslJson;

    /**
     * Get the URL to JSON object showing EOSL versions for each project.
     *
     * @return The URL to the JSON object.
     */
    public String getEoslJson() {
        return eoslJson;
    }

    /**
     * Location of the EPUB XSL stylesheet customization file,
     * relative to the build directory.
     *
     * <br>
     *
     * docbkx-tools element: &lt;epubCustomization&gt;
     */
    private String epubCustomization = "docbkx-stylesheets/epub/coredoc.xsl";

    /**
     * Get the location of the EPUB XSL stylesheet customization file.
     *
     * <br>
     *
     * Default: {@code ${project.build.directory}/docbkx-stylesheets/epub/coredoc.xsl}
     *
     * <br>
     *
     * docbkx-tools element: &lt;epubCustomization&gt;
     *
     * @return The location of the EPUB XSL stylesheet customization file.
     */
    public final File getEpubCustomization() {
        return new File(getBuildDirectory(), epubCustomization);
    }

    /**
     * Favicon link element for the pre-site version of the HTML.
     */
    @Parameter(defaultValue = "<link rel=\"shortcut icon\" href=\"http://forgerock.org/favicon.ico\">")
    private String faviconLink;

    /**
     * Get the favicon link element for the pre-site version of the HTML.
     *
     * @return The link element.
     */
    public final String getFaviconLink() {
        return faviconLink;
    }

    /**
     * Location of the FO XSL stylesheet customization file (for PDF, RTF),
     * relative to the build directory.
     *
     * <br>
     *
     * docbkx-tools element: &lt;foCustomization&gt;
     */
    private String foCustomization = "docbkx-stylesheets/fo/coredoc.xsl";

    /**
     * Get the location of the FO XSL stylesheet customization file (for PDF, RTF).
     *
     * <br>
     *
     * Value: {@code ${project.build.directory}/docbkx-stylesheets/fo/coredoc.xsl}
     *
     * <br>
     *
     * docbkx-tools element: &lt;foCustomization&gt;
     *
     * @return The location of the FO XSL stylesheet.
     */
    public final File getFoCustomization() {
        return new File(getBuildDirectory(), foCustomization);
    }

    /**
     * Directory where fonts and font metrics are stored,
     * relative to the build directory.
     */
    private String fontsDirectory  = "fonts";

    /**
     * Directory where fonts and font metrics are stored.
     *
     * <br>
     *
     * Value: {@code ${project.build.directory}/fonts}
     *
     * @return The directory where fonts and font metrics are stored.
     */
    public final File getFontsDirectory() {
        return new File(getBuildDirectory(), fontsDirectory);
    }

    /**
     * Version of the FOP hyphenation plugin to use.
     */
    @Parameter
    private String fopHyphVersion;

    /**
     * Get the version of the FOP hyphenation plugin to use.
     * @return The version of the FOP hyphenation plugin to use.
     */
    public String getFopHyphVersion() {
        return getVersionProperty(fopHyphVersion, "fopHyphVersion");
    }

    /**
     * Log level when building FO output (PDF, RTF).
     *
     * <br>
     *
     * docbkx-tools element: &lt;fopLogLevel&gt;
     */
    @Parameter(defaultValue = "ERROR")
    private String fopLogLevel;

    /**
     * Log level when building FO output (PDF, RTF).
     *
     * <br>
     *
     * Default: {@code ERROR}
     *
     * <br>
     *
     * docbkx-tools element: &lt;fopLogLevel&gt;
     *
     * @return The log level for Apache FOP.
     */
    public final String getFopLogLevel() {
        return fopLogLevel;
    }

    /**
     * Supported output formats.
     */
    public enum Format {
        /**
         * EPUB v2 without styling; not ready for publication.
         */
        epub,

        /**
         * Styled single-page and chunked HTML 4.
         */
        html,

        /**
         * Styled with HtmlForBootstrap single-page HTML 4.
         */
        bootstrap,

        /**
         * Reference manual pages for use with the {@code man} command.
         */
        man,

        /**
         * PDF.
         */
        pdf,

        /**
         * RTF without styling; not ready for publication.
         */
        rtf,

        /**
         * Styled DocBook Webhelp format.
         */
        webhelp,

        /**
         * Single-page XHTML5 without styling except syntax highlighting;
         * not ready for publication as is.
         */
        xhtml5
    }

    /**
     * Comma-separated list of output formats to generate.
     */
    @Parameter(property = "formats", defaultValue = "bootstrap,pdf")
    private List<Format> formats;

    /**
     * Return a list of output formats to generate.
     *
     * <br>
     *
     * Default: bootstrap,pdf
     *
     * @return List of output formats.
     */
    public List<Format> getFormats() {
        return this.formats;
    }

    /**
     * Google Analytics identifier for the project.
     *
     * <br>
     *
     * The identifier for docs.forgerock.org is {@code UA-23412190-14}.
     */
    @Parameter(defaultValue = "UA-23412190-14")
    private String googleAnalyticsId;

    /**
     * Google Analytics identifier for the project.
     *
     * <br>
     *
     * Default: {@code UA-23412190-14}
     *
     * @return The Google Analytics identifier.
     */
    public String getGoogleAnalyticsId() {
        return googleAnalyticsId;
    }

    /**
     * Whether these are draft documents, rather than final documents.
     *
     * <br>
     *
     * docbkx-tools element: &lt;draftMode&gt;
     */
    @Parameter(defaultValue = "yes", property = "isDraftMode")
    private String isDraftMode;

    /**
     * Whether these are draft documents, rather than final documents.
     *
     * <br>
     *
     * Default: {@code yes}
     *
     * <br>
     *
     * docbkx-tools element: &lt;draftMode&gt;
     *
     * @return Whether these are draft documents.
     */
    public final String isDraftMode() {
        return isDraftMode;
    }

    /**
     * Whether documents should be allowed to include other documents.
     *
     * <br>
     *
     * docbkx-tools element: &lt;xincludeSupported&gt;
     */
    private String isXincludeSupported = "true";

    /**
     * Whether documents should be allowed to include other documents.
     *
     * <br>
     *
     * Value: {@code true}
     *
     * <br>
     *
     * docbkx-tools element: &lt;xincludeSupported&gt;
     *
     * @return Where documents should be allowed to include other documents.
     */
    public final String isXincludeSupported() {
        return isXincludeSupported;
    }

    /**
     * JavaScript file name, found under {@code /js/} in plugin resources.
     */
    private String javaScriptFileName = "uses-jquery.js";

    /**
     * Get the main JavaScript file name, found under {@code /js/} in plugin resources.
     *
     * <br>
     *
     * Value: {@code uses-jquery.js}
     *
     * @return The JavaScript file name.
     */
    public String getJavaScriptFileName() {
        return javaScriptFileName;
    }

    /**
     * The set of source paths where cited Java files are found.
     */
    @Parameter
    private List<File> jCiteSourcePaths;

    /**
     * Get the source paths where cited Java files are found.
     *
     * <br>
     *
     * If source paths are not set, {@code src/main/java} is used.
     *
     * @return the set of source paths where cited Java files are found.
     */
    public List<File> getJCiteSourcePaths() {
        return jCiteSourcePaths;
    }

    /**
     * JCite version to use for code citations.
     */
    @Parameter
    private String jCiteVersion;

    /**
     * Get the JCite artifact version to use for Java code citations.
     *
     * @return The JCite artifact version to use for Java code citations.
     */
    public String getJCiteVersion() {
        return getVersionProperty(jCiteVersion, "jCiteVersion");
    }

    /**
     * Whether to keep a custom index.html file for the documentation set.
     */
    @Parameter(defaultValue = "false")
    private boolean keepCustomIndexHtml;

    /**
     * Whether to keep a custom index.html file for the documentation set.
     *
     * <br>
     *
     * Default: {@code false}
     *
     * @return Whether to keep a custom index.html file.
     */
    public boolean keepCustomIndexHtml() {
        return keepCustomIndexHtml;
    }

    /**
     * URL to JSON object showing latest versions for each project.
     */
    @Parameter(defaultValue = "http://docs.forgerock.org/latest.json")
    private String latestJson;

    /**
     * Get the URL to JSON object showing latest versions for each project.
     *
     * @return The URL to the JSON object.
     */
    public String getLatestJson() {
        return latestJson;
    }

    /**
     * ForgeRock link tester plugin version to use.
     */
    @Parameter
    private String linkTesterVersion;

    /**
     * ForgeRock link tester plugin version to use.
     *
     * @return The link tester plugin version to use.
     */
    public String getLinkTesterVersion() {
        return getVersionProperty(linkTesterVersion, "linkTesterVersion");
    }

    /**
     * Locale tag for the documentation set.
     */
    @Parameter(defaultValue = "en")
    private String localeTag;

    /**
     * Get the Locale tag for the documentation set.
     *
     * <br>
     *
     * Default: {@code en}
     *
     * @return The Locale tag for the documentation set.
     */
    public String getLocaleTag() {
        return localeTag;
    }

    /**
     * Location of the man page XSL stylesheet customization file,
     * relative to the build directory.
     *
     * <br>
     *
     * docbkx-tools element: &lt;manpagesCustomization&gt;
     */
    private String manpagesCustomization = "docbkx-stylesheets/man/coredoc.xsl";

    /**
     * Get the location of the man page XSL stylesheet customization file.
     *
     * <br>
     *
     * Value: {@code ${project.build.directory}/docbkx-stylesheets/man/coredoc.xsl}
     *
     * <br>
     *
     * docbkx-tools element: &lt;manpagesCustomization&gt;
     *
     * @return The location of the man page XSL stylesheet.
     */
    public final File getManpagesCustomization() {
        return new File(getBuildDirectory(), manpagesCustomization);
    }

    /**
     * Version of the Maven assembly plugin to use.
     */
    @Parameter
    private String mavenAssemblyVersion;

    /**
     * Get the version of the Maven dependency plugin to use.
     * @return The version of the Maven dependency plugin to use.
     */
    public String getMavenAssemblyVersion() {
        return getVersionProperty(mavenAssemblyVersion, "mavenAssemblyVersion");
    }

    /**
     * Version of the Maven dependency plugin to use.
     */
    @Parameter
    private String mavenDependencyVersion;

    /**
     * Get the version of the Maven dependency plugin to use.
     * @return The version of the Maven dependency plugin to use.
     */
    public String getMavenDependencyVersion() {
        return getVersionProperty(mavenDependencyVersion, "mavenDependencyVersion");
    }

    /**
     * Version of the Maven filtering library to use.
     */
    @Parameter
    private String mavenFilteringVersion;

    /**
     * Get the version of the Maven filtering library to use.
     * <br>
     * This is used as a workaround for
     * <a href="https://jira.codehaus.org/browse/MSHARED-325">MSHARED-325</a>.
     *
     * @return The version of the Maven filtering library to use.
     */
    public String getMavenFilteringVersion() {
        return getVersionProperty(mavenFilteringVersion, "mavenFilteringVersion");
    }

    /**
     * Maven resources plugin version.
     * Executions seem to hit an NPE when the version is not specified.
     */
    @Parameter
    private String mavenResourcesVersion;

    /**
     * Get the Maven resources plugin version.
     * Executions seem to hit an NPE when the version is not specified.
     *
     * @return The Maven resources plugin version.
     */
    public String getMavenResourcesVersion() {
        return getVersionProperty(mavenResourcesVersion, "mavenResourcesVersion");
    }

    /**
     * Maximum height for PNG images used in PDF, in inches.
     */
    @Parameter(defaultValue = "5")
    private int maxImageHeightInInches;

    /**
     * Get maximum height for PNG images used in PDF, in inches.
     *
     * @return Maximum height for PNG images used in PDF, in inches.
     */
    public int getMaxImageHeightInInches() {
        return maxImageHeightInInches;
    }

    /**
     * Overwrite the copy of DocBook sources if it exists.
     */
    @Parameter(defaultValue = "true")
    private boolean overwriteModifiableCopy;

    /**
     * Whether to overwrite the copy of DocBook sources if it exists.
     *
     * <br>
     *
     * One of the first things the plugin does when preparing DocBook sources
     * is to make a working copy of the files that is separate from the sources.
     * This allows the plugin to make changes to the files as necessary.
     *
     * <br>
     *
     * If for some reason you must provide the copy yourself,
     * and your copy must be in the {@code docbkxModifiableSourcesDirectory},
     * then you can set this to {@code false}
     * to prevent the plugin from replacing the copy.
     * The plugin will then pre-process the copy, however,
     * so expect the files in the modifiable copy to be changed.
     *
     * <br>
     *
     * Default: true
     *
     * @return Whether to overwrite the copy of DocBook sources if it exists.
     */
    public boolean doOverwriteModifiableCopy() {
        return overwriteModifiableCopy;
    }

    /**
     * Overwrite project files with shared content.
     */
    @Parameter(defaultValue = "true", property = "overwriteProjectFilesWithSharedContent")
    private boolean overwriteProjectFilesWithSharedContent;

    /**
     * Whether to overwrite project files with shared content.
     *
     * <br>
     *
     * Default: true
     *
     * @return Whether to overwrite project files with shared content.
     */
    public boolean doOverwriteProjectFilesWithSharedContent() {
        return overwriteProjectFilesWithSharedContent;
    }

    /**
     * Get path name in UNIX format.
     *
     * @param file Path to return in UNIX format.
     * @return The path in UNIX format.
     */
    public String path(final File file) {
        String result = "";
        if (file != null) {
            result = FilenameUtils.separatorsToUnix(file.getPath());
        }
        return result;
    }

    /**
     * Version of the PlantUML artifact to use.
     */
    @Parameter
    private String plantUmlVersion;

    /**
     * Get the version of the PlantUML artifact.
     *
     * @return The version of the PlantUML artifact.
     */
    public String getPlantUmlVersion() {
        return getVersionProperty(plantUmlVersion, "plantUmlVersion");
    }

    /**
     * The version of Plexus Utils used by the XCite Maven plugin.
     */
    @Parameter
    private String plexusUtilsVersion;

    /**
     * Return the version of Plexus Utils used by the XCite Maven plugin.
     *
     * @return The version of Plexus Utils used by the XCite Maven plugin.
     */
    public String getPlexusUtilsVersion() {
        return getVersionProperty(plexusUtilsVersion, "plexusUtilsVersion");
    }

    /**
     * The Maven {@code BuildPluginManager} object.
     */
    @Component
    private BuildPluginManager pluginManager;

    /**
     * Get the Maven {@code BuildPluginManager} object.
     *
     * @return The Maven {@code BuildPluginManager} object.
     */
    public BuildPluginManager getPluginManager() {
        return pluginManager;
    }

    /**
     * CSS file for the pre-site version of the HTML,
     * relative to the build directory.
     */
    private String preSiteCssFileName = "coredoc.css";

    /**
     * Get the CSS file for the pre-site version of the HTML.
     *
     * <br>
     *
     * Value: {@code ${project.build.directory}/coredoc.css}
     *
     * @return The CSS file.
     */
    public final File getPreSiteCss() {
        return new File(getBuildDirectory(), preSiteCssFileName);
    }

    /**
     * The {@code MavenProject} object, which is read-only.
     */
    @Parameter(property = "project", required = true, readonly = true)
    private MavenProject project;

    /**
     * Get the {@code MavenProject} object.
     *
     * @return The {@code MavenProject} object.
     */
    public MavenProject getProject() {
        return project;
    }

    /**
     * Short name of the project, such as OpenAM, OpenDJ, OpenIDM.
     */
    @Parameter(property = "projectName", required = true)
    private String projectName;

    /**
     * Short name of the project, such as OpenAM, OpenDJ, OpenIDM.
     *
     * @return The short name of the project.
     */
    public String getProjectName() {
        return projectName;
    }

    /**
     * Project version.
     */
    @Parameter(property = "projectVersion", required = true)
    private String projectVersion;

    /**
     * Get the project version.
     *
     * @return The project version.
     */
    public String getProjectVersion() {
        return projectVersion;
    }

    /**
     * CSS file for the release version of the HTML,
     * relative to the build directory.
     */
    private String releaseCssFileName = "dfo.css";

    /**
     * Get the CSS file for the release version of the HTML.
     *
     * <br>
     *
     * Value: {@code ${project.build.directory}/dfo.css}
     *
     * @return The CSS file.
     */
    public final File getReleaseCss() {
        return new File(getBuildDirectory(), releaseCssFileName);
    }

    /**
     * Software release date.
     */
    @Parameter(property = "releaseDate")
    private String releaseDate;

    /**
     * Get the software release date.
     *
     * <br>
     *
     * Default: now
     *
     * @return The software release date.
     */
    public String getReleaseDate() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        return releaseDate == null || releaseDate.isEmpty() ? format.format(new Date()) : releaseDate;
    }

    /**
     * File system directory for release layout documentation,
     * relative to the build directory.
     */
    @Parameter(defaultValue = "release")
    private String releaseDirectory;

    /**
     * Get the file system directory for release layout documentation.
     *
     * <br>
     *
     * Default: {@code ${project.build.directory}/release}
     *
     * @return {@link #releaseDirectory}
     */
    public final File getReleaseDirectory() {
        return new File(getBuildDirectory(), releaseDirectory);
    }

    /**
     * Favicon link element for the release version of the HTML.
     */
    @Parameter(defaultValue = "<link rel=\"shortcut icon\" href=\"http://forgerock.org/favicon.ico\">")
    private String releaseFaviconLink;

    /**
     * Get the favicon link element for the release version of the HTML.
     *
     * @return The link element.
     */
    public final String getReleaseFaviconLink() {
        return releaseFaviconLink;
    }

    /**
     * Version for this release.
     */
    @Parameter(property = "releaseVersion", required = true)
    private String releaseVersion;

    /**
     * Get the version for this release.
     *
     * @return The version for this release.
     */
    public final String getReleaseVersion() {
        return releaseVersion;
    }

    /**
     * Get the path to the directory to hold the release version documents,
     * such as {@code ${project.build.directory}/release/1.0.0}.
     *
     * @return The path to the release version directory.
     */
    public final String getReleaseVersionPath() {
        return getReleaseDirectory().getPath() + File.separator + getReleaseVersion();
    }

    /**
     * File system directory for arbitrary documentation set resources,
     * relative to the modifiable sources directory.
     */
    @Parameter(defaultValue = "resources")
    private String resourcesDirectory;

    /**
     * Path to arbitrary documentation set resources,
     * relative to the modifiable sources directory.
     *
     * <br>
     *
     * Default: {@code resources}
     *
     * @return The resources directory path, relative to the modifiable sources directory.
     */
    public String getRelativeResourcesDirectoryPath() {
        return resourcesDirectory;
    }

    /**
     * Directory for arbitrary documentation set resources.
     *
     * <br>
     *
     * Default: {@code ${basedir}/src/main/docbkx/resources}
     *
     * @return The resources directory.
     */
    public File getResourcesDirectory() {
        return new File(getDocbkxModifiableSourcesDirectory(), resourcesDirectory);
    }

    /**
     * Whether to run the ForgeRock link tester plugin.
     */
    @Parameter(defaultValue = "true", property = "runLinkTester")
    private String runLinkTester;

    /**
     * Whether to run the ForgeRock link tester plugin.
     *
     * <br>
     *
     * You only need to run the link test from the top level of a project.
     *
     * <br>
     *
     * Default: {@code "true"}
     *
     * @return Whether to run the ForgeRock link tester plugin.
     */
    public String runLinkTester() {
        return runLinkTester;
    }

    /**
     * The {@code MavenSession} object, which is read-only.
     */
    @Parameter(property = "session", required = true, readonly = true)
    private MavenSession session;

    /**
     * Get the {@code MavenSession} object.
     * @return The {@code MavenSession} object.
     */
    public MavenSession getSession() {
        return session;
    }

    /**
     * Location of the single page HTML XSL stylesheet customization file,
     * relative to the build directory.
     *
     * <br>
     *
     * docbkx-tools element: &lt;htmlCustomization&gt;
     */
    private String singleHTMLCustomization = "/docbkx-stylesheets/html/coredoc.xsl";

    /**
     * Get the location of the single page HTML XSL stylesheet customization file.
     *
     * <br>
     *
     * Value: {@code ${project.build.directory}/docbkx-stylesheets/html/coredoc.xsl}
     *
     * <br>
     *
     * docbkx-tools element: &lt;htmlCustomization&gt;
     *
     * @return The location of the single-page HTML XSL stylesheet.
     */
    public final File getSingleHTMLCustomization() {
        return new File(getBuildDirectory(), singleHTMLCustomization);
    }

    /**
     * File system directory for site content, relative to the build directory.
     */
    @Parameter(defaultValue = "site")
    private String siteDirectory;

    /**
     * Get the file system directory for site content.
     *
     * <br>
     *
     * Default: {@code ${project.build.directory}/site}
     *
     * @return The file system directory for site content.
     */
    public final File getSiteDirectory() {
        return new File(getBuildDirectory(), siteDirectory);
    }

    /**
     * Whether the ForgeRock link tester plugin should skip checking
     * that external URLs are valid.
     *
     * <br>
     *
     * See the {@code skipUrls} parameter of the <a
     * href="https://github.com/aldaris/docbook-linktester/">linktester plugin</a>.
     */
    @Parameter(defaultValue = "false", property = "skipLinkCheck")
    private String skipLinkCheck;

    /**
     * Whether the ForgeRock link tester plugin should skip checking
     * that external URLs are valid.
     *
     * <br>
     *
     * See the {@code skipUrls} parameter of the <a
     * href="https://github.com/aldaris/docbook-linktester/">linktester plugin</a>.
     *
     * <br>
     *
     * Default: {@code false}
     *
     * @return Whether to test that external URLs are valid.
     */
    public String skipLinkCheck() {
        return skipLinkCheck;
    }

    /**
     * Regex patterns of URLs to skip when checking external links.
     *
     * <br>
     *
     * See the {@code skipUrlPatterns} parameter of the <a
     * href="https://github.com/aldaris/docbook-linktester/">linktester plugin</a>.
     */
    @Parameter
    private String[] skipUrlPatterns;

    /**
     * Get regex patterns of URLs to skip when checking external links.
     *
     * <br>
     *
     * Default: {@code null}
     *
     * @return Regex patterns of URLs to skip when checking external links.
     */
    public String[] getSkipUrlPatterns() {
        return skipUrlPatterns;
    }

    /**
     * Whether to build from pre-processed DocBook XML sources.
     */
    @Parameter(defaultValue = "false", property = "usePreProcessedSources")
    private boolean usePreProcessedSources;

    /**
     * Whether to build from pre-processed DocBook XML sources.
     *
     * <p>
     *
     * Default: {@code false}
     *
     * @return True if {@code docbkxSourceDirectory} contains fully pre-processed sources.
     */
    public boolean doUsePreProcessedSources() {
        return usePreProcessedSources;
    }

    /**
     * Whether &lt;programlisting&gt; content has syntax highlighting.
     *
     * <br>
     *
     * docbkx-tools element: &lt;highlightSource&gt;
     */
    private String useSyntaxHighlighting = "1";

    /**
     * Whether &lt;programlisting&gt; content has syntax highlighting.
     *
     * <br>
     *
     * Value: {@code 1} (true)
     *
     * <br>
     *
     * docbkx-tools element: &lt;highlightSource&gt;
     *
     * @return Where program listings use syntax highlighting.
     */
    public final String useSyntaxHighlighting() {
        return useSyntaxHighlighting;
    }

    /**
     * Location of the main CSS for webhelp documents,
     * relative to the build directory.
     */
    private String webhelpCss = "docbkx-stylesheets/webhelp/positioning.css";

    /**
     * Get the location of the main CSS file for webhelp documents.
     *
     * <br>
     *
     * Value: {@code ${project.build.dir}/docbkx-stylesheets/webhelp/positioning.css}
     *
     * @return The main CSS file for webhelp documents.
     */
    public final File getWebHelpCss() {
        return new File(getBuildDirectory(), webhelpCss);
    }

    /**
     * Location of the webhelp XSL stylesheet customization file, relative to
     * the build
     * directory.
     *
     * <br>
     *
     * docbkx-tools element: &lt;webhelpCustomization&gt;
     */
    private String webhelpCustomization = "docbkx-stylesheets/webhelp/coredoc.xsl";

    /**
     * Get the location of the webhelp XSL stylesheet customization file.
     *
     * <br>
     *
     * Value: {@code ${project.build.dir}/docbkx-stylesheets/webhelp/coredoc.xsl}
     *
     * <br>
     *
     * docbkx-tools element: &lt;webhelpCustomization&gt;
     *
     * @return The location of the webhelp XSL stylesheet.
     */
    public final File getWebHelpCustomization() {
        return new File(getBuildDirectory(), webhelpCustomization);
    }

    /**
     * Location of the logo image for webhelp documents,
     * relative to the build directory.
     */
    private String webhelpLogo = "docbkx-stylesheets/webhelp/logo.png";

    /**
     * Get the location of the logo image for webhelp documents.
     *
     * <br>
     *
     * Value: {@code ${project.build.dir}/docbkx-stylesheets/webhelp/logo.png}
     *
     * @return The logo image for webhelp documents.
     */
    public final File getWebHelpLogo() {
        return new File(getBuildDirectory(), webhelpLogo);
    }

    /**
     * Version of the XCite Maven plugin to use.
     */
    @Parameter
    private String xCiteVersion;

    /**
     * Return the version of the XCite Maven plugin to use.
     *
     * @return The version of the XCite Maven plugin to use.
     */
    public String getXCiteVersion() {
        return getVersionProperty(xCiteVersion, "xCiteVersion");
    }

    /**
     * Location of the XHTML5 XSL stylesheet customization file,
     * relative to the build directory.
     *
     * <br>
     *
     * docbkx-tools element: &lt;xhtml5Customization&gt;
     */
    private String xhtml5Customization = "docbkx-stylesheets/xhtml5/coredoc.xsl";

    /**
     * Location of the XHTML5 XSL stylesheet customization file,
     * relative to the build directory.
     *
     * <br>
     *
     * Value: {@code ${project.build.directory}/docbkx-stylesheets/xhtml5/coredoc.xsl}
     *
     * <br>
     *
     * Default: {@code ${project.build.directory}/docbkx-stylesheets/xhtml5/coredoc.xsl}
     *
     * <br>
     *
     * docbkx-tools element: &lt;xhtml5Customization&gt;
     *
     * @return The location of the XHTML5 XSL stylesheet.
     */
    public final File getXhtml5Customization() {
        return new File(getBuildDirectory(), xhtml5Customization);
    }

    /**
     * Location of the HtmlForBootstrap XSL stylesheet customization file,
     * relative to the build directory.
     *
     * <br>
     *
     * docbkx-tools element: &lt;bootstrapCustomization&gt;
     */
    private String bootstrapCustomization =
            "docbkx-stylesheets/bootstrap/coredoc.xsl";
    /**
     * Get the location of the HtmlForBootstrap XSL stylesheet customization file.
     *
     * <br>
     *
     * Default: {@code ${project.build.directory}/docbkx-stylesheets/bootstrap/coredoc.xsl}
     *
     * <br>
     *
     * docbkx-tools element: &lt;bootstrapCustomization&gt;
     *
     * @return The location of the HtmlForBootstrap XSL stylesheet.
     */
    public final File getBootstrapCustomization() {
        return new File(getBuildDirectory(), bootstrapCustomization);
    }
}
