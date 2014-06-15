# Grails Recent Domain Plugin

This plugin can be used to present to the user a list of domain instances that you have previously visited/looked at.
This feature is known as "bread crumb" or "recent list".

Domain instances used in views are pushed onto a FIFO (First In First Out).
A list of instances can later be retrieved from this FIFO.
GSP tags are provided that render a list of domain instances last visited.

Domain instances can be added to the recent list programatically or models
returned from controller actions can automatically be scanned for domain instances and put on the recent list.

## Configuration

**auto scanning**

The model returned by controller actions can be scanned for domain instances and automatically put on the recent list.
The configuration parameter *recentDomain.autoscan.actions* specified what controllers and actions to scan.

recentDomain.autoscan.actions = ['customer:show', 'project:show']

You can use '*' as a wildcard for both controller and action.

## GSP Tags

### hasHistory

The *hasHistory* tag is used to check if the FIFO contains any domain instances for a specific type.
If domain instances exists the tag body is rendered.

Attribute | Description
--------- | --------------
type      | domain class or "property name" of a domain class
tag       | optional tag/sub-group name

    <recent:hasHistory type="${com.mycompany.Customer}">
        Recent viewed customers: ...
    </recent:hasHistory>

### each

Attribute | Description
--------- | --------------
type      | domain class or "property name" of a domain class
tag       | optional tag/sub-group name
reverse   | true for reversed list (last visited first)
max       | max number of domain instances to list
var       | name of attribute holding reference to domain (default 'it')
status    | name of attribute holding iteration counter

    <recent:hasHistory>
        <div class="recent-list">
            <recent:each var="m" max="5" reverse="true">
                <g:link controller="${m.controller}" action="${m.action}" id="${m.id}">
                    <i class="${m.icon ?: 'icon-chevron-right'}"></i>
                    ${m.encodeAsHTML()}
                </g:link>
            </recent:each>
        </div>
    </recent:hasHistory>
    
## Controller Methods

The following methods are added to all controllers in the application.

**rememberDomain(Object domainInstance, String tag = null)**

With rememberDomain() you can programatically add a domain instance to the recent list.
If the tag parameter is omitted all domain instances of the same class will be added to the same recent list.
If you want to add domain instances to multiple lists depending on some business logic you can use the tag parameter
to specify the name of a list.

The tag feature can be used to temporary "remember" a domain instance in a (hidden) list.
Later in the same user session you can pick up (and remove) the domain instance from the list
for further processing. It's like the list becomes a long lived flash scope.

**forgetDomain(Object domainInstance, String tag = null)**

Remove a domain instance from the recent list.
