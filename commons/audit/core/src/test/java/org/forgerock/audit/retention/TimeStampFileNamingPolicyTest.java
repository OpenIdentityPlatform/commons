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
 * Portions Copyright 2026 3A Systems, LLC.
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

    public static final String TIME_STAMP_DATE_FORMAT = "-MM.dd.yy-HH.mm.ss";
    public static final String PREFIX = "prefix-";

    @Test
    public void testGetInitialName() {
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
    public void testGetNextName() {
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
    public void testGetNextNameKeepsCompressionSuffixLastOnCollision() throws Exception {
        // given
        final File initialFile = getTempFile();
        // the timestamp changes once a year, so the second name collides with the first one
        final TimeStampFileNamingPolicy fileNamingPolicy =
                new TimeStampFileNamingPolicy(initialFile, "-yyyy.gz", PREFIX);
        final File firstFile = fileNamingPolicy.getNextName();
        assertThat(firstFile.createNewFile()).isTrue();
        firstFile.deleteOnExit();

        // when
        final File file = fileNamingPolicy.getNextName();

        // then
        final String firstName = firstFile.getName();
        assertThat(firstName).endsWith(".gz");
        assertThat(file.getName()).isEqualTo(firstName.substring(0, firstName.length() - ".gz".length()) + ".1.gz");
    }

    @Test
    public void testGetNextNameSkipsEveryExistingCollisionName() throws Exception {
        // given
        final File initialFile = getTempFile();
        final File firstFile = new TimeStampFileNamingPolicy(initialFile, "-yyyy.gz", PREFIX).getNextName();
        final String timestampedName = firstFile.getName().substring(0, firstFile.getName().length() - ".gz".length());
        createFile(initialFile, timestampedName + ".gz");
        createFile(initialFile, timestampedName + ".1.gz");
        // a new policy, as after a restart, counts collisions from the start again
        final TimeStampFileNamingPolicy fileNamingPolicy =
                new TimeStampFileNamingPolicy(initialFile, "-yyyy.gz", PREFIX);

        // when
        final File file = fileNamingPolicy.getNextName();

        // then
        assertThat(file.getName()).isEqualTo(timestampedName + ".2.gz");
    }

    @Test
    public void testGetNextNameSkipsNameOfFileLeftUncompressed() throws Exception {
        // given
        final File initialFile = getTempFile();
        final TimeStampFileNamingPolicy fileNamingPolicy =
                new TimeStampFileNamingPolicy(initialFile, "-yyyy.gz", PREFIX);
        final File firstFile = fileNamingPolicy.getNextName();
        final String timestampedName = firstFile.getName().substring(0, firstFile.getName().length() - ".gz".length());
        // a rotated file is renamed without the .gz suffix before it is compressed
        createFile(initialFile, timestampedName);

        // when
        final File file = fileNamingPolicy.getNextName();

        // then
        assertThat(file.getName()).isEqualTo(timestampedName + ".1.gz");
    }

    private static void createFile(final File initialFile, final String name) throws Exception {
        final File file = new File(initialFile.getParentFile(), name);
        assertThat(file.createNewFile()).isTrue();
        file.deleteOnExit();
    }

    @Test
    public void testListFiles() throws Exception {
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
