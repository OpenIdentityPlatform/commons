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
 * Copyright 2015 ForgeRock AS.
 */
package org.forgerock.audit.retention;

import java.io.File;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Created a size based file retention policy. This policy stores a set number of archived files.
 */
public class SizeBasedRetentionPolicy implements RetentionPolicy {
    private final int maxNumberOfFiles;

    /**
     * Constructs a SizeBasedRetentionPolicy with a given maximum number of archived files. A negative value
     * disables pruning of old archive files.
     * @param maxNumberOfFiles The maximum number of archive files to keep. A negative value will disable
     *                         pruning old archive files.
     */
    public SizeBasedRetentionPolicy(final int maxNumberOfFiles) {
        this.maxNumberOfFiles = maxNumberOfFiles;
    }

    @Override
    public List<File> deleteFiles(FileNamingPolicy fileNamingPolicy) {
        final List<File> managedArchivedFiles = fileNamingPolicy.listFiles();
        final int numberOfManagedArchiveFiles = managedArchivedFiles.size();
        if (maxNumberOfFiles <= 0 || numberOfManagedArchiveFiles <= maxNumberOfFiles) {
            return Collections.emptyList();
        } else {
            final List<File> filesToDelete =
                    new LinkedList<>(managedArchivedFiles.subList(0, numberOfManagedArchiveFiles - maxNumberOfFiles));
            return filesToDelete;
        }
    }
}
