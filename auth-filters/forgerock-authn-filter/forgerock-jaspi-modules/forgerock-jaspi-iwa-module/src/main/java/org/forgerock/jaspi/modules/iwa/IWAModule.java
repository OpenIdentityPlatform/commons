package org.forgerock.jaspi.modules.iwa;

import org.forgerock.jaspi.modules.iwa.wdsso.WDSSO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.MessagePolicy;
import javax.security.auth.message.module.ServerAuthModule;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Principal;
import java.util.Map;

public class IWAModule implements ServerAuthModule {

    private static final Logger LOGGER = LoggerFactory.getLogger(IWAModule.class);

    private static final String IWA_FAILED = "iwa-failed";

    private CallbackHandler handler;
    private Map options;

    @Override
    public void initialize(MessagePolicy requestPolicy, MessagePolicy responsePolicy, CallbackHandler handler,
            Map options) throws AuthException {
        this.handler = handler;
        this.options = options;
    }

    @Override
    public Class[] getSupportedMessageTypes() {
        return new Class[]{HttpServletRequest.class, HttpServletResponse.class};
    }

    @Override
    public AuthStatus validateRequest(MessageInfo messageInfo, Subject clientSubject, Subject serviceSubject)
            throws AuthException {

        LOGGER.debug("IWAModule: validateRequest START");

        HttpServletRequest request = (HttpServletRequest)messageInfo.getRequestMessage();
        HttpServletResponse response = (HttpServletResponse)messageInfo.getResponseMessage();

        String httpAuthorization = request.getHeader("Authorization");

        try {
            if (httpAuthorization == null || "".equals(httpAuthorization)) {
                LOGGER.debug("IWAModule: Authorization Header NOT set in request.");

                response.addHeader("WWW-Authenticate", "Negotiate");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                try {
                    response.getWriter().write("{\"failure\":true,\"reason\":\"" + IWA_FAILED + "\"}");
                } catch (IOException e) {
                    LOGGER.debug("IWAModule: Error writing Negotiate header to Response. {}", e.getMessage());
                    throw new AuthException("Error writing to Response");
                }

                return AuthStatus.SEND_CONTINUE;
            } else {
                LOGGER.debug("IWAModule: Authorization Header set in request.");
                try {
                    final String username = new WDSSO().process(options, request);
                    LOGGER.debug("IWAModule: IWA successful with username, {}", username);

                    clientSubject.getPrincipals().add(new Principal() {
                        public String getName() {
                            return username;
                        }
                    });
                } catch (Exception e) {
                    LOGGER.debug("IWAModule: IWA has failed. {}", e.getMessage());
                    throw new AuthException("IWA has failed");
                }

                return AuthStatus.SUCCESS;
            }
        } finally {
            LOGGER.debug("IWAModule: validateRequest END");
        }
    }

    @Override
    public AuthStatus secureResponse(MessageInfo messageInfo, Subject serviceSubject) throws AuthException {
        return AuthStatus.SEND_SUCCESS;
    }

    @Override
    public void cleanSubject(MessageInfo messageInfo, Subject subject) throws AuthException {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
