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
package org.forgerock.audit.handlers.csv;

import static java.nio.charset.StandardCharsets.*;
import static org.assertj.core.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.List;

import org.forgerock.audit.handlers.csv.CsvSecureArchiveVerifierCli.OptionsParser;
import org.forgerock.audit.handlers.csv.CsvSecureVerifier.VerificationResult;
import org.testng.annotations.Test;

@SuppressWarnings("javadoc")
public class CsvSecureArchiveVerifierCliTest {

    private static final String NEW_LINE = System.lineSeparator();

    @Test
    public void canPrintVerificationResults() throws UnsupportedEncodingException {
        // given
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        List<VerificationResult> results = Arrays.asList(
                new VerificationResult(new File("access.csv.1"), true, ""),
                new VerificationResult(new File("access.csv.2"), false, "Missing signature"),
                new VerificationResult(new File("access.csv.3"), true, ""));

        // when
        CsvSecureArchiveVerifierCli.printVerificationResults(results, new PrintStream(out));

        // then
        assertThat(out.toString(UTF_8.name())).isEqualTo(""
                + "PASS    access.csv.1" + NEW_LINE
                + "FAIL    access.csv.2    Missing signature" + NEW_LINE
                + "PASS    access.csv.3" + NEW_LINE);
    }

    @Test
    public void canPrintHelpWhenNoArgsProvided() throws UnsupportedEncodingException {
        // given
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        OptionsParser optionsParser = new OptionsParser(new PrintStream(out), new PrintStream(err));

        // when
        optionsParser.parse(new String[]{});

        // then
        assertThat(out.toString(UTF_8.name())).isEqualTo(""
                + "arguments: --archive <path> --topic <topic> "
                + "[--prefix <prefix>] [--suffix <suffix>] --keystore <path> --password <password>" + NEW_LINE
                + NEW_LINE
                + "   --archive       path to directory containing files to verify" + NEW_LINE
                + "   --topic         name of topic fileset to verify" + NEW_LINE
                + "   --prefix        prefix prepended to archive files" + NEW_LINE
                + "   --suffix        format of timestamp suffix appended to archive files" + NEW_LINE
                + "   --keystore      path to keystore file" + NEW_LINE
                + "   --password      keystore file password" + NEW_LINE);
        assertThat(err.toString()).isEqualTo("");
    }
}
