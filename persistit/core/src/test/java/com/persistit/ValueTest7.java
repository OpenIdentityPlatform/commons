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

import com.persistit.Value.Version;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ValueTest7 extends PersistitUnitTestCase {

    @Test
    public void testUnpackMvvVersionsPrimordial() throws Exception {

        List<Version> versions;
        Version version;
        final Value value = new Value(_persistit);

        versions = value.unpackMvvVersions();
        assertEquals(1, versions.size());
        version = versions.get(0);
        assertFalse(version.getValue().isDefined());

        value.put(RED_FOX);
        versions = value.unpackMvvVersions();
        assertEquals(1, versions.size());
        version = versions.get(0);
        assertEquals(0, version.getCommitTimestamp());
        assertEquals(RED_FOX, version.getValue().get());
    }

    @Test
    public void testUnpackMvvVersions() throws Exception {
        final Value value = new Value(_persistit);
        value.ensureFit(100000);
        final Value v = new Value(_persistit);
        for (int i = 1; i < 5; i++) {
            v.put(RED_FOX + "_" + i);
            final int s = TestShim.storeVersion(value.getEncodedBytes(), 0, value.getEncodedSize(), 100000, i,
                    v.getEncodedBytes(), 0, v.getEncodedSize());
            value.setEncodedSize(s);
        }
        final List<Version> versions = value.unpackMvvVersions();
        for (int i = 0; i < 5; i++) {
            final Version version = versions.remove(0);
            assertEquals(i, version.getVersionHandle());
            if (i > 0) {
                assertEquals(RED_FOX + "_" + i, version.getValue().get());
            }
        }

    }
}
