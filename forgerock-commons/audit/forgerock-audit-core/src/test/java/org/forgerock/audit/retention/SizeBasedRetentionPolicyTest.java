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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.testng.annotations.Test;

public class SizeBasedRetentionPolicyTest {

    private static final int MAX_HISTORY_FILES = 3;

    @Test
    public void testGettingFilesToDeleteWhenTooManyHistoryExist() {
        // given
        final SizeBasedRetentionPolicy retentionPolicy = new SizeBasedRetentionPolicy(MAX_HISTORY_FILES);
        final FileNamingPolicy fileNamingPolicy = mock(FileNamingPolicy.class);
        final List<File> archivedFiles = new LinkedList<>();
        for (int i = 0; i < MAX_HISTORY_FILES + 1; i++) {
            archivedFiles.add(mock(File.class));
        }
        when(fileNamingPolicy.listFiles()).thenReturn(archivedFiles);

        // when
        List<File> filesToDelete = retentionPolicy.deleteFiles(fileNamingPolicy);

        // then
        assertThat(filesToDelete).containsOnly(archivedFiles.get(0));
    }

    @Test
    public void testGettingFilesToDeleteWhenNotUsingTooMuchDiskSpace() {
        // given
        final SizeBasedRetentionPolicy retentionPolicy = new SizeBasedRetentionPolicy(MAX_HISTORY_FILES);
        final FileNamingPolicy fileNamingPolicy = mock(FileNamingPolicy.class);
        final List<File> archivedFiles = new LinkedList<>();
        for (int i = 0; i < MAX_HISTORY_FILES - 1; i++) {
            archivedFiles.add(mock(File.class));
        }
        when(fileNamingPolicy.listFiles()).thenReturn(archivedFiles);

        // when
        List<File> filesToDelete = retentionPolicy.deleteFiles(fileNamingPolicy);

        // then
        assertThat(filesToDelete).isEmpty();
    }
}
