/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2015 ForgeRock AS. All rights reserved.
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

/*global define, window */

define("org/forgerock/commons/ui/common/components/Navigation", [
    "jquery",
    "underscore",
    "backbone",
    "org/forgerock/commons/ui/common/main/AbstractConfigurationAware",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/ViewManager",
    "org/forgerock/commons/ui/common/main/Router"
], function($, _, Backbone, AbstractConfigurationAware, AbstractView, conf, constants, eventManager, viewManager, router) {
    var obj = new AbstractConfigurationAware();

    obj.init = function(callback) {
        var Navigation = AbstractView.extend({

            element: "#menu",
            template: "templates/common/NavigationTemplate.html",
            noBaseTemplate: true,
            data: {},

            events: {
                "click a.inactive": "disableLink",
                "click .event-link:not(.inactive)": "fireEvent"
            },
            disableLink: function(e){
                e.preventDefault();
            },

            fireEvent: function(e){
                e.preventDefault();
                if (e.currentTarget.dataset.event){
                    eventManager.sendEvent(e.currentTarget.dataset.event);
                }
            },
            render: function(args, callback) {
                // The user information is shown at the top of the userBar widget,
                // but it is stored in different ways for different products.
                if (conf.loggedUser) {
                    if (conf.loggedUser.userName) {
                        this.data.username = conf.loggedUser.userName; //idm
                    } else if (conf.loggedUser.cn) {
                        this.data.username = conf.loggedUser.cn; //am
                    } else {
                        this.data.username = conf.loggedUser._id; //fallback option
                    }

                    this.data.userBar = _.map(obj.configuration.userBar, function (link) {
                        if (_.has(link, "i18nKey")) {
                            link.label = $.t(link.i18nKey);
                        }
                        return link;
                    });
                }

                this.reload();
                this.parentRender(callback);
            },


            addLinks: function(linkName) {
                var urlName,
                    subUrl,
                    subUrlName,
                    baseActive,
                    navObj;


                for (urlName in obj.configuration.links[linkName].urls) {

                    navObj = obj.configuration.links[linkName].urls[urlName];
                    baseActive = this.isCurrent(navObj.url) || this.isCurrent(navObj.baseUrl) || this.childIsCurrent(navObj.urls);

                    this.data.topNav.push(this.buildNavElement(navObj, baseActive));

                    // none dropdown menus display as submenus and only render for the baseActive.
                    if (navObj.dropdown !== true){

                        if (baseActive && navObj.urls) {
                            for (subUrlName in navObj.urls) {
                                subUrl = navObj.urls[subUrlName];
                                this.data.subNav.push(this.buildNavElement(subUrl, this.isCurrent(subUrl.url)));
                            }

                            //Added to provide reference for responsive design submenus to appear in the correct location.
                            this.data.topNav[this.data.topNav.length - 1].subNav = this.data.subNav;
                        }
                    }
                }
            },

            buildNavElement: function (navObj, active) {
                var self = this,
                    subs = [],
                    navElement = {
                        key: navObj.name,
                        title: $.t(navObj.name),
                        icon: navObj.icon
                    };

                if (active) {
                    navElement.active = active;
                }
                if (navObj.url) {
                    navElement.hashurl = navObj.url;
                } else if (navObj.event) {
                    navElement.event = navObj.event;
                }
                if (navObj.divider) {
                    navElement.divider = navObj.divider;
                }
                if (navObj.cssClass) {
                    navElement.cssClass = navObj.cssClass;
                }

                if (navObj.dropdown === true) {
                    navElement.dropdown = true;
                     _.each(navObj.urls, function(obj){
                        subs.push(self.buildNavElement(obj));
                    });

                    navElement.urls = subs;
                }

                return navElement;
            },

            childIsCurrent: function(urls) {
                var urlName;
                for (urlName in urls) {
                    if (this.isCurrent(urls[urlName].url)) {
                        return true;
                    }
                }
                return false;
            },

            isCurrent: function(urlName) {
                var fromHash, afterHash = window.location.href.split('#')[1];
                if (afterHash) {
                    fromHash = "#" + afterHash;
                } else {
                    fromHash = "#/";
                }
                return fromHash.indexOf(urlName) !== -1;
            },

            clear: function() {
                this.data.topNav = [];
                this.data.subNav = [];
            },

            reload: function() {
                this.clear();

                var link, linkName;

                for(linkName in obj.configuration.links) {
                    link = obj.configuration.links[linkName];

                    if(!link.role){
                        this.addLinks(linkName);
                        return;
                    } else if (link.role && conf.loggedUser && _.contains(conf.loggedUser.roles, link.role)) {
                        this.addLinks(linkName);
                        return;
                    }
                }
            }
        });

        obj.navigation = new Navigation();
        obj.navigation.render(null, callback);
    };

    obj.reload = function() {
        if(obj.navigation) {
            obj.navigation.render();
        }
    };

    obj.addUserBarLink = function (link, position) {
        if (!_.find(obj.configuration.userBar, function (ub) {
                return ub.id === link.id;
            })) {

            if (position === "top") {
                obj.configuration.userBar.unshift(link);
            } else {
                obj.configuration.userBar.push(link);
            }
        }
    };

    return obj;
});
