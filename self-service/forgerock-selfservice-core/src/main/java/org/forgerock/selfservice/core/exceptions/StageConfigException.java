package org.forgerock.selfservice.core.exceptions;

/**
 * Represents some framework error around the use of progress stages and configs.
 *
 * @since 0.1.0
 */
public final class StageConfigException extends RuntimeException {

    /**
     * Creates an exception instance.
     *
     * @param message
     *         should detail the cause of the error
     */
    public StageConfigException(String message) {
        super(message);
    }

}
