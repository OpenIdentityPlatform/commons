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

package org.forgerock.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.audit.AuditServiceImplTest.makeCreateRequest;
import static org.mockito.Mockito.*;

import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.ServiceUnavailableException;
import org.forgerock.services.context.Context;
import org.forgerock.services.context.RootContext;
import org.forgerock.util.promise.Promise;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@SuppressWarnings("javadoc")
public class AuditServiceProxyTest {

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldRejectNullDelegatePassedToConstructor() throws Exception {
        // when
        new AuditServiceProxy(null);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldRejectNullDelegatePassedToSetter() throws Exception {
        // given
        final AuditService auditService = mock(AuditService.class);
        final AuditServiceProxy auditServiceProxy = new AuditServiceProxy(auditService);

        // when
        auditServiceProxy.setDelegate(null);
    }

    @Test
    public void shouldShutdownOldAuditServiceDelegateWhenAssigningNewDelegate() throws ServiceUnavailableException {
        // given
        final AuditService initialAuditService = mock(AuditService.class);
        final AuditService newAuditService = mock(AuditService.class);
        final AuditServiceProxy auditServiceProxy = new AuditServiceProxy(initialAuditService);

        // when
        auditServiceProxy.setDelegate(newAuditService);

        // then
        verify(initialAuditService).shutdown();
    }

    @Test
    public void shouldPerformNoActionIfOldAuditServiceDelegateIsNewDelegate() throws ServiceUnavailableException {
        // given
        final AuditService initialAuditService = mock(AuditService.class);
        final AuditServiceProxy auditServiceProxy = new AuditServiceProxy(initialAuditService);
        reset(initialAuditService); // ignore AuditServiceProxy call to AuditService.startup

        // when
        auditServiceProxy.setDelegate(initialAuditService);

        // then
        verifyNoMoreInteractions(initialAuditService);
    }

    @Test
    public void shouldShutdownAuditServiceDelegateWhenShutdownIsCalled() throws ServiceUnavailableException {
        // given
        final AuditService initialAuditService = mock(AuditService.class);
        final AuditServiceProxy auditServiceProxy = new AuditServiceProxy(initialAuditService);

        // when
        auditServiceProxy.shutdown();

        // then
        verify(initialAuditService).shutdown();
    }

    @Test
    public void shouldObtainWriteLockBeforeUpdatingAuditServiceDelegate() throws Exception {
        final AuditService initialAuditService = mock(AuditService.class);
        final AuditService newAuditService = mock(AuditService.class);
        final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
        final AuditServiceProxy auditServiceProxy = new AuditServiceProxy(initialAuditService, rwLock);
        final CountDownLatch handleCreateLatch = new CountDownLatch(1);
        final CountDownLatch shutdownLatch = new CountDownLatch(1);
        final CreateRequest createRequest = makeCreateRequest();
        final Thread threadNeedingReadLock = new Thread(new Runnable() {
            @Override
            public void run() {
                auditServiceProxy.handleCreate(new RootContext(), createRequest);
            }
        });
        final Thread threadNeedingWriteLock = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    auditServiceProxy.setDelegate(newAuditService);
                } catch (ServiceUnavailableException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        when(initialAuditService.handleCreate(any(Context.class), eq(createRequest))).then(
                new Answer<Promise<ResourceResponse, ResourceException>>() {
                    @Override
                    public Promise<ResourceResponse, ResourceException> answer(InvocationOnMock invocationOnMock)
                            throws Throwable {
                        handleCreateLatch.countDown();
                        shutdownLatch.await();
                        return null;
                    }
                });

        // Make a call to mutableAuditService.handleCreate that will result in the Mockito Answer (above)
        // releasing the threadNeedingReadLock latch but blocking on the shutdownLatch (with the read lock held)
        threadNeedingReadLock.start();
        handleCreateLatch.await();
        assertThat(rwLock.getReadLockCount()).isEqualTo(1);

        // Make a call to mutableAuditService.setDelegate and prove that it blocks until the
        // write lock can be obtained (i.e. wait until the read lock is released)
        threadNeedingWriteLock.start();
        while (!rwLock.hasQueuedThread(threadNeedingWriteLock)) {
            Thread.sleep(10);
        }

        // Verify that shutdown() is not called until the write lock is obtained
        verify(initialAuditService, times(0)).shutdown();
        shutdownLatch.countDown();
        threadNeedingReadLock.join();
        threadNeedingWriteLock.join();
        verify(initialAuditService).shutdown();
    }

    @Test
    public void shouldObtainReadLockBeforeCallingProxiedMethodOfAuditServiceDelegate() throws Exception {
        final AuditService initialAuditService = mock(AuditService.class);
        final AuditService newAuditService = mock(AuditService.class);
        final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
        final AuditServiceProxy auditServiceProxy = new AuditServiceProxy(initialAuditService, rwLock);
        final CountDownLatch shutdownLatch = new CountDownLatch(1);
        final CountDownLatch handleCreateLatch = new CountDownLatch(1);
        final CreateRequest createRequest = makeCreateRequest();
        final Thread threadNeedingWriteLock = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    auditServiceProxy.setDelegate(newAuditService);
                } catch (ServiceUnavailableException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        final Thread threadNeedingReadLock = new Thread(new Runnable() {
            @Override
            public void run() {
                auditServiceProxy.handleCreate(new RootContext(), createRequest);
            }
        });
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocationOnMock)
                    throws Throwable {
                shutdownLatch.countDown();
                handleCreateLatch.await();
                return null;
            }
        }).when(initialAuditService).shutdown();

        // Make a call to mutableAuditService.setDelegate that will result in the Mockito Answer (above)
        // blocking on the CountDownLatch (with the write lock held)
        threadNeedingWriteLock.start();
        shutdownLatch.await();
        assertThat(rwLock.isWriteLocked()).isTrue();

        // Make a call to mutableAuditService.handleCreate and prove that it blocks until the
        // read lock can be obtained (i.e. wait until the write lock is released)
        threadNeedingReadLock.start();
        while (!rwLock.hasQueuedThread(threadNeedingReadLock)) {
            Thread.sleep(10);
        }

        // Verify that shutdown() is not called until the write lock is obtained
        verify(initialAuditService, times(0)).handleCreate(any(Context.class), any(CreateRequest.class));
        handleCreateLatch.countDown();
        threadNeedingWriteLock.join();
        threadNeedingReadLock.join();
        verify(newAuditService).handleCreate(any(Context.class), eq(createRequest));
    }

    @Test
    public void shouldObtainWriteLockBeforeShuttingDown() throws Exception {
        final AuditService initialAuditService = mock(AuditService.class);
        final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
        final AuditServiceProxy auditServiceProxy = new AuditServiceProxy(initialAuditService, rwLock);
        final CountDownLatch handleCreateLatch = new CountDownLatch(1);
        final CountDownLatch shutdownLatch = new CountDownLatch(1);
        final CreateRequest createRequest = makeCreateRequest();
        final Thread threadNeedingReadLock = new Thread(new Runnable() {
            @Override
            public void run() {
                auditServiceProxy.handleCreate(new RootContext(), createRequest);
            }
        });
        final Thread threadNeedingWriteLock = new Thread(new Runnable() {
            @Override
            public void run() {
                auditServiceProxy.shutdown();
            }
        });
        when(initialAuditService.handleCreate(any(Context.class), eq(createRequest))).then(
                new Answer<Promise<ResourceResponse, ResourceException>>() {
                    @Override
                    public Promise<ResourceResponse, ResourceException> answer(InvocationOnMock invocationOnMock)
                            throws Throwable {
                        handleCreateLatch.countDown();
                        shutdownLatch.await();
                        return null;
                    }
                });

        // Make a call to mutableAuditService.handleCreate that will result in the Mockito Answer (above)
        // blocking on the CountDownLatch (with the read lock held)
        threadNeedingReadLock.start();
        handleCreateLatch.await();
        assertThat(rwLock.getReadLockCount()).isEqualTo(1);

        // Make a call to mutableAuditService.shutdown and prove that it blocks until the
        // write lock can be obtained (i.e. wait until the read lock is released)
        threadNeedingWriteLock.start();
        while (!rwLock.hasQueuedThread(threadNeedingWriteLock)) {
            Thread.sleep(10);
        }

        // Verify that shutdown() is not called until the write lock is obtained
        verify(initialAuditService, times(0)).shutdown();
        shutdownLatch.countDown();
        threadNeedingReadLock.join();
        threadNeedingWriteLock.join();
        verify(initialAuditService).shutdown();
    }

    @Test
    public void shouldBlockProxiedMethodOfAuditServiceDelegateUntilShutdownCompletes() throws Exception {
        final AuditService initialAuditService = mock(AuditService.class);
        final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
        final AuditServiceProxy auditServiceProxy = new AuditServiceProxy(initialAuditService, rwLock);
        final CountDownLatch shutdownLatch = new CountDownLatch(1);
        final CountDownLatch handleCreateLatch = new CountDownLatch(1);
        final CreateRequest createRequest = makeCreateRequest();
        final Thread threadNeedingWriteLock = new Thread(new Runnable() {
            @Override
            public void run() {
                auditServiceProxy.shutdown();
            }
        });
        final Thread threadNeedingReadLock = new Thread(new Runnable() {
            @Override
            public void run() {
                auditServiceProxy.handleCreate(new RootContext(), createRequest);
            }
        });
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocationOnMock)
                    throws Throwable {
                shutdownLatch.countDown();
                handleCreateLatch.await();
                return null;
            }
        }).when(initialAuditService).shutdown();

        // Make a call to mutableAuditService.shutdown that will result in the Mockito Answer (above)
        // blocking on the CountDownLatch (with the write lock held)
        threadNeedingWriteLock.start();
        shutdownLatch.await();
        assertThat(rwLock.isWriteLocked()).isTrue();

        // Make a call to mutableAuditService.handleCreate and prove that it blocks until the
        // read lock can be obtained (i.e. wait until the write lock is released)
        threadNeedingReadLock.start();
        while (!rwLock.hasQueuedThread(threadNeedingReadLock)) {
            Thread.sleep(10);
        }

        // Verify that shutdown() is not called until the write lock is obtained
        verify(initialAuditService, times(0)).handleCreate(any(Context.class), any(CreateRequest.class));
        handleCreateLatch.countDown();
        threadNeedingWriteLock.join();
        threadNeedingReadLock.join();
        verify(initialAuditService).handleCreate(any(Context.class), eq(createRequest));
        // NB: If initialAuditService weren't a mock, the above call would return ServiceUnavailableException
    }

    @Test
    public void shouldBlockProxiedMethodOfAuditServiceDelegateUntilStartupCompletes() throws Exception {
        final AuditService initialAuditService = mock(AuditService.class);
        final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
        final AuditServiceProxy auditServiceProxy = new AuditServiceProxy(initialAuditService, rwLock);
        final CountDownLatch startupLatch = new CountDownLatch(1);
        final CountDownLatch handleCreateLatch = new CountDownLatch(1);
        final CreateRequest createRequest = makeCreateRequest();
        final Thread threadNeedingWriteLock = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    auditServiceProxy.startup();
                } catch (ServiceUnavailableException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        final Thread threadNeedingReadLock = new Thread(new Runnable() {
            @Override
            public void run() {
                auditServiceProxy.handleCreate(new RootContext(), createRequest);
            }
        });
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocationOnMock)
                    throws Throwable {
                startupLatch.countDown();
                handleCreateLatch.await();
                return null;
            }
        }).when(initialAuditService).startup();

        // Make a call to mutableAuditService.startup that will result in the Mockito Answer (above)
        // blocking on the CountDownLatch (with the write lock held)
        threadNeedingWriteLock.start();
        startupLatch.await();
        assertThat(rwLock.isWriteLocked()).isTrue();

        // Make a call to mutableAuditService.handleCreate and prove that it blocks until the
        // read lock can be obtained (i.e. wait until the write lock is released)
        threadNeedingReadLock.start();
        while (!rwLock.hasQueuedThread(threadNeedingReadLock)) {
            Thread.sleep(10);
        }

        // Verify that shutdown() is not called until the write lock is obtained
        verify(initialAuditService, times(0)).handleCreate(any(Context.class), any(CreateRequest.class));
        handleCreateLatch.countDown();
        threadNeedingWriteLock.join();
        threadNeedingReadLock.join();
        verify(initialAuditService).handleCreate(any(Context.class), eq(createRequest));
        // NB: If initialAuditService weren't a mock, the above call would return ServiceUnavailableException
    }

    @Test(expectedExceptions = IllegalStateException.class,
            expectedExceptionsMessageRegExp =
                    "AuditServiceProxy should not be called from delegate shutdown or startup operations")
    public void shouldRejectReentrantCallsToProxyWhileUpdatingDelegate() throws Exception {
        // given
        final AuditService initialAuditService = mock(AuditService.class);
        final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
        final AuditServiceProxy auditServiceProxy = new AuditServiceProxy(initialAuditService, rwLock);
        final CreateRequest createRequest = makeCreateRequest();
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocationOnMock)
                    throws Throwable {
                auditServiceProxy.handleCreate(new RootContext(), createRequest);
                return null;
            }
        }).when(initialAuditService).shutdown();

        // when
        auditServiceProxy.shutdown();

        // then
        // throw IllegalStateException
    }
}
