/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 ForgeRock AS. All rights reserved.
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

/*global define*/

// this module is merely a useful construct for identifying
// top-level modules which would be likely to need to embed
// within a single minified package, since they are pretty much
// always needed (even for the simplest of forgerock-ui apps)
define("org/forgerock/commons/ui/common/main", [
    "./main/AbstractView",
    "./components/BootstrapDialogView",
    "./main/ErrorsHandler",
    "./components/Footer",
    "./main/i18nManager",
    "./components/Messages",
    "./components/Navigation",
    "./main/ProcessConfiguration",
    "./main/Router",
    "./main/SessionManager",
    "./main/SpinnerManager",
    "./SiteConfigurator"
]);
