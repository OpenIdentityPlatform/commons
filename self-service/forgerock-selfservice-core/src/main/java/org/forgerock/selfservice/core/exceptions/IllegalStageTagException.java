package org.forgerock.selfservice.core.exceptions;

/**
 * Exception that represents an unknown stage tag.
 *
 * @since 0.1.0
 */
public final class IllegalStageTagException extends IllegalArgumentException {

    /**
     * Creates a new exception instance.
     *
     * @param stageTag
     *         represents the illegal/invalid stage tag
     */
    public IllegalStageTagException(String stageTag) {
        super("Unknown stage tag " + stageTag);
    }

}
