# Upgrade Guide

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

## 9.x -> 10.x

* Ability to provide project-specific `KBADelegate`

Both OpenAM and OpenIDM need to provide a path to a `KBADelegate`, which can be either a project-specific or the commons one.
