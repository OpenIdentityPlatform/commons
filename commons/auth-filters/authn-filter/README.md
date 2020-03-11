# Overview of Authentication Framework

## Async API Usage:

An **AsyncServerAuthContext** uses a **MessageContext** instance to store state for its own later use and
to pass additional information to other **AsyncServerAuthContext** and **AsyncServerAuthModule** instances
that are configured.

On the other hand an **AsyncServerAuthModule** uses a **MessageContextInfo** instance to store state for
its own later use and to pass additional information to other **AsyncServerAuthContext** and
**AsyncServerAuthModule** instances that are configured.

The reason for these two different objects is to hide information from the **AsyncServerAuthModule**
instances that is only application at the **AsyncServerAuthContext** level. A **MessageContext** is a
**MessageContextInfo** so no conversion needs to take place when passing a **MessageContext** into a call
to an **AsyncServerAuthModule**.

The authentication framework will create a new **MessageContext** instance for each incoming request to
process, which will be passed to the configured **AsyncServerAuthContext**.

---

### Example implementation of an AsyncServerAuthContext
```java
new AsyncServerAuthContext() {

    private final AsyncServerAuthModule module = ...;

    @Override
    public Promise<AuthStatus, AuthenticationException> validateRequest(MessageContext context,
            Subject clientSubject, Subject serviceSubject) {
        return module.validateRequest(context, clientSubject, serviceSubject);
    }

    @Override
    public Promise<AuthStatus, AuthenticationException> secureResponse(MessageContext context,
            Subject serviceSubject) {
        return module.secureResponse(context, serviceSubject);
    }

    @Override
    public Promise<Void, AuthenticationException> cleanSubject(MessageContext context, Subject clientSubject) {
        return module.cleanSubject(context, clientSubject);
    }
};
```

### Example implementation of an AsyncServerAuthModule
```java
new AsyncServerAuthModule() {

    @Override
    public String getModuleId() {
        return "MyFirstModule";
    }

    @Override
    public Promise<Void, AuthenticationException> initialize(MessagePolicy requestPolicy,
            MessagePolicy responsePolicy, CallbackHandler handler, Map<String, Object> options) {
        final PromiseImpl<Void, AuthenticationException> promise = PromiseImpl.create();
        ThreadPoolExecutor executor = ...;
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    // Perform some asynchronous processing
                    promise.handleResult(null);
                } catch (Exception e) {
                    promise.handleError(new AuthenticationException("Failed to initialize module", e));
                }
            }
        });
        return promise;
    }

    @Override
    public Collection<Class<?>> getSupportedMessageTypes() {
        HashSet<Class<?>> supportedMessageTypes = new HashSet<Class<?>>();
        supportedMessageTypes.add(Request.class);
        supportedMessageTypes.add(Response.class);
        return supportedMessageTypes;
    }

    @Override
    public Promise<AuthStatus, AuthenticationException> validateRequest(MessageContextInfo messageInfo,
            Subject clientSubject, Subject serviceSubject) {
        final PromiseImpl<AuthStatus, AuthenticationException> promise = PromiseImpl.create();
        ThreadPoolExecutor executor = ...;
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    // Perform some asynchronous processing
                    promise.handleResult(AuthStatus.SUCCESS);
                } catch (Exception e) {
                    promise.handleError(new AuthenticationException("Failed to validate request", e));
                }
            }
        });
        return promise;
    }

    @Override
    public Promise<AuthStatus, AuthenticationException> secureResponse(MessageContextInfo messageInfo,
            Subject serviceSubject) {
        final PromiseImpl<AuthStatus, AuthenticationException> promise = PromiseImpl.create();
        ThreadPoolExecutor executor = ...;
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    // Perform some asynchronous processing
                    promise.handleResult(AuthStatus.SEND_SUCCESS);
                } catch (Exception e) {
                    promise.handleError(new AuthenticationException("Failed to secure response", e));
                }
            }
        });
        return promise;
    }

    @Override
    public Promise<Void, AuthenticationException> cleanSubject(MessageContextInfo messageInfo,
            Subject clientSubject) {
        final PromiseImpl<Void, AuthenticationException> promise = PromiseImpl.create();
        ThreadPoolExecutor executor = ...;
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    // Perform some asynchronous processing
                    promise.handleResult(null);
                } catch (Exception e) {
                    promise.handleError(new AuthenticationException("Failed to clean subject", e));
                }
            }
        });
        return promise;
    }
};
```

---
---

## JASPI Wrapper Usage

The **JaspiAdapter** class is intended to be used internally by the authentication framework for 
adapting JASPI auth contexts and modules. The authentication framework will offer utility methods 
for creating and configuring an instance of the authentication framework and these utility methods
will be overloaded so as they can accept either asynchronous and synchronous auth contexts/modules.

---

### Example of adapting a JASPI **ServerAuthContext** to an **AsyncServerAuthContext**
```java
ServerAuthContext authContext = ...;
AsyncServerAuthContext asyncAuthContext = JaspiAdapters.adapt(authContext);
```

### Example of adapting a JASPI **ServerAuthModule** to an **AsyncServerAuthModule**
```java
ServerAuthModule authModule = ...;
AsyncServerAuthModule asyncAuthModule = JaspiAdapters.adapt(authModule);
```

### Example of adapting a JASPI **AuthException** to an **AuthenticationException**
```java
AuthException authException = ...;
AuthenticationException authenticationException = JaspiAdapters.adapt(authException);
```

### Example of adapting a **MessageContextInfo** to a JASPI **MessageInfo**
```java
MessageContextInfo messageContextInfo = ...;
MessageInfo messageInfo = JaspiAdapters.adapt(messageContextInfo);
```

---
---

## Authentication Framework Configuration

The **AuthenticationFilter** class is the entry point into both configuring the Authentication 
Framework and for protecting resources. 

```java
AuthenticationFilter authenticationFilter = AuthenticationFilter.builder()
        .logger(logger)
        .auditApi(auditApi)
        .serviceSubject(serviceSubject)
        .responseHandler(responseHandler)
        .sessionModule(
                configureModule(sessionAuthModule)
                        .requestPolicy(sessionAuthModuleRequestPolicy)
                        .responsePolicy(sessionAuthModuleResponsePolicy)
                        .callbackHandler(sessionAuthModuleHandler)
                        .withSettings(sessionAuthModuleSettings))
        .authModules(
                configureModule(authModuleOne)
                        .requestPolicy(authModuleOneRequestPolicy)
                        .responsePolicy(authModuleOneResponsePolicy)
                        .callbackHandler(authModuleOneHandler)
                        .withSettings(authModuleOneSettings),
                configureModule(authModuleTwo)
                        .requestPolicy(authModuleTwoRequestPolicy)
                        .responsePolicy(authModuleTwoResponsePolicy)
                        .callbackHandler(authModuleTwoHandler)
                        .withSettings(authModuleTwoSettings))
        .build();
        
authenticationFilter.filter(context, request, handler);        
```

---

The Authentication Framework can be configured with zero 
or one "session" authentication modules and list of zero or more authentication modules. 

A "session" authentication module differs from a normal authentication module only in intent, 
meaning a "session" authentication module, if configured, always validates the request first and 
will always secure the response if the request is successfully authenticated.

The framework processes request by first invoking the "session" authentication module and only 
proceeding to the authentication module list if the "session" module fails. Similarly each module 
from the list of authentication modules is invoked in order and the next module is only invoked if 
the previous module failed. If no modules succeed in authenticating the request then 
authentication fails and an unauthenticated response is sent to the client.

If the "session" module or any of the authentication modules succeed in authenticating the request 
then the downstream resource is called.

Once the downstream resource has handled the request and returned a response, the response is then 
handed to the authentication module that successfully authenticated the request, then the "session" 
module gets the chance to add any session information to the response, which can then be used on 
subsequent requests to avoid having to go through the whole authentication process which may have 
to include credentials to be sent on the request as well.

**Note:** Only the module that successfully authenticates the request (and the "session" module) 
gets to secure the response, not the entire list of authentication modules. 

---

The Authentication Filter also performs audit logging on each request detailing which modules 
attempted to authenticate the request and which succeed or failed. In addition each successfully 
authenticated request gets a unique ID which is logged with the audit record.

If a "session" module has been configured then the module has the opportunity to set a unique 
session id on the audit record, when it secures a response. Then each subsequent request that
the "session" module successfully authenticates it can set the same session id onto the audit
record and then a set of requests can be tied together and back to the request that first resulted
in the session being started.

Each authentication module ("session" or otherwise) always has the chance to add additional audit 
information to the audit record and also, in the case of an module failed attempt to authenticate 
the request, the module can set detail about the cause/reason of the failure on the audit record.