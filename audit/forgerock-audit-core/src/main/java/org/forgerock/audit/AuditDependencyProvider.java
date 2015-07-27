package org.forgerock.audit;

/**
 * Created by brmiller on 7/26/15.
 */
public interface AuditDependencyProvider {
    Object getDependency(Class<?> clazz) throws ClassNotFoundException;
}
