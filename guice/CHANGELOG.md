# 2.0.0

## Changes

### Major
* Updated Google Guice dependency from 3.0 to 4.0


# 1.2.0

### Minor
* Injector stage will now default to PRODUCTION. To change the stage use:
```java
InjectorConfiguration.setStage(Stage)
```
before calling:
```java
InjectorHolder.getInstance(Clazz)
```

### Bug Fixes
* Updated Servlet API dependency from 3.0.0 to 3.0.1