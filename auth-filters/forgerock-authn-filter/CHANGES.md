1.5.0

Added new properties to MessageInfo map:
* org.forgerock.authentication.audit.trail - available to ServerAuthContext's only
* org.forgerock.authentication.audit.info - available to ServerAuthModule's both #validateRequest and #secureResponse
* org.forgerock.authentication.audit.session.id - available to "session" ServerAuthModule only
* org.forgerock.authentication.audit.failure.reason - available to ServerAuthModule #validateRequest only


New attribute will be set on http request:
* org.forgerock.authentication.request.id - will contain unique request id for the http request, for auditing purposes


New audit api, which will be given a JsonValue such as the following:
* for successful authentications:
 {
   "result": "SUCCESSFUL",
   "requestId": "...",
   "principal": "demo",
   "context": {
       ....
   },
   "sessionId": "...",
   "entries": [
     {
       "moduleId": "Session-JwtSessionModule",
       "result": "SUCCESSFUL",
       "info": {
         "...": "...",
         ...
       }
     },...
   ]
 }
 
* for failed authentications:
 {
    "result": "FAILED",
    "requestId": "...",
    "principal": "demo",
    "context": {
        ....
    },
    "entries": [
      {
        "moduleId": "Session-JwtSessionModule",
        "result": "FAILED",
        "reason": "...",
        "info": {
          "...": "...",
          ...
        }
      },...
    ]
  }
Note: principal may not be set for a failed authentication, it depends how the module failed, but it will be present on
  a successful authentication. Session id is only set by a "session" auth module and only in its #secureResponse method.
  
  
Old audit api (JaspiAuditLogger) can still be used and is supported, but deprecated. To use new AuditApi specify an 
init-param with the name 'audit-api-class' with a value of the fully qualified class name of the implementation class.


NOTE: Deprecated classes and methods will be removed in the next major release.