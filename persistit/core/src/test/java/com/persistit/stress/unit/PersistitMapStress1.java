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

package com.persistit.stress.unit;

import com.persistit.PersistitMap;
import com.persistit.Value;
import com.persistit.util.ArgParser;

import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;

public class PersistitMapStress1 extends StressBase {

    private final static String[] ARGS_TEMPLATE = { "op|String:Cwrtd|Operations to perform",
            "repeat|int:1:0:1000000000|Repetitions", "count|int:10000:0:1000000000|Number of nodes to populate",
            "size|int:30:1:20000|Size of each data value", "splay|int:1:1:1000|Splay",
            "_flag|t|Timing test on TreeMap instead of PersistitMap", };

    int _size;
    int _splay;
    String _opflags;

    SortedMap _dm1;
    SortedMap _dm2;

    long _timeWrite;
    long _timeRead;
    long _timeIter;
    long _timeRemove;

    public PersistitMapStress1(final String argsString) {
        super(argsString);
    }

    @Override
    public void setUp() {
        _ap = new ArgParser("com.persistit.stress.PersistitMapStress2", _args, ARGS_TEMPLATE).strict();
        _splay = _ap.getIntValue("splay");
        _opflags = _ap.getStringValue("op");
        _size = _ap.getIntValue("size");
        _repeatTotal = _ap.getIntValue("repeat");
        _total = _ap.getIntValue("count");

        try {
            // Exchange with Thread-private Tree
            _ex = getPersistit().getExchange("persistit", _rootName + _threadIndex, true);
        } catch (final Exception ex) {
            handleThrowable(ex);
        }

        _ex.to("dmtest").append(_rootName + _threadIndex);

        if (_ap.isFlag('t')) {
            _dm1 = new TreeMap();
            _dm2 = new PersistitMap(_ex);
        } else {
            _dm1 = new PersistitMap(_ex);
            _dm2 = new TreeMap();
        }
    }

    private long ts() {
        return System.currentTimeMillis();
    }

    @Override
    public void executeTest() {
        final Value value1 = _ex.getValue();
        final Value value2 = new Value(getPersistit());

        if (_opflags.indexOf('C') >= 0) {
            setPhase("C");
            try {
                _dm1.clear();
                addWork(1);

            } catch (final Exception e) {
                handleThrowable(e);
            }
        }

        for (_repeat = 0; (_repeat < _repeatTotal || isUntilStopped()) && !isStopped(); _repeat++) {

            long ts = ts();
            long tt;

            if (_opflags.indexOf('w') >= 0) {
                setPhase("w");
                for (_count = 0; (_count < _total) && !isStopped(); _count++) {
                    final int keyInteger = keyInteger(_count);

                    setupTestValue(_ex, _count, _size);
                    try {
                        _dm1.put(new Integer(keyInteger), _sb1.toString());
                        addWork(1);

                    } catch (final Exception e) {
                        handleThrowable(e);
                        break;
                    }
                }
                if (_dm1.size() != _total && !isStopped()) {
                    fail("PersistitMap.size()=" + _dm1.size() + " out of " + _total + " repetition=" + _repeat
                            + " in thread=" + _threadIndex);
                    break;
                }
                _timeWrite += (tt = ts()) - ts;
                ts = tt;

            }

            if (_opflags.indexOf('r') >= 0) {
                setPhase("r");
                for (_count = 0; (_count < _total) && !isStopped(); _count++) {
                    final int keyInteger = keyInteger(_count);
                    setupTestValue(_ex, _count, _size);
                    final String s1 = _sb1.toString();

                    try {
                        // fetch to a different Value object so we can compare
                        // with the original.
                        final String s2 = (String) _dm1.get(new Integer(keyInteger));
                        addWork(1);

                        compareStrings(s1, s2);
                    } catch (final Exception e) {
                        handleThrowable(e);
                    }
                }
                _timeRead += (tt = ts()) - ts;
                ts = tt;
            }

            if (_opflags.indexOf('t') >= 0) {
                setPhase("t");
                final Iterator itr = _dm1.keySet().iterator();

                for (_count = 0; (_count < (_total * 10)) && !isStopped(); _count++) {
                    try {
                        if (!itr.hasNext()) {
                            break;
                        }
                        itr.next();
                        addWork(1);

                    } catch (final Exception e) {
                        handleThrowable(e);
                    }
                }
                if (_count != _total && !isStopped()) {
                    fail("Traverse count=" + _count + " out of " + _total + " repetition=" + _repeat + " in thread="
                            + _threadIndex);
                    break;
                }
                _timeIter += (tt = ts()) - ts;
                ts = tt;
            }

            if (_opflags.indexOf('d') >= 0) {
                setPhase("d");
                for (_count = 0; (_count < _total) && !isStopped(); _count++) {
                    final int keyInteger = keyInteger(_count);
                    setupTestValue(_ex, _count, _size);
                    final String s1 = _sb1.toString();
                    try {
                        final String s2 = (String) _dm1.remove(new Integer(keyInteger));
                        addWork(1);

                        compareStrings(s1, s2);
                    } catch (final Exception e) {
                        handleThrowable(e);
                    }
                }

                //
                // Now verify that the interator has no members.
                //
                final Iterator itr = _dm1.keySet().iterator();

                for (_count = 0; (_count < (_total * 10)) && !isStopped(); _count++) {
                    try {
                        if (!itr.hasNext()) {
                            break;
                        }
                        itr.next();
                        addWork(1);

                    } catch (final Exception e) {
                        handleThrowable(e);
                    }
                }
                if (_count != 0 && !isStopped()) {
                    fail("Traverse count=" + _count + " when 0 were expected" + " repetition=" + _repeat
                            + " in thread=" + _threadIndex);
                    break;
                }
                _timeRemove += (tt = ts()) - ts;
                ts = tt;
            }

            if (_opflags.indexOf('D') >= 0) {
                setPhase("D");
                //
                // Now verify that the interator has no members.
                //
                final Iterator itr = _dm1.keySet().iterator();

                for (_count = 0; (_count < (_total * 10)) && !isStopped(); _count++) {
                    try {
                        if (!itr.hasNext()) {
                            break;
                        }
                        itr.next();
                        itr.remove();
                        addWork(1);

                    } catch (final Exception e) {
                        handleThrowable(e);
                    }
                }
                if (_dm1.size() != 0 && !isStopped()) {
                    fail("PersistitMap.size()= " + _dm1.size() + " when 0 were expected" + " repetition=" + _repeat
                            + " in thread=" + _threadIndex);
                    break;
                }
                _timeRemove += (tt = ts()) - ts;
                ts = tt;
            }

            if ((_opflags.indexOf('h') > 0) && !isStopped()) {
                setPhase("h");
                try {
                    Thread.sleep(1000);
                } catch (final Exception e) {
                }
            }
        }
    }

    int keyInteger(final int counter) {
        int keyInteger = (counter * _splay) % _total;
        if (keyInteger < 0) {
            keyInteger += _total;
        }
        return keyInteger;
    }

}
