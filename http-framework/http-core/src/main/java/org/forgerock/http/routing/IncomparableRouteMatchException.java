package org.forgerock.http.routing;

/**
 * An exception which is thrown when two incompatible {@link RouteMatch}
 * instances are attempted to be compared.
 *
 * @since 1.0.0
 */
public class IncomparableRouteMatchException extends Exception {

    private static final long serialVersionUID = 4933718263312991528L;

    /**
     * Constructs a {@code IncomparableRouteMatchException} with the two
     * {@link RouteMatch} instance that caused the exception.
     *
     * @param firstRouteMatch The first {@code RouteMatch} instance.
     * @param secondRouteMatch The second {@code RouteMatch} instance.
     */
    public IncomparableRouteMatchException(RouteMatch firstRouteMatch, RouteMatch secondRouteMatch) {
        super(firstRouteMatch.getClass().toString() + " cannot be compared to "
                + secondRouteMatch.getClass().toString());
    }
}
