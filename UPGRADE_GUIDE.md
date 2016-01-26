# Upgrade Guide

## 14.x -> 15.x
### React Support!
* ViewManager can now handle being passed either a Backbone or a React view.

* `react` and `react-dom` are bundled with commons, ensure you update your product license file(s). e.g. in OpenAM...
```
// openam-server-only/src/license/THIRD-PARTY.properties
org.forgerock.commons.ui.libs--react--15.2.1=BSD
org.forgerock.commons.ui.libs--react-dom--15.2.1=BSD
```

#### `withRouter`
* `org/forgerock/commons/ui/common/components/hoc/withRouter`

* React views are *not* passed parameters from the router as Backbone views are. This is intentional as not all view may need router parameters.

* `withRouter` is a HoC that you use access router parameters in your React component.
```javascript
import withRouter from "org/forgerock/commons/ui/common/components/hoc/withRouter"
class MyReactComponent extends Component { ... }
export default withRouter(MyReactComponent)
```

## 13.x -> 14.x

References to UserProfileView in projects which use forgerock-ui-user will need to be updated.
In 13.x, use of UserProfileView was determined via dependency injection in the requirejs map aliases, like so:

    require.config({ "map": { "*": {
        "UserProfileView" : (serverInfo.kbaEnabled === "true"
            ? "org/forgerock/commons/ui/user/profile/UserProfileKBAView"
            : "org/forgerock/commons/ui/user/profile/UserProfileView")
    } } });

This was based on the idea that you would swap out one whole view in favor of another, to be responsible
for rendering the user profile. Switching the view was used to decide whether or not to show the basic profile or the "KBA"-enhanced version.

In 14.x, the reference to the whole view no longer needs to be replaced. Instead, there is a single instance of the UserProfileView which simply needs to be told which child views are available for rendering additional tabs (such as the KBA tab). For example:

    if (serverInfo.kbaEnabled === "true") {
        require(["org/forgerock/commons/ui/user/profile/UserProfileKBATab"], (tab) => {
            UserProfileView.registerTab(tab);
        });
    }

## 12.x -> 13.x

The entries for form2js and js2form must be adjusted to match the new version. Previously it was listed like so:

    js2form: "libs/js2form-2.0",
    form2js: "libs/form2js-2.0",

Now they must be specified like so:

    js2form: "libs/js2form-2.0-769718a",
    form2js: "libs/form2js-2.0-769718a",

## 11.x -> 12.x

* There is a new parameter attached to the event for EVENT_SHOW_LOGIN_DIALOG : "authenticatedCallback". This is a function which is expected to be called when the inline login dialog has successfully re-authenticated the user. Since there is an expectation that the EVENT_SHOW_LOGIN_DIALOG could be invoked multiple times, it is recommended that each callback function be stored in a queue (such as the new org/forgerock/commons/ui/common/util/Queue facility) so that they all get called and in the order received. If the implementing product is making use of the default EVENT_SHOW_LOGIN_DIALOG implementation, then nothing needs to be changed in the product to work properly.

* Updating to Handlebars 4

The path to the new version of handlebars (4.0.5) will need to be adjusted in the root main.js.

## 9.x -> 10.x

* Ability to provide project-specific `KBADelegate`

Both OpenAM and OpenIDM need to provide a path to a `KBADelegate`, which can be either a project-specific or the commons one.

## 8.x -> 9.x

* Ability to pass (main) link filters to Navigation

The logic within `Navigation.js` that filters the master list of links based on various conditions (e.g. roles) has been factored out and is now provided to the Navigation component via a separate module.

The original behaviour is provided via the filter `org/forgerock/commons/ui/common/components/navigation/filters/RoleFilter`.

To use, add a new mapping to the RequireJS configuration:

```javascript
require.config({
    map: {
        "*" : {
            "NavigationFilter" : "org/forgerock/commons/ui/common/components/navigation/filters/RoleFilter"
            ...
```

For reference, OpenAM is using `org/forgerock/openam/ui/common/components/navigation/filters/RouteNavGroupFilter`.

* Introduction of new user role `ui-self-service-user`

Applications will need to add this role to any users that should be able to access the self-service (profile) pages.
