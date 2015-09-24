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

import static java.lang.String.valueOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.util.Files.newFile;
import static org.assertj.core.util.Files.temporaryFolderPath;
import static org.assertj.core.util.Strings.concat;

import java.io.File;
import java.util.List;

import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormat;
import org.testng.annotations.Test;

public class TimeStampFileNamingPolicyTest {

    public static final String TIME_STAMP_DATE_FORMAT = "-MM.dd.yy-kk.mm.ss";
    public static final String PREFIX = "prefix-";

    @Test
    public void TestGetInitialName() {
        // given
        final File initialFile = getTempFile();
        final TimeStampFileNamingPolicy fileNamingPolicy =
                new TimeStampFileNamingPolicy(initialFile, TIME_STAMP_DATE_FORMAT, PREFIX);

        // when
        final File file = fileNamingPolicy.getInitialName();

        // then
        assertThat(file).isEqualTo(initialFile);
    }

    @Test
    public void TestGetNextName() {
        // given
        final File initialFile = getTempFile();
        final TimeStampFileNamingPolicy fileNamingPolicy =
                new TimeStampFileNamingPolicy(initialFile, TIME_STAMP_DATE_FORMAT, PREFIX);

        // when
        final File file = fileNamingPolicy.getNextName();

        // then
        final String filename = file.toPath().getFileName().toString();
        assertThat(filename).startsWith(PREFIX + initialFile.toPath().getFileName());
    }

    @Test
    public void TestListFiles() throws Exception {
        // given
        final File initialFile = getTempFile();
        final TimeStampFileNamingPolicy fileNamingPolicy =
                new TimeStampFileNamingPolicy(initialFile, TIME_STAMP_DATE_FORMAT, PREFIX);
        File archiveFile =
                new File(initialFile.getParent(), createNewFilename(initialFile.toPath().getFileName().toString()));
        archiveFile.createNewFile();
        archiveFile.deleteOnExit();

        // when
        final List<File> files = fileNamingPolicy.listFiles();

        // then
        assertThat(files).containsOnly(archiveFile);
    }

    private String createNewFilename(final String filename) {
        return PREFIX + filename + LocalDateTime.now().toString(DateTimeFormat.forPattern(TIME_STAMP_DATE_FORMAT));
    }

    private File getTempFile() {
        // define file name using nanoTime instead of currentTimeMillis since the tests run so fast
        String tempFileName = concat(valueOf(System.nanoTime()), ".txt");
        File file = newFile(concat(temporaryFolderPath(), tempFileName));
        file.deleteOnExit();
        return file;
    }
}
