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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.testng.annotations.Test;

public class DiskSpaceUsedRetentionPolicyTest {

    public static final long MAX_DISK_SPACE_TO_USE = 1000;

    @Test
    public void testGettingFilesToDeleteWhenOverMaxDiskSpace() {
        // given
        final DiskSpaceUsedRetentionPolicy retentionPolicy = new DiskSpaceUsedRetentionPolicy(MAX_DISK_SPACE_TO_USE);
        final FileNamingPolicy fileNamingPolicy = mock(FileNamingPolicy.class);
        final File file = mock(File.class);
        when(fileNamingPolicy.listFiles()).thenReturn(new ArrayList<>(Collections.singletonList(file)));
        when(file.length()).thenReturn(MAX_DISK_SPACE_TO_USE + 1L);

        // when
        List<File> filesToDelete = retentionPolicy.deleteFiles(fileNamingPolicy);

        // then
        assertThat(filesToDelete).contains(file);
    }

    @Test
    public void testGettingFilesToDeleteWhenNotOverMaxDiskSpace() {
        // given
        final DiskSpaceUsedRetentionPolicy retentionPolicy = new DiskSpaceUsedRetentionPolicy(MAX_DISK_SPACE_TO_USE);
        final FileNamingPolicy fileNamingPolicy = mock(FileNamingPolicy.class);
        final File file = mock(File.class);
        when(fileNamingPolicy.listFiles()).thenReturn(new ArrayList<>(Collections.singletonList(file)));
        when(file.length()).thenReturn(MAX_DISK_SPACE_TO_USE - 1L);

        // when
        List<File> filesToDelete = retentionPolicy.deleteFiles(fileNamingPolicy);

        // then
        assertThat(filesToDelete).isEmpty();
    }
}
