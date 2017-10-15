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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.json.resource.http;

import static org.mockito.Mockito.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

import org.forgerock.http.io.BranchingInputStream;
import org.forgerock.http.io.Buffer;
import org.forgerock.http.io.PipeBufferedStream;
import org.forgerock.util.Factory;
import org.testng.annotations.Test;

public class PipeBufferedStreamTest {

    @Test
    public void shouldReReadWritesFromSeparateBranches() throws IOException {

        //Given
        byte[] firstBytes = ("{\"result\":[{\"intField\":42,\"stringField\":\"stringValue\"}" + "],"
                + "\"resultCount\":1,").getBytes(Charset.forName("UTF-8"));
        byte[] secondBytes = ("\"error\":{\"code\":404,\"reason\":\"Not Found\",\"message\":\"Not Found\"}}")
                .getBytes(Charset.forName("UTF-8"));

        PipeBufferedStream pipe = new PipeBufferedStream();
        OutputStream outputStream = pipe.getIn();
        BranchingInputStream inputStream = pipe.getOut();
        BranchingInputStream branch = inputStream.branch();

        //When
        for (byte i : firstBytes) {
            outputStream.write(i);
        }
        byte[] firstRead = new byte[firstBytes.length];

        inputStream.read(firstRead);

        for (byte i : secondBytes) {
            outputStream.write(i);
        }
        byte[] secondRead = new byte[firstBytes.length + secondBytes.length];
        branch.read(secondRead);

        //Then
        assertEquals(new String(firstRead), "{\"result\":["
                + "{\"intField\":42,\"stringField\":\"stringValue\"}" + "]," + "\"resultCount\":1,");
        assertEquals(new String(secondRead), "{\"result\":["
                + "{\"intField\":42,\"stringField\":\"stringValue\"}" + "]," + "\"resultCount\":1,"
                + "\"error\":{\"code\":404,\"reason\":\"Not Found\",\"message\":\"Not Found\"}}");
    }

    @Test
    public void shouldReadSubsequentWrites() throws IOException {

        //Given
        byte[] firstBytes = ("{\"result\":[{\"intField\":42,\"stringField\":\"stringValue\"}" + "],"
                + "\"resultCount\":1,").getBytes(Charset.forName("UTF-8"));
        byte[] secondBytes = ("\"error\":{\"code\":404,\"reason\":\"Not Found\",\"message\":\"Not Found\"}}")
                .getBytes(Charset.forName("UTF-8"));

        PipeBufferedStream pipe = new PipeBufferedStream();
        OutputStream outputStream = pipe.getIn();
        BranchingInputStream inputStream = pipe.getOut();

        //When
        for (byte i : firstBytes) {
            outputStream.write(i);
        }
        byte[] firstRead = new byte[firstBytes.length];
        inputStream.read(firstRead);

        //Then input stream should be empty as have read everything from output stream.
        int empty = inputStream.read();
        assertTrue(empty == -1);

        for (byte i : secondBytes) {
            outputStream.write(i);
        }
        byte[] secondRead = new byte[secondBytes.length];
        inputStream.read(secondRead);

        //Then
        String fullRead = new String(firstRead) + new String(secondRead);
        assertEquals(fullRead, "{\"result\":["
                + "{\"intField\":42,\"stringField\":\"stringValue\"}" + "]," + "\"resultCount\":1,"
                + "\"error\":{\"code\":404,\"reason\":\"Not Found\",\"message\":\"Not Found\"}}");
    }

    @Test
    public void shouldCloseBufferAfterOutputStreamThenInputStreamAreClosed() throws Exception {
        Buffer mockBuffer = mock(Buffer.class);
        @SuppressWarnings("unchecked")
        Factory<Buffer> mockBufferFactory = mock(Factory.class);
        when(mockBufferFactory.newInstance()).thenReturn(mockBuffer);

        PipeBufferedStream pipe = new PipeBufferedStream(mockBufferFactory);
        OutputStream outputStream = pipe.getIn();
        BranchingInputStream inputStream = pipe.getOut();

        verify(mockBufferFactory, times(1)).newInstance();
        verify(mockBuffer, times(0)).close();

        outputStream.close();
        verify(mockBuffer, times(0)).close();

        inputStream.close();
        verify(mockBuffer, times(1)).close();
    }

    @Test
    public void shouldCloseBufferAfterInputStreamThenOutputStreamAreClosed() throws Exception {
        Buffer mockBuffer = mock(Buffer.class);
        @SuppressWarnings("unchecked")
        Factory<Buffer> mockBufferFactory = mock(Factory.class);
        when(mockBufferFactory.newInstance()).thenReturn(mockBuffer);

        PipeBufferedStream pipe = new PipeBufferedStream(mockBufferFactory);
        OutputStream outputStream = pipe.getIn();
        BranchingInputStream inputStream = pipe.getOut();

        verify(mockBufferFactory, times(1)).newInstance();
        verify(mockBuffer, times(0)).close();

        inputStream.close();
        verify(mockBuffer, times(0)).close();

        outputStream.close();
        verify(mockBuffer, times(1)).close();
    }
}
