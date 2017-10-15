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
 * A {@link RetentionPolicy} that will retain/delete log files given a minimum amount of disk space the file system
 * must contain.
 */
public class FreeDiskSpaceRetentionPolicy implements RetentionPolicy {

    private final long minFreeSpaceRequired;
    private final Comparator<File> comparator = new LastModifiedTimeFileComparator();

    /**
     * Constructs a {@link FreeDiskSpaceRetentionPolicy} given a minimum amount of disk space the file system must
     * contain.
     * @param minFreeSpaceRequired The minimum amount of free disk space the the file system must contain in bytes.
     */
    public FreeDiskSpaceRetentionPolicy(final long minFreeSpaceRequired) {
        this.minFreeSpaceRequired = minFreeSpaceRequired;
    }

    @Override
    public List<File> deleteFiles(FileNamingPolicy fileNamingPolicy) {
        final List<File> archivedFiles = fileNamingPolicy.listFiles();
        if (archivedFiles.isEmpty()) {
            return Collections.emptyList();
        }

        final long freeSpace = archivedFiles.get(0).getUsableSpace();
        if (freeSpace >= minFreeSpaceRequired) {
            return Collections.emptyList();
        }

        final long freeSpaceNeeded = minFreeSpaceRequired - freeSpace;

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
