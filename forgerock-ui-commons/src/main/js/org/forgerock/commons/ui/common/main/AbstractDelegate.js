/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 ForgeRock AS. All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */

/*global $, define, _ */

/**
 * @author yaromin
 */
define("org/forgerock/commons/ui/common/main/AbstractDelegate", [
    "org/forgerock/commons/ui/common/util/Constants", 
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/main/ServiceInvoker"
], function(constants, configuration, serviceInvoker) {

    var obj = function AbstractDelegate(serviceUrl) {
        var baseRegex = new RegExp("^/" + constants.context + "\/([\\w/]*)"),
            baseEntity = serviceUrl.match(baseRegex);
        this.serviceUrl = serviceUrl;
        
        if (baseEntity && baseEntity.length > 1) {
            this.baseEntity = baseEntity[1];
        }
    };

    obj.prototype.serviceCall = function(callParams) {

        if (!callParams.hasOwnProperty('headers')) {
            callParams.headers = {};
        }
        if (configuration.hasOwnProperty('passwords') && configuration.passwords.hasOwnProperty('password')) {
            callParams.headers[constants.OPENIDM_HEADER_PARAM_REAUTH]=configuration.passwords.password;
        }
        if(callParams.serviceUrl) {
            callParams.url = callParams.serviceUrl + callParams.url;
        } else {
            callParams.url = this.serviceUrl + callParams.url;
        }
        return serviceInvoker.restCall(callParams);
    };

    obj.prototype.createEntity = function(object, successCallback, errorCallback) {
        console.debug("create entity");
        return this.serviceCall({url: "/?_action=create" , type: "POST", success: successCallback, error: errorCallback, data: JSON.stringify(object)});
    };

    obj.prototype.deleteEntity = function(id, successCallback, errorCallback) {
        console.debug("delete entity");
        var current = this;
        return this.readEntity(id, function(data) {
            var callParams = {url: "/" + id, type: "DELETE", success: successCallback, error: errorCallback };
            if(data._rev) {
                callParams.headers = [];
                callParams.headers["If-Match"] = '"' + data._rev + '"';
            }
            current.serviceCall(callParams);
        }, errorCallback);      

    };

    obj.prototype.readEntity = function(id, successCallback, errorCallback) {
        console.debug("get entity");
        return this.serviceCall({url: "/" + id, type: "GET", success: successCallback, error: errorCallback});
    };

    obj.prototype.updateEntity = function(objectParam, successCallback, errorCallback) {
        console.debug("update entity");
        var headers = {};
        
        if(objectParam._rev) {
            headers["If-Match"] = '"' + objectParam._rev + '"';
        } else {
            headers["If-Match"] = '"' + "*" + '"';
        }
        
        return this.serviceCall({url: "/" + objectParam._id,
            type: "PUT",
            success: successCallback, 
            error: errorCallback, 
            data: JSON.stringify(objectParam),
            headers: headers
        });
    };

    /**
     * Discovers differences between new and old object and invokes patch action only on attributes which are not equal.
     */
    obj.prototype.patchEntityDifferences = function(queryParameters, oldObject, newObject, successCallback, errorCallback, noChangesCallback) {
        console.debug("patching entity");

        var differences = this.getDifferences(oldObject, newObject);
        if(!differences.length){
            console.debug("No changes detected");
            if(noChangesCallback){
                noChangesCallback();
            }
            return;
        }
        return this.patchEntity(queryParameters, differences, successCallback, 
            _.bind(function () { 
                this.patchEntity(queryParameters, this.getDifferences(oldObject, newObject, "add"), successCallback, 
                                    errorCallback, noChangesCallback); 
            }, this), 
            noChangesCallback);
    };

    /**
     * Invokes patch action which modify only selected object attributes defined as PATCH action compatible JSON object {replace: "fieldname", value: "value" } 
     */
    obj.prototype.patchEntity = function(queryParameters, patchDefinition, successCallback, errorCallback, noChangesCallback, fields) {
        //simple transformation
        var i;
        for(i = 0; i < patchDefinition.length; i++) {
            if (typeof(patchDefinition[i].replace) !== "undefined") {
                patchDefinition[i].replace = "/" + patchDefinition[i].replace;
            }
            if (typeof(patchDefinition[i].add) !== "undefined") {
                patchDefinition[i].add = "/" + patchDefinition[i].add;
            }
            
        }
        return this.serviceCall({url: "/" + queryParameters.id + "?_action=patch", 
            type: "POST", 
            success: successCallback, 
            error: errorCallback, 
            data: JSON.stringify(patchDefinition),
            headers: {
                "If-Match": '"' + queryParameters.rev + '"'
            }
        });
    };

    /**
     *  Patches single attribute
     */
    obj.prototype.patchEntityAttribute = function(queryParameters, attributeName, newValue, successCallback, errorCallback, noChangesCallback, fields) {
        return this.patchEntity(queryParameters, [{replace: attributeName, value: newValue}], successCallback, errorCallback, noChangesCallback);
    };

    /**
     * Returns differences between new and old object as a PATCH action compatible JSON object
     */
    obj.prototype.getDifferences = function(oldObject, newObject, method) {
        var newValue, oldValue, field, fieldContents, result = [],patchCmd = {};
        if (!method) {
            method = "replace";
        }
        for ( field in newObject) {
            fieldContents = newObject[field];
            if ( typeof (fieldContents) !== "function") {
                newValue = newObject[field];
                oldValue = oldObject[field];
                if((newValue!=="" || oldValue) && newValue !== oldValue){
                    patchCmd = {};
                    patchCmd[method] = field;
                    patchCmd.value = newValue;
                    result.push( patchCmd );
                }
            }
        }
        return result;
    };

    return obj;
});