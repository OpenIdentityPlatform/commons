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

/*global define */

define("org/forgerock/commons/ui/common/main/AbstractModel", [
    "underscore",
    "backbone",
    "org/forgerock/commons/ui/common/util/ObjectUtil",
    "org/forgerock/commons/ui/common/main/ServiceInvoker"
], function(_, Backbone, ObjectUtil, ServiceInvoker) {
    /**
     * @exports org/forgerock/commons/ui/common/main/AbstractModel
     */
    return Backbone.Model.extend({
        idAttribute: "_id",
        getMVCCRev : function () {
            return this.get("_rev") || "*";
        },
        sync: function (method, model, options) {
            switch (method) {
                case "create":
                    return ServiceInvoker.restCall(_.extend(
                        {
                            data: JSON.stringify(model.toJSON())
                        },
                        (function () {
                            if (model.has("id")) {
                                return {
                                    "type": "PUT",
                                    "headers": {
                                        "If-None-Match": "*"
                                    },
                                    "url": model.url + "/" + model.id
                                };
                            } else {
                                return {
                                    "type": "POST",
                                    "url": model.url + "?_action=create"
                                };
                            }
                        }()),
                        options
                    ));
                case "read":
                    return ServiceInvoker.restCall(_.extend(
                        {
                            "url" : model.url + "/" + model.id,
                            "type": "GET"
                        },
                        options
                    )).then(function (response) {
                        if (options.parse) {
                            model.set(model.parse(response, options));
                        } else {
                            model.set(response);
                        }
                        return model.toJSON();
                    });
                case "update":
                    return ServiceInvoker.restCall(_.extend(
                        {
                            "type": "PUT",
                            "data": JSON.stringify(model.toJSON()),
                            "url": model.url + "/" + model.id,
                            "headers": {
                                "If-Match": model.getMVCCRev()
                            }
                        },
                        options
                    ));
                case "patch":
                    return ServiceInvoker.restCall(_.extend(
                        {
                            "url" : model.url + "/" + model.id,
                            "type": "PATCH",
                            "data": JSON.stringify(ObjectUtil.generatePatchSet(model.toJSON(), model.previousAttributes())),
                            "headers": {
                                "If-Match": model.getMVCCRev()
                            }
                        },
                        options
                    ));
                case "delete":
                    return ServiceInvoker.restCall(_.extend(
                        {
                            "url" : model.url + "/" + model.id,
                            "type": "DELETE",
                            "headers": {
                                "If-Match": model.getMVCCRev()
                            }
                        },
                        options
                    ));
            }
        }
    });
});
