/**
 * Copyright 2011-2012 Akiban Technologies, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.persistit;

import com.persistit.util.Util;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class CommandLineTest extends PersistitUnitTestCase {

    @Test
    public void testCliParser() throws Exception {
        assertNotNull(TestShim.parseTask(_persistit, "backup file=somefile -a -y -z"));
        assertNotNull(TestShim.parseTask(_persistit, "save trees=persistit file=somefile"));
        assertNotNull(TestShim.parseTask(_persistit, "load trees=persistit:*{1:2} file=somefile -t -n"));
        assertNull(TestShim.parseTask(_persistit, "open datapath=somefile"));
        try {
            TestShim.parseTask(_persistit, "backup file=somefile -s -y -z wrong=parameter");
            fail();
        } catch (final Exception e) {
            // okay
        }
    }

    @Test
    public void testCommands() throws Exception {

        final PersistitMap<Integer, String> pmap = new PersistitMap<Integer, String>(_persistit.getExchange(
                "persistit", "CommandLineTest", true));
        for (int index = 0; index < 500; index++) {
            pmap.put(new Integer(index), "This is the record for index=" + index);
        }

        final Management management = _persistit.getManagement();

        String status = management.launch("icheck trees=persistit:CommandLineTest");
        waitForCompletion(taskId(status));
        final File file = File.createTempFile("CommandLineTest", ".sav");
        file.deleteOnExit();
        status = management.launch("save file=" + file + " trees=persistit:CommandLineTest{200:}");
        waitForCompletion(taskId(status));
        pmap.clear();

        status = management.launch("load file=" + file);
        waitForCompletion(taskId(status));

        assertEquals(300, pmap.size());

        status = management.launch("jquery -T -V -v page=1");
        waitForCompletion(taskId(status));

    }

    @Test
    public void testSelect() throws Exception {
        final CLI cli = _persistit.getSessionCLI();
        final Tree tree = _persistit.getVolume("persistit").getTree("atree", true);
        final Management management = _persistit.getManagement();
        management.execute("select tree=persistit:*tree");
        assertEquals("Should be same tree", tree, cli.getCurrentTree());
        _persistit.getVolume("persistit").getTree("btree", true);
        management.execute("select tree=persistit:*tree");
        assertNull("No tree or volume should be selected", cli.getCurrentTree());
        assertNull("No tree or volume should be selected", cli.getCurrentVolume());
    }

    @Test
    public void testScript() throws Exception {
        final PersistitMap<Integer, String> pmap = new PersistitMap<Integer, String>(_persistit.getExchange(
                "persistit", "CommandLineTest", true));
        for (int index = 0; index < 500; index++) {
            pmap.put(new Integer(index), "This is the record for index=" + index);
        }

        final String datapath = _persistit.getConfiguration().getProperty("datapath");
        final String rmiport = _persistit.getConfiguration().getProperty("rmiport");

        _persistit.close();

        final StringReader stringReader = new StringReader(String.format(
                "help\nopen datapath=%s rmiport=%s\nicheck -v\n", datapath, rmiport));
        final BufferedReader reader = new BufferedReader(stringReader);
        final StringWriter stringWriter = new StringWriter();
        final PrintWriter writer = new PrintWriter(stringWriter);
        CLI.runScript(null, reader, writer);
        final String result = stringWriter.toString();
        assertTrue(result.contains("data"));
    }

    private long taskId(final String status) {
        return Long.parseLong(status);
    }

    private void waitForCompletion(final long taskId) throws Exception {
        for (int waiting = 0; waiting < 60; waiting++) {
            final String status = _persistit.getManagement().execute("task taskId=" + taskId);
            if (!status.isEmpty()) {
                final String[] s = status.split(Util.NEW_LINE, 2);
                if (s.length == 2) {
                    System.out.println(s[1]);
                }
            } else {
                return;
            }

            Thread.sleep(500);
        }
        throw new IllegalStateException("Task " + taskId + " did not compelete within 10 seconds");
    }

    @Override
    public void runAllTests() throws Exception {

    }

}
