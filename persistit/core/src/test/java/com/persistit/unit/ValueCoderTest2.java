/**
 * Copyright 2005-2012 Akiban Technologies, Inc.
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

package com.persistit.unit;

import com.persistit.DefaultObjectCoder;
import com.persistit.Exchange;
import com.persistit.PersistitUnitTestCase;
import com.persistit.exception.PersistitException;
import org.junit.Test;

import java.lang.reflect.Array;
import java.util.TreeMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ValueCoderTest2 extends PersistitUnitTestCase {

    Exchange _exchange;
    DefaultObjectCoder _coder;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        _coder = DefaultObjectCoder.registerObjectCoder(_persistit, Vehicle.class, new String[] { "id" }, new String[] {
                "id", "description", "speed", "wheels", "passengers", "canFly", "other1", "other2" });

        _exchange = _persistit.getExchange("persistit", getClass().getSimpleName(), true);
    }

    @Override
    public void tearDown() throws Exception {
        _exchange = null;
        _coder = null;
        super.tearDown();
    }

    static class Vehicle {
        String id;
        String description;
        int speed;
        int wheels;
        int passengers;
        boolean canFly;
        Vehicle other1;
        Vehicle other2;

        Vehicle() {
        }

        Vehicle(final String id, final String description, final int speed, final int wheels, final int passengers,
                final boolean canFly) {
            this.id = id;
            this.description = description;
            this.speed = speed;
            this.wheels = wheels;
            this.passengers = passengers;
            this.canFly = canFly;
        }

        @Override
        public boolean equals(final Object object) {
            final Vehicle v = (Vehicle) object;
            return id.equals(v.id) && description.equals(v.description) && (speed == v.speed) && (wheels == v.wheels)
                    && (passengers == v.passengers) && (canFly == v.canFly) && equals(other1, v.other1)
                    && equals(other2, v.other2);
        }

        private boolean equals(final Object o1, final Object o2) {
            if (o1 == null) {
                return o2 == null;
            } else if (o2 == null) {
                return false;
            } else {
                return ((Vehicle) o1).equals(o2);
            }
        }

        @Override
        public String toString() {
            return "Vehicle(id=" + id + ",description=" + description + ",speed=" + speed + ",wheels=" + wheels
                    + ",passengers=" + passengers + ",canFly=" + canFly + ",other1="
                    + (other1 == null ? null : other1.id) + ",other2=" + (other1 == null ? null : other2.id) + ")";
        }
    }

    @Test
    public void test1() throws PersistitException {
        System.out.print("test1 ");
        final Vehicle car1 = new Vehicle("c1", "Colt", 58, 4, 4, false);
        final Vehicle car2 = new Vehicle("c2", "Viper", 158, 4, 2, false);
        final Vehicle plane = new Vehicle("p1", "Piper", 220, 3, 4, true);

        _exchange.getValue().put(car1);
        _exchange.clear().append(car1).store();

        _exchange.getValue().put(car2);
        _exchange.clear().append(car2).store();

        _exchange.getValue().put(plane);
        _exchange.clear().append(plane).store();

        final Vehicle v = new Vehicle();
        v.id = "c1";
        _exchange.clear().append(v).fetch();
        final Object v1 = _exchange.getValue().get();
        assertEquals(v1, car1);

        final Vehicle v2 = new Vehicle();
        _exchange.next();
        _exchange.getKey().decode(v2);
        final Object k2a = v2.id;
        _exchange.getValue().get(v2);
        final Object k2b = v2.id;
        assertEquals(v2, car2);
        assertEquals(k2a, k2b);

        final Vehicle v3 = new Vehicle();
        _exchange.next();
        _exchange.getValue().get(v3);
        assertEquals(v3, plane);

        v.id = "c1";
        _exchange.clear().append(v).remove();

        v.id = "c2";
        _exchange.clear().append(v).remove();

        v.id = "plane";
        _exchange.clear().append(v).remove();

        System.out.println("- done");
    }

    @Test
    public void test2() throws PersistitException {
        System.out.print("test2 ");

        final TreeMap x = new TreeMap();
        final TreeMap y = new TreeMap();
        final TreeMap z = new TreeMap();

        final Vehicle car1 = new Vehicle("c1", "Colt", 58, 4, 4, false);
        final Vehicle car2 = new Vehicle("c2", "Viper", 158, 4, 2, false);
        final Vehicle plane = new Vehicle("p1", "Piper", 220, 3, 4, true);

        final TreeMap map1 = new TreeMap();
        map1.put("c1", car1);
        map1.put("c2", car2);
        map1.put("plane", plane);

        Object map2;
        String toString1;
        String toString1a;
        String toString2;

        // _exchange.getValue().put(map1);
        // _exchange.clear().append("map1").store();
        // _exchange.getValue().clear();
        // _exchange.fetch();
        //
        // map2 = _exchange.getValue().get();
        // toString1 = map1.toString();
        // toString1a = _exchange.getValue().toString();
        // toString2 = map2.toString();
        // assertTrue(map2 instanceof TreeMap);
        // assertEquals(toString1, toString2);

        map1.put("c1a", car1);
        map1.put("c2a", car2);
        map1.put("planea", plane);

        _exchange.getValue().put(map1);
        _exchange.clear().append("map1").store();
        _exchange.getValue().clear();
        _exchange.fetch();

        map2 = _exchange.getValue().get();
        toString1 = map1.toString();
        toString1a = _exchange.getValue().toString();
        toString2 = map2.toString();
        assertTrue(map2 instanceof TreeMap);
        assertEquals(toString1, toString2);

        map1.put("map1", map1);
        map1.put("map1a", map1);

        _exchange.getValue().put(map1);
        _exchange.clear().append("map1").store();
        _exchange.getValue().clear();
        _exchange.fetch();

        map2 = _exchange.getValue().get();
        toString1 = map1.toString();
        toString1a = _exchange.getValue().toString();
        toString2 = map2.toString();
        assertTrue(map2 instanceof TreeMap);
        assertEquals(toString1, toString2);

        System.out.println("- done");
    }

    @Test
    public void test3() throws PersistitException {
        System.out.print("test3 ");

        final Vehicle car1 = new Vehicle("c1", "Colt", 58, 4, 4, false);
        final Vehicle car2 = new Vehicle("c2", "Viper", 158, 4, 2, false);
        final Vehicle plane = new Vehicle("p1", "Piper", 220, 3, 4, true);

        // Make all object reference fields part of a circular graph.
        //
        car1.other1 = car2;
        car1.other2 = plane;
        car2.other1 = car1;
        car2.other2 = plane;
        plane.other1 = car1;
        plane.other2 = car2;

        final TreeMap map1 = new TreeMap();
        map1.put("c1", car1);
        map1.put("c2", car2);
        map1.put("plane", plane);

        _exchange.getValue().put(map1);
        _exchange.clear().append("map1").store();
        _exchange.getValue().clear();
        _exchange.fetch();

        Object map2 = _exchange.getValue().get();
        String toString1 = map1.toString();
        String toString1a = _exchange.getValue().toString();
        String toString2 = map2.toString();
        assertTrue(map2 instanceof TreeMap);
        assertEquals(toString1, toString2);

        map1.put("c1a", car1);
        map1.put("c2a", car2);
        map1.put("planea", plane);

        _exchange.getValue().put(map1);
        _exchange.clear().append("map1").store();
        _exchange.getValue().clear();
        _exchange.fetch();

        map2 = _exchange.getValue().get();
        toString1 = map1.toString();
        toString1a = _exchange.getValue().toString();
        toString2 = map2.toString();
        assertTrue(map2 instanceof TreeMap);
        assertEquals(toString1, toString2);

        map1.put("map1", map1);
        map1.put("map1a", map1);

        _exchange.getValue().put(map1);
        _exchange.clear().append("map1").store();
        _exchange.getValue().clear();
        _exchange.fetch();

        map2 = _exchange.getValue().get();
        toString1 = map1.toString();
        toString1a = _exchange.getValue().toString();
        toString2 = map2.toString();
        assertTrue(map2 instanceof TreeMap);
        assertEquals(toString1, toString2);
        System.out.println("- done");
    }

    @Test
    public void test4() throws PersistitException {
        System.out.print("test4 ");

        final Vehicle car1 = new Vehicle("c1", "Colt", 58, 4, 4, false);

        _exchange.getValue().directPut(_coder, car1, null);

        final Vehicle car2 = new Vehicle();
        _exchange.getValue().directGet(_coder, car2, Vehicle.class, null);

        final Vehicle car3 = (Vehicle) _exchange.getValue().directGet(_coder, Vehicle.class, null);

        assertEquals("Cars differ", car1, car2);
        assertEquals("Cars differ", car1, car3);

        final Vehicle car4 = new Vehicle();
        _exchange.getValue().directGet(_coder, car4, Vehicle.class, null);

        final Vehicle car5 = (Vehicle) _exchange.getValue().directGet(_coder, Vehicle.class, null);

        assertEquals("Cars differ", car1, car4);
        assertEquals("Cars differ", car1, car5);
        System.out.println("- done");
    }

    public boolean equals(final Object a, final Object b) {
        if ((a == null) || (b == null)) {
            return a == b;
        }
        if (a.getClass().isArray()) {
            if (!b.getClass().isArray()) {
                return false;
            }
            if (a.getClass().getComponentType() != b.getClass().getComponentType()) {
                return false;
            }
            if (Array.getLength(a) != Array.getLength(b)) {
                return false;
            }
            for (int index = Array.getLength(a); --index >= 0;) {
                if (!equals(Array.get(a, index), Array.get(b, index))) {
                    return false;
                }
            }
            return true;
        } else if (a.getClass().isPrimitive()) {
            return a == b;
        } else {
            return a.equals(b);
        }
    }

    public static void main(final String[] args) throws Exception {
        new ValueCoderTest2().initAndRunTest();
    }

    @Override
    public void runAllTests() throws Exception {
        test1();
        test2();
        test3();
    }
}
