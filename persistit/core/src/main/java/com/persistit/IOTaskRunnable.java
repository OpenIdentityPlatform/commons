/**
 * Copyright 2011-2012 Akiban Technologies, Inc.
 *
 * Portions Copyrighted 2026 3A Systems, LLC.
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

import com.persistit.exception.PersistitException;
import com.persistit.util.Util;

/**
 * Base class for the background threads that perform various IO tasks.
 * 
 * @author peter
 * 
 */
abstract class IOTaskRunnable implements Runnable {

    private final static long THREAD_DEATH_WAIT_INTERVAL = 5000;

    protected final Persistit _persistit;

    private volatile Thread _thread;

    private boolean _stopped;

    private volatile boolean _crashed;

    private boolean _notified;

    private long _pollInterval;

    private int _exceptionCount = 0;

    private Exception _lastException;

    protected IOTaskRunnable(final Persistit persistit) {
        _persistit = persistit;
    }

    protected void start(final String name, final long pollInterval) {
        _pollInterval = pollInterval;
        _thread = new Thread(this, name);
        _thread.start();
    }

    Thread getThread() {
        return _thread;
    }

    synchronized void kick() {
        if (!_notified) {
            _notified = true;
            notify();
        }
    }

    public final synchronized long getPollInterval() {
        return _pollInterval;
    }

    public final synchronized void setPollInterval(final long pollInterval) {
        _pollInterval = pollInterval;
        kick();
    }

    public final synchronized Exception getLastException() {
        return _lastException;
    }

    public final synchronized int getExceptionCount() {
        return _exceptionCount;
    }

    synchronized boolean lastException(final Exception exception) {
        _exceptionCount++;
        if (_lastException == null || exception == null || !_lastException.getClass().equals(exception.getClass())) {
            _lastException = exception;
            return true;
        } else {
            return false;
        }
    }

    protected synchronized boolean isStopped() {
        final Thread thread = _thread;
        if (thread == null || !thread.isAlive()) {
            return true;
        }
        return _stopped;
    }

    protected synchronized void setStopped() {
        _stopped = true;
    }

    void join(final long millis) throws InterruptedException {
        if (_thread != null) {
            _thread.join(millis);
        }
    }

    // Use only for tests. Simulates an abrupt crash of the background thread.
    //
    // Thread.stop() was removed from the JDK (it throws NoSuchMethodError on
    // recent releases), so instead we raise a "crashed" flag and interrupt the
    // thread. The run() loop observes the flag and returns immediately without
    // performing its normal clean shutdown, reproducing the abandon-in-place
    // behavior the recovery tests rely on.
    protected void crash() {
        final Thread thread = _thread;
        _crashed = true;
        setStopped();
        for (int count = 0; (thread != null && thread.isAlive()); count++) {
            if (count > 0) {
                _persistit.getLogBase().crashRetry.log(count, _thread.getName());
            }
            thread.interrupt();
            try {
                thread.join(THREAD_DEATH_WAIT_INTERVAL);
            } catch (final InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void run() {

        while (true) {
            synchronized (this) {
                _notified = false;
            }
            if (_crashed) {
                /*
                 * A simulated crash abandons work in place: return without a
                 * clean shutdown (no flush, no closeSession) to mimic the old
                 * Thread.stop() behavior relied on by the recovery tests.
                 */
                return;
            }
            try {
                /*
                 * Unit tests use a negative poll interval to prevent processing
                 * here
                 */
                if (getPollInterval() < 0) {
                    Util.spinSleep();
                } else {
                    runTask();
                }
            } catch (final Exception e) {
                if (lastException(e)) {
                    _persistit.getLogBase().exception.log(e);
                }
            }

            final long lastCycleTime = System.currentTimeMillis();
            synchronized (this) {
                if (shouldStop()) {
                    _stopped = true;
                    kick();
                    break;
                }

                while (!shouldStop() && !_crashed) {
                    final long pollInterval = pollInterval();
                    if (_notified && pollInterval >= 0) {
                        break;
                    }
                    final long waitTime = pollInterval < 0 ? Persistit.SHORT_DELAY : lastCycleTime + pollInterval
                            - System.currentTimeMillis();

                    if (waitTime <= 0) {
                        break;
                    }

                    try {
                        wait(waitTime);
                    } catch (final InterruptedException ie) {
                        _persistit.getLogBase().exception.log(ie);
                        break;
                    }
                }
            }
        }
        try {
            _persistit.closeSession();
        } catch (final PersistitException e) {
            _persistit.getLogBase().exception.log(e);
        }
    }

    static void crash(final IOTaskRunnable task) {
        if (task != null) {
            task.crash();
        }
    }

    protected long pollInterval() {
        return getPollInterval();
    }

    protected abstract boolean shouldStop();

    protected abstract void runTask() throws Exception;

}
