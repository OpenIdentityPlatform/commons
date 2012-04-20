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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 *      Copyright 2011 ForgeRock AS
 */
package org.forgerock.i18n.maven;

import java.io.File;

/**
 * Goal which generates message source files from a one or more property files.
 *
 * @Checkstyle:ignoreFor 2
 * @goal generate-messages
 * @phase generate-sources
 * @threadSafe
 */
public final class GenerateMessagesMojo extends AbstractGenerateMessagesMojo {

    /**
     * The target directory in which the source files should be generated.
     *
     * @parameter
     *            default-value="${project.build.directory}/generated-sources/messages"
     * @required
     */
    private File targetDirectory;

    /**
     * The resource directory containing the message files.
     *
     * @parameter default-value="${basedir}/src/main/resources"
     * @required
     */
    private File resourceDirectory;

    /**
     * {@inheritDoc}
     */
    @Override
    void addNewSourceDirectory(final File targetDirectory) {
        getMavenProject().addCompileSourceRoot(
                targetDirectory.getAbsolutePath());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    File getResourceDirectory() {
        return resourceDirectory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    File getTargetDirectory() {
        return targetDirectory;
    }

}
