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

/*global define, $, _, Backbone, window */

/**
 * @author mbilski
 */

define("org/forgerock/commons/ui/common/components/Navigation", [
    "underscore",
    "backbone",
    "org/forgerock/commons/ui/common/main/AbstractConfigurationAware",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/util/Constants"
], function(_, Backbone, AbstractConfigurationAware, AbstractView, conf, constants) {
    var obj = new AbstractConfigurationAware();
    
    obj.init = function() {
        var Navigation = AbstractView.extend({
            
            element: "#menu",
            template: "templates/common/NavigationTemplate.html",
            noBaseTemplate: true,
            
            render: function() {
                this.parentRender(_.bind(function() {
                    this.reload();
                }, this));
            },
            
            addLinks: function(linkName) {
                var url, urlName, subUrl, subUrlName;
                
                for(urlName in obj.configuration.links[linkName].urls) {
                    url = obj.configuration.links[linkName].urls[urlName];
                    
                    if (this.isCurrent(url.url) || this.isCurrent(url.baseUrl)) {
                        this.addLink(url.name, url.url, true);
                        
                        if (url.urls) {
                            this.$el.append('<div id="submenu" class="submenubar"><ul>');
                            for(subUrlName in url.urls) {
                                subUrl = url.urls[subUrlName];
                                this.addSubLink(subUrl.name, subUrl.url, this.isCurrent(subUrl.url));
                            }
                            this.$el.append('</ul></div>');
                        }
                        
                    } else {
                        this.addLink(url.name, url.url, false);
                    }
                    
                }
            },
            
            addLink: function(name, url, isActive) {
                var newLink = this.$el.find("ul:first").append('<li><a href="'+url+'">'+ $.t(name) +'</a></li>');
                if (isActive) {
                    $(newLink).find("li:last").addClass('active');
                }
            },
            
            addSubLink: function(name, url, isActive) {
                var newSubLink = this.$el.find("ul:last").append('<li><a href="'+url+'">'+ $.t(name) +'</a></li>');
                if (isActive) {
                    $(newSubLink).find("li:last").addClass('active');
                }
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
                $("#menu li").remove();
                $("#submenu").remove();
            },
            
            reload: function() {
                this.clear();
                
                var link, linkName;
                
                for(linkName in obj.configuration.links) {
                    link = obj.configuration.links[linkName];
                    
                    if(link.role && conf.loggedUser && _.contains(conf.loggedUser.roles.split(","), link.role)) {
                        this.addLinks(linkName);
                        return;
                    } else if(!link.role) {
                        this.addLinks(linkName);
                        return;
                    }
                }
            }
        });
                
        obj.navigation = new Navigation();
        obj.navigation.render();
    };
    
    obj.reload = function() {
        if(obj.navigation) {
            obj.navigation.reload();
        }
    };

    return obj;
});