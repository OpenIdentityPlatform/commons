Introduction
============
ForgeRock UI (FR-UI) is designed to allow for a consistent pattern developing User Interfaces using modern web technologies. This documentation contains a list of technologies, basic application architecture and boiler plate information for creating different UI pieces in the FR-UI. 

Index
=====
  - [FR_UI Stack](#stack)
  - [FR_UI Contents](#contents) 
    - [forgerock-ui-commons](#contents_commons)  
    - [forgerock-ui-user](#contents_user)
    - [forgerock-ui-mock](#contents_mock)
  - [Launching](#launching)
  - [First Time Initialization](#init)
  - [Routes](#routes)
    - [Adding a New Route](#routes_new)
    - [Parameters](#routes_param)
  - [Views](#views)
    - [Creating a New View](#views_new)
    - [Parameters](#views_param)
  - [Templates](#templates)
    - [Create a New Template](#templates_new)
  - [Common Components](#common_components)
    - [Existing Common Components](#common_components_existing)
    - [How to Add a New Common Component](#common_components_how)
  - [Roles](#roles)
  - [Validation](#validation)
    - [How to Add a New Validator](#validation_how)
  - [Messages](#messages)
  - [Translations](#translations)
  - [Constants](#constants)
  - [Events](#events)
    - [Adding a New Event Listener](#events_listener)
    - [Parameters](#events_param)
  - [Error Handling](#error)
    - [Parameters](#error_param)
  - [Theme](#theme)

<h1 id="stack">FR-UI Stack</h1>

1. [Backbone.js](http://backbonejs.org/)
2. [Headlebars](http://handlebarsjs.com/)
3. [RequireJS](http://requirejs.org/)
4. [Underscore](http://underscorejs.org/)
5. [jQuery](http://jquery.com/)
6. [Less](http://lesscss.org/)
7. [Sinon](http://sinonjs.org/) - For the ui-mock project 
8. [Qunit](http://qunitjs.com/)

<h1 id="contents">FR-UI Contents</h1>

Framework consists of 3 modules: 

1. forgerock-ui-common
2. forgerock-ui-user
3. forgerock-ui-mock 

<h2 id="contents_commons">forgerock-ui-commons</h2>

This module contains core functionality. 

1. Common configuration files:
    * error handlers (_config/errorhandlers/CommonErrorHandlers.js_)
    * messages (_config/messages/CommonMessages.js_)
    * event configuration (_config/process/CommonConfig.js_)
    * validation (_config/validators/CommonValidators.js_)
    * routes configuration (_config/routes/CommonRoutesConfig.js_)
2. Various abstract objects definitions: 
    * abstract delegator (_org/forgerock/commons/ui/common/main/AbstractDelegate.js_)
    * errors handler (_org/forgerock/commons/ui/common/main/ErrorsHandler.js_)
    * event manager (_org/forgerock/commons/ui/common/main/EventManager.js_)
    * i18n manager (_org/forgerock/commons/ui/common/main/i18nManager.js_)
    * router (_org/forgerock/commons/ui/common/main/Router.js_)
3. Common views: 
    * login (_org/forgerock/commons/ui/common/LoginView.js_)
    * not found (_org/forgerock/commons/ui/common/NotFoundView.js_)
    * login dialog (_org/forgerock/commons/ui/common/LoginDialog.js_)
    * enable cookies (_org/forgerock/commons/ui/common/EnableCookiesView.js_)
    * user bar view (_org/forgerock/commons/ui/common/LoggedUserBarView.js_)
4. Common components: navigation, footer, dialog, breadcrumbs, etc.
    * navigation (_org/forgerock/commons/ui/common/Navigation.js_)
    * footer (_org/forgerock/commons/ui/common/Footer.js_)
    * dialog (_org/forgerock/commons/ui/common/Dialog.js_)
    * breadcrumbs (_org/forgerock/commons/ui/common/Breadcrumbs.js_)
5. Common templates: navigation, dialog, login, footer, 404, etc.
    * navigation (_resources/templates/common/NavigationTemplate.html_)
    * dialog (_resources/templates/common/DialogTemplate.html_)
    * login (_resources/templates/common/LoginTemplate.html_)
    * footer (_resources/templates/common/FooterTemplate.html_)
    * 404 (_resources/templates/common/404.html_)
6. Common utility files: cookie helper, date util, constants, etc.
    * cookie helper (_org/forgerock/commons/ui/common/util/CookieHelper.js_)
    * date util (_org/forgerock/commons/ui/common/util/DateUtil.js_)
    * constants (_org/forgerock/commons/ui/common/util/Constants.js_)

<h2 id="contents_user">forgerock-ui-user</h2>

This module contains end-user specific functionality 

1. User specific configuration: 
    * messages (_config/messages/UserMessages.js_)
    * event configuration (_config/process/UserConfig.js_)
    * routes configuration (_config/routes/UserRoutesConfig.js_)
    * validators (_config/validators/UserValidators.js_)
2. UserDelegate.js: contains user specific operations (login, logout,getUserById, getAllUsers, etc.). This is an abstract delegate as well 
as _org/forgerock/commons/ui/common/main/AbstractDelegate.js_ 
in the commons module and it is intended to be implemented with respect to specific end-project needs
3. User profile view (_org/forgerock/commons/ui/user/profile/UserProfileView.js_)
4. Resources: less files, images, localization file, user profile template

<h2 id="contents_mock">forgerock-ui-mock</h2>

This module is a mock project and it is intended to serve as an example 
for other projects (framework implementations). It is also intended for 
making development, debugging and testing of the FR-UI easier.

1. End-project specific configuration: 
    * routes (_config/routes/MockRoutesConfig.js_)
    * application configuration (_config/AppConfiguration.js_): to be implemented for every project; provides configuration for modules.
2. End-project specific implementations: delegates, helpers, utility files, views 
3. Resources: templates, index.html
4. Couple of helpers to enable running of mock project without web server.

<h1 id="launching">Launching</h1>

**Non-optimized**

For non-optimized version run mvn clean install inside forgerock-ui-mock module

For the non-optimized version, open it directly in firefox after that completes by opening forgerock-ui-mock/target/www/index.html.

You can also open the non-optimized version in any browser if you host it through a web server.

**Optimized**

For optimized version run with mvn clean install -Pproduction parameter.

If you use the optimized version, you should be able to open that same file directly with any browser.

**Login Credentials** 

Username: test

Password: test 

<h2 id="init">First Time Initialization </h2>

Upon the initial page load, root main.js file is executed: 
        
    <script data-main="main" src="libs/requirejs-X.X.X-min.js"></script>

All AMD (Asynchronous Module Definition) modules are preloaded upon the initial page load, via the main.js call to require(): 

    require([
        "underscore",
        ...,
        "org/forgerock/mock/ui/common/main",
        "org/forgerock/mock/ui/user/main",
        "org/forgerock/commons/ui/user/main",
        "org/forgerock/commons/ui/common/main",
        "config/main"
        ], 
        function (...) {
            eventManager.sendEvent(constants.EVENT_DEPENDECIES_LOADED);
        }
    );

This facilitates synchronous calls to require('module') within the code. 

_org/forgerock/mock/ui/common/main_ loads end-project specific modules implementations (delegates, theme manager)

_org/forgerock/mock/ui/user/main_ loads end-user specific modules: login view, registration view, etc.

_org/forgerock/commons/ui/user/main_ (provided by forgerock-ui-user module) loads user profile view 

_org/forgerock/commons/ui/common/main_ (provided by forgerock-ui-common module) loads various common files: helpers, utility files, common views, common components, abstract object definitions

_config/main_ loads all routes configuration files, messages, validators, error handlers, and application configuration

This call to require() also initiates the execution of the rest of the application, via the trigger of the **EVENT\_DEPENDECIES\_LOADED** event. 


This execution of the **EVENT\_DEPENDECIES\_LOADED** event causes several things to occur: 

1. Creates modules based off of configuration: **EVENT\_DEPENDECIES\_LOADED** handler fires **EVENT\_CONFIGURATION\_CHANGED** event which is used for notifying modules defined in _config/AppConfiguration.js_ file. These modules are being created and if they have already been created they are being notified of a change. 
**EVENT\_CONFIGURATION\_CHANGED** handler fires **updateConfigurationCallback()**, which processes configuration objects of those modules.
2. Defines handlers for events in configuration files (_config/process/UserConfig.js_, _config/process/CommonConfig.js_) got registered.
3. Starts basic components: **EVENT\_READ\_CONFIGURATION\_REQUEST** is fired. Site configuration is being retrieved and processed. After that **EVENT\_APP\_INTIALIZED** is triggered. The handler for this event is defined in _config/process/CommonConfig.js_ and it initializes basic components (_org/forgerock/commons/ui/common/components/Navigation.js_, _org/forgerock/commons/ui/common/main/Router.js_, _helpers_, _etc._)  and also checks if user is already logged in.

<h1 id="routes">Routes</h1>

Available top-level "page" requests are configured in the files found under _config/routes_.  They are broken down by the various modules - Common, User, and Mock: _config/routesCommonRoutesConfig.js_, _config/routesUserRoutesConfig.js_, and _config/routesMockRoutesConfig.js_.  Within each file are route identifiers, each of which specify the roles allowed to access them, along with a reference to the particular "view" used to render that page. 

<h2 id="routes_new">Adding a New Route</h2>
 
To add a new route, create end-project specific routes configuration file. _config/routes/MockRoutesConfig.js_ is used for the mock project and can be referred as an example for real project implementation.

<h2 id="routes_param">Parameters</h2>

**base** 

  * base view to render, need to specify this for a dialog

**url**

  * url identifier for a route (regexp allowed)

**role**

  * array of roles that allow navigating the route; if route is being accessed by somebody who doesn't have the specified role, **EVENT_UNAUTHORIZED** event is raised. If this parameter is not specified, the route can be navigated by anyone

**excluderole**

  * if user has the specified role, he is not allowed to navigate the route

**event**

  * specify event that should be raised when the route is being navigated to

**dialog**

  * name of the dialog module

**view**

  * name of the view module (specify one of the following: _event_, _dialog_, or _view_

**defaults**

  * default parameters for the route

**pattern**

  * url pattern

**forceUpdate**

  * forces view initialization

<h1 id="views">Views</h1>

The apperance for each page is based on Backbone views. When the Router matches route, it calls render() method of that route's view. These views inherit from the org/forgerock/commons/ui/common/main/AbstractView.js "class". As such, each view has an associated template and DOM element reference, along with a render() function used to put the two together.

There are some views that are already defined in the FR-UI: login, not found, user profile, enable cookies, user bar, login dialog.

<h2 id="views_new">Creating a New View</h2>

Firstly, create new view under the corresponding folder, e.g. new view for the user in the mock project would go into the _org/forgerock/mock/ui/user/_ folder.

There are a couple of ways to create a new view: 

1. For a completely new view extend directly from _org/forgerock/commons/ui/common/main/AbstractView.js_:

        var NameOfTheNewView = AbstractView.extend({});
        return new NameOfTheNewView();

2. For a new dialog extend from _org/forgerock/commons/ui/common/components/Dialog.js_:

        var NameOfTheNewDialog = Dialog.extend({});
        return new NameOfTheNewDialog();

3. Extend from an existing view from FR-UI framework (e.g. base profile view _org/forgerock/commons/ui/user/profile/UserProfileView.js_, base login view _org/forgerock/commons/ui/common/LoginView.js_)

Be sure to specify template, events, render() or other basic methods and properties if needed.

Secondly, include the newly created view as dependency into the closest main.js file. 

Next, define corresponding route for the view in the routes configuration file (e.g. config/routes/MockRoutesConfig.js for the mock project).

**Base templates need to line up between dialog / base. If they do not it causes sync issues and errors in the display**

<h2 id="views_param">Parameters:</h2>

**element**
  
  * the dom element to replace/append to

**baseTemplate**
  
**template**

**contentTemplate** 
 
**mode**
    
  * replace/append for the view

**formLock** 

**data**

  * Method for passing data to for view use (for example use with handlebars)

**events** 

  * Events to execute in the view

**actions**

  * Creates a list components to place in the footer of a dialog
    1. Type - The type of component
    2. Name - Name of the form field (currently only generates an input type button)

**It would be a best practice to include all of your events for a specific view in the events property. This will keep things organized and reduce code in the view.** 

**In the future for consistency sake the data property should be used to populate any data displayed by the view on the initial load. This will keep the view file cleaner and allow for the view/template to handle the brunt of the initial display.** 


<h1 id="templates">Templates</h1>

Templates are (at the moment) created with handlebars.

<h2 id="templates_new">Create a New Template</h2> 

New template should be placed under _resources/templates_ folder, e.g. UserRegistrationTemplate.html is placed under _resources/templates/mock_ folder of the mock project.

Specify the name of the template as template property for a regular view and as  contentTemplate property for a dialog.

**If you are using the mock project make sure you add your templates to the Data.js file**

<h1 id="common_components">Common Components</h1>

Common components - separate views which represent common page sections such as Navigation, Breadcrumbs, Footer, Tables, Popup, Messages.
All Common components are located in the _/org/forgerock/commons/ui/common/_ 
components folder.

<h2 id="common_components_existing">Existing Common Components</h2>

**Breadcrumbs (Breadcrumbs.js):**

This component adds list of links to the **#nav-content** html element.
Initialization of this component happens during app initialization (**EVENT\_APP\_INTIALIZED** event handler in the _/config/process/CommonConfig.js_).
Updating of the Breadcrumbs section happens each time when view is changed ( **EVENT\_CHANGE\_BASE\_VIEW** event handler in the _/config/process/CommonConfig.js_).

**Dialog (Dialog.js):**

This component adds html fragment to the **#dialogs** element on the page. It uses _templates/common/DialogTemplate.html_ as a template.

Initially it is used to display a popup with some content in it, image button "x" at the right top corner and button "close" at the bottom. In case there should be a different html layout or events it can be extended.

Refer to _org/forgerock/mock/ui/user/TermsOfUseDialog.js_ as an example of Dialog with end-user layout.

Refer to _org/forgerock/mock/ui/user/profile/ChangeSecurityDataDialog.js_ as an example of Dialog with end-user layout and events; see screenshot:

**ConfirmationDialog (ConfirmationDialog.js):**

The ConfirmationDialog is an extended Dialog component. It's based on templates _/common/ConfirmationDialogTemplate.html_ template and has own events definition. 

**Footer (Footer.js):**
This component represents footer section on the page. It adds html fragment to the **#footer** element and uses _templates/common/FooterTemplate.html_ as a template. This section is included in all pages.

To add footer section to the page use **render()** method.

**ElementTable (ElementTable.js):**

**GridTableView (GridTableView.js):**

**LineTableView (LineTableView.js):**

**Messages (Messages.js):**

This component adds html fragments to the **#messages** element on the page.
It is used to display notifications ("error" or "info"). See screenshot:

To display the notification use:

    eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, messageKey)

Where messageKey is a key from _config/messages/CommonMessages.js_ or _config/messages/UserMessages.js_.

Example can be found in _config/process/CommonConfig.js_.

**Navigation (Navigation.js):**

The Navigation component adds list of links to the **#menu** element on the page, _templates/common/NavigationTemplate.html_ is used as a template.

Navigation links can be defined in the _config/AppConfiguration.js_ file, module _org/forgerock/commons/ui/common/components/Navigation_. By using role parameter it is possible to set specific links to the corresponding role.

        moduleClass: "org/forgerock/commons/ui/common/components/Navigation",
        configuration: {
            links: {
                user: {
                    urls: {
                        dashboard: {
                            url: "#dashboard/",
                            name: "config.AppConfiguration.Navigation.links.dashboard",
                            icon: "glyph-icon-th-list",
                            inactive: false
                        }
                    }
                }
            }
        }

For initialization of Navigation component **init()** method is used (see **EVENT\_CHANGE\_BASE\_VIEW** 
event handler in _config/process/CommonConfig.js_). To update navigation section method **reload()** needs to be used (see 
**EVENT\_CHANGE\_VIEW** event handler in _config/process/CommonConfig.js_).

Parameters

**url** 

  * The url that the navigation button will map to (matches a route) 

**name** 

  * Title of the navigation button

**icon** 

  * Glyph icon type currently the glyph icons are specified in a CSS file

**inactive** 

  * Sets the button to inactive true/false

**PopupCtrl (PopupCtrl.js)**

**PopupView (PopupView.js):**

PopupCtrl is just a shorthand for the popup view. Which provides a simple way of doing a mouseover popup. 

    PopupCtrl.showBy("MESSAGE", Element); 

(same as) 
    
    PopupView.setContent("MESSAGE");
    PopupView.setPositionBy(Element);

<h2 id="common_components_how">How to Add a New Common Component</h2>

New common component can be implemented as a completely new view or it can extend the existing ones. In general components inherit from the _org/forgerock/commons/ui/common/main/AbstractView.js_ **class**.

Following steps describe how to create new common component:

1. create new Common Component under 
_org/forgerock/commons/ui/common/components/ComponentName_. For more details see chapter **Create new view**;
2. include newly created component as dependency to the closest **main.js** file;
3. update _config/process/CommonConfig.js_ with necessary code (e.g. add initialization of new common component). 

<h1 id="roles">Roles</h1>
=====
Property "roles" should be defined for each registered user. E.g. it could be
"administrator", "task manager" or "user". Using "roles" property allows to
create different behaviour of the app, display different sections on the page, navigate the route only for a dedicated role, etc.

Example of the routes configuration:

    "dashboard": {
        view: "org/forgerock/project-name/ui/admin/Dashboard",
        role: "ui-user,ui-admin",
        url: "dashboard/",
        forceUpdate: true
    },   
        "adminUsers": {
        view: "org/forgerock/project-name/ui/admin/users/UsersView",
        url: "users/",
        role: "ui-admin"
    }

To exclude a specific role use **excludedRole** property.

<h1 id="validation">Validation</h1>
==========
For form validation on the page _org/forgerock/commons/ui/common/main/ValidatorsManager.js_ "class" is used.

Common rules for validation can be found in _config/validators/CommonValidators.js_:

  * "Required field" rule;
  * "Min length for password field" rule;

Specific end-user rules can be found in _config/validators/UserValidators.js_.

_org/forgerock/commons/ui/common/util/ValidatorsUtils.js_ is responsible for adding/changing validation status. It adds html fragments with validation status right after the form fields.

<h2 id="validation_how">How to Add a New Validator</h2>

To add the rules for specific role following steps need to be done:

1.  Create new validator with specific rules under the _config/validators/_
    
        "exampleValidator": {
            "name": "Example Validator",
            "dependencies": [
                "org/forgerock/commons/ui/common/util/ValidatorsUtils"
            ],
            "validator": function(el, input, callback, utils) {
                var value = input.val(),
                    errors = [];
                if (value === "example") {
                    errors.push("Error, value not equal to 'example'");
                }
                if (errors.length === 0) {
                    callback(); 
                } else {
                    callback(errors);
                }
            }
        } 

2.  Add new validator to the _org/forgerock/commons/ui/common/main/ValidatorsManager.js_ in the _config/AppConfiguration_

        {
            moduleClass:"org/forgerock/commons/ui/common/main/ValidatorsManager",
            configuration: {
                validators: { },
                loader: [
                    {"validators":"config/validators/RoleSpecificValidators"},
                    {"validators":"config/validators/UserValidators"},
                    {"validators":"config/validators/CommonValidators"}
                ]
            }
        }

3.  Attach the validator to an input

        <div class="group-input-span">
            <input type="text"  value="" data-validator="exampleValidator"
                data-validator-event="keyup change" required/>
            <span class="error">x</span>
        </div>
4.  Initialize
To initialize form validators on the view use bindValidators() method on the render phase.  If rendering a validator on a dialog the bind function needs to be called on show

    You must include _"org/forgerock/commons/ui/common/main/ValidatorsManager"_ and you must add the **onValidate** event.  

        render: function (arg, callback) {
            this.show(_.bind(function () {
                validatorsManager.bindValidators(this.$el);
            }, this));   
        }

<h1 id="messages">Messages</h1>

Both _config/messages/CommonMessages.js_ and _config/messages/UserMessages.js_ contains keys for common messages like **"Login/password combination is invalid."**, **"Service unavailable"** and so on.

Any additional messages should be configured in the end-project specific file (e.g. _config/messages/ProjectNameMessages.js_)

New messages file should be defined in the _config/AppConfiguration.js_ file:

    {
        moduleClass: "org/forgerock/commons/ui/common/components/Messages",
        configuration: {
            messages: {},
            loader: [
                {"messages": "config/messages/CommonMessages"},
                {"messages": "config/messages/UserMessages"},
                {"messages": "config/messages/ProjectNameMessages"}
            ]
        }
    }

Messages file should also be listed among dependencies in the _config/main.js_file.

Messages are displayed by firing the **"EVENT\_DISPLAY\_MESSAGE_REQUEST"** 

`eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "messageKey");` call, where **messageKey** is a key from any of the above files.

To add a new message specify message key and its type (info or error):

    "exampleMessageKey": {
       msg: "config.messages.ProjectNameMessages.exampleMessage",
       type: "error"
    }

<h1 id="translations">Translations</h1>

User facing strings these should be added into the _translation.json_ file:

    "config" : {
        "messages" : {
            "ProjectNameMessages" : {
                "exampleMessage": "Some example message"
            }
        }
    }

These can be accessed by either `$.t("JSON.object.selection")` or from handlebars `{{t "JSON.object.selection"}}`

<h1 id="constants">Constants</h1>

Frequently used variables and events should be added to the _constants.js_ file. 

    obj.EVENT_SHOW_DIALOG = "dialog.EVENT_SHOW_DIALOG";

<h1 id="events">Events</h1>

Events are fired via the call to 
    
    eventManager.sendEvent(constants.EVENT_ID, {});

Event listeners are defined in the _config/process/CommonConfig.js_ and _config/process/UserConfig.js_. These files are processed during application initialization.

Any additional event listeners should be configured in a separate file _config/process/ProjectNameConfig.js_. This should be added into the _appConfiguration.js_ file under the ProcessConfiguration module:

    {
        moduleClass: "org/forgerock/commons/ui/common/main/ProcessConfiguration",
        configuration: {
            processConfigurationFiles: [
               "config/process/UserConfig",
               "config/process/CommonConfig",
               "config/process/ProjectNameConfig.js"
            ]
        }
    }

All of the configuration files should be listed among dependencies in the _config/main.js_ file.

<h2 id="events_listener">Adding a New Event Listener</h2>

Sample configuration might look like this:

    {
        startEvent: constants.EVENT_ID,
        description: "Event description",
        dependencies: [
            "path/to/dependency/1",
            "path/to/dependency/2"
        ],
        processDescription: function(arg1, arg2, ...) {
        }
    }

<h2 id="events_param">Parameters</h2>

**startEvent** 

  * event ID to execute

**description** 

  * event description, "Event processing: description" message will be displayed in console 

**dependencies** 

  * list of dependencies to be loaded via require() calls, will be supplied as processDescription() parameters

**processDescription** 

  * event handler 

<h1 id="error">Error Handling</h1>

The _config/errorhandlers/CommonErrorHandlers.js_ file contains keys for common errors like **"badRequestError"**, **"notFoundError"**, **"internalError"** and so on. 

Typical error handling configuration might look like this:

    "serverError": {
        status: "503",
        event: constants.EVENT_SERVICE_UNAVAILABLE
    }

or

    "internalServerError": {
        status: "500",
        message: "internalError"
    }

Any additional errors should be configured in the end-project specific file (e.g. _config/errorhandlers/ProjectNameErrorHandlers.js_)

New errorHandler files should be defined in _config/AppConfiguration.js_ file:

    {
        moduleClass: "org/forgerock/commons/ui/common/main/ErrorsHandler",
        configuration: {
            defaultHandlers: {},
            loader: [
                {"defaultHandlers": "config/errorhandlers/CommonErrorHandlers"},
                {"defaultHandlers": "config/errorhandlers/ProjectNameErrorHandlers"}
            ]
        }
    }

ErrorHandler file should also be listed among dependencies in the _config/main.js_ file.

<h2 id="error_param">Parameters</h2>

**status**
  
  * error code

**message**

  * A message that will be displayed when error occurs (defined in config/messages/...)
  
    Example:

        "internalError": {
            msg: "config.messages.CommonMessages.internalError",
            type: "error"
        }

**event**

  * An event that will be triggered when some error occurs. Event handler behavior is described in config/process/...
   
    Example:

        {
            startEvent: constants.EVENT_SERVICE_UNAVAILABLE,
            description: "",
            dependencies: ["org/forgerock/commons/ui/common/main/Router"],
            processDescription: function(error, router) {
                ...
            }
        }

<h1 id="theme">Theme</h1>

FR-UI provides default theme settings. As a manual for enabling theme for the project, refer to _org/forgerock/mock/ui/common/util/ThemeManager.js_ implementation.

Styles are written in Less.js. There is a possibility to provide end-project specific variables' values. As soon as ThemeManager.js fetches the styles, they will be resolved with provided variables.

To override default theme settings, create _ui-themeconfig.json_ under resources/conf folder. The following example is taken from OpenIDM. 

    {
        "icon": "favicon.ico",
        "settings": {
            "logo": {
                "src": "images/logo.png",
                "title": "ForgeRock",
                "alt": "ForgeRock",
                "height": "80",
                "width": "120"
            },
            "lessVars": {
                "background-color": "#000",
                "background-image": "url('../images/box-bg.png')",
                "background-repeat": "no-repeat",
                "background-position": "950px -100px",
                "footer-background-color": "rgba(238, 238, 238, 0.7)",
                "background-font-color": "#5a646d",
                "column-padding": "0px",
                "login-container-label-align": "left",
                "highlight-color": "#eeea07",

                "login-container-width": "430px",
                "medium-container-width": "850px",
                "site-width": "960px",
                "message-background-color": "#fff",
                "content-background": "#f9f9f9",
                "font-color": "#5a646d",
                "font-size": "14px",
                "font-family": "Arial, Helvetica, sans-serif",
                "site-width": "960px",
                "line-height": "18px",

                "color-active": "#80b7ab",
                "color-inactive": "#626d75",

                "active-menu-color": "#80b7ab",
                "active-menu-font-color": "#f9f9f9",
                "inactive-menu-color": "#5d6871",
                "inactive-menu-font-color": "#f9f9f9",
                "button-hover-lightness": "4%",   

                "href-color": "#80b7ab",
                "href-color-hover": "#5e887f",
                "color-error": "#d97986",
                "color-warning": "yellow",
                "color-success": "#71bd71",
                "color-info": "blue",
                "color-inactive": "gray",

                "input-background-color" : "#fff",
                "input-background-invalid-color" : "#fff",
                "input-border-invalid-color" : "#f8b9b3",
                "input-border-basic" : "#DBDBDB",
                "header-border-color": "#5D5D5D",

                "footer-height": "126px"
            },
            "footer": {
                "mailto": "info@forgerock.com",
                "phone": "+47-2108-1746"
            }
        }
    }
