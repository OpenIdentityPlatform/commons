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
 * Copyright 2013-2015 ForgeRock AS.
 */

package org.forgerock.doc.maven.utils;

import org.apache.commons.io.DirectoryWalker;
import org.apache.commons.io.FileUtils;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Apply and XSL transformation to DocBook XML files.
 */
public class XmlTransformer extends DirectoryWalker<File> {

    /**
     * Construct an updater to match DocBook XML files.
     *
     * <p>
     *
     * The files are updated in place.
     *
     * <p>
     *
     * The XSL resource must be on the classpath,
     * and so you probably want to extend this with your own transformer class.
     *
     * @param  filterToMatch    Filter to match XML files.
     * @param  xslResource      Path to XSL resource.
     */
    public XmlTransformer(final FileFilter filterToMatch, final String xslResource) {
        super(filterToMatch, -1);

        try {
            this.transformer = getTransformer(xslResource);
        } catch (IOException ie) {
            System.err.println(Arrays.toString(ie.getStackTrace()));
            System.exit(1);
        } catch (TransformerConfigurationException tce) {
            System.err.println(Arrays.toString(tce.getStackTrace()));
            System.exit(1);
        }
    }

    /**
     * Construct an updater to match DocBook XML files.
     *
     * <p>
     *
     * The files are updated in place.
     *
     * @param  filterToMatch    Filter to match XML files.
     * @param  xsl              XSL as string.
     */
    public XmlTransformer(final String xsl, final FileFilter filterToMatch) {
        super(filterToMatch, -1);

        try {
            this.transformer = getTransformerForString(xsl);
        } catch (TransformerConfigurationException tce) {
            System.err.println(Arrays.toString(tce.getStackTrace()));
            System.exit(1);
        }
    }

    private Transformer transformer;

    private Transformer getTransformer(final String xslResource)
            throws IOException, TransformerConfigurationException {
        TransformerFactory factory = TransformerFactory.newInstance();
        Source xslt = new StreamSource(getClass().getResource(xslResource).openStream());
        return factory.newTransformer(xslt);
    }

    private Transformer getTransformerForString(final String xsl) throws TransformerConfigurationException {
        TransformerFactory factory = TransformerFactory.newInstance();
        Source xslt = new StreamSource(new ByteArrayInputStream(xsl.getBytes(Charset.forName("UTF-8"))));
        return factory.newTransformer(xslt);
    }

    /**
     * Update files that match the filter.
     *
     * @param startDirectory
     *            Base directory under which to update files, recursively
     * @return List of updated files
     * @throws java.io.IOException
     *             Something went wrong changing a file's content.
     */
    public final List<File> update(final File startDirectory) throws IOException {
        List<File> results = new ArrayList<File>();
        walk(startDirectory, results);
        return results;
    }

    /**
     * Update files that match, adding them to the results.
     *
     * @param file
     *            File to update
     * @param depth
     *            Not used
     * @param results
     *            List of files updated
     * @throws java.io.IOException
     *             Something went wrong changing a file's content.
     */
    @Override
    protected final void handleFile(final File file,
                                    final int depth,
                                    final Collection<File> results)
            throws IOException {
        if (file.isFile()) {
            try {
                Source xml = new StreamSource(file);
                File tmpFile = File.createTempFile(file.getName(), ".tmp");
                transformer.transform(xml, new StreamResult(tmpFile));

                FileUtils.deleteQuietly(file);
                FileUtils.moveFile(tmpFile, file);
                results.add(file);
            } catch (TransformerException te) {
                throw new IOException(file.toString()+te.getMessageAndLocation(), te);
            }
        }
    }
}
