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
 * Copyright 2015-2016 ForgeRock AS.
 */
package org.forgerock.audit.retention;

import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.forgerock.audit.util.LastModifiedTimeFileComparator;

/**
 * A {@link RetentionPolicy} that will retain/delete log files based off the total disk space used.
 */
public class DiskSpaceUsedRetentionPolicy implements RetentionPolicy {
    private final long maxDiskSpaceToUse;
    private final Comparator<File> comparator = new LastModifiedTimeFileComparator();

    /**
     * Constructs a {@link DiskSpaceUsedRetentionPolicy} with a given maximum of disk space to use in bytes.
     * @param maxDiskSpaceToUse The maximum amount of disk space the historical audit files can occupy.
     */
    public DiskSpaceUsedRetentionPolicy(final long maxDiskSpaceToUse) {
        this.maxDiskSpaceToUse = maxDiskSpaceToUse;
    }

    @Override
    public List<File> deleteFiles(FileNamingPolicy fileNamingPolicy) {
        final List<File> archivedFiles = fileNamingPolicy.listFiles();
        long currentDiskSpaceUsed = 0L;
        for (final File file: archivedFiles) {
            currentDiskSpaceUsed += file.length();
        }

        if (currentDiskSpaceUsed <= maxDiskSpaceToUse) {
            return Collections.emptyList();
        }

        final long freeSpaceNeeded = currentDiskSpaceUsed - maxDiskSpaceToUse;
        Collections.sort(archivedFiles, comparator);

        long freedSpace = 0L;
        List<File> filesToDelete = new LinkedList<>();
        for (File file : archivedFiles) {
            filesToDelete.add(file);
            freedSpace += file.length();
            if (freedSpace >= freeSpaceNeeded) {
                break;
            }
        }

        return filesToDelete;
    }
}
