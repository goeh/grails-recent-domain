# Grails Recent Domain Plugin

This plugin can be used to present to the user a list of domain instances that he/she has previously visited/looked at.
This feature is known as "bread crumb" or "recent list".

Domain instances used in views are pushed onto a FIFO (First In First Out).
A list of instances can later be retrieved from this FIFO.
GSP tags are provided that render a list of domain instances last visited.

Domain instances can be added to the recent list programatically or models
returned from controller actions can automatically be scanned for domain instances and put on the recent list.

Recent lists are stored in the user's HTTP session.

## Configuration

**auto scanning**

The model returned by controller actions can be scanned for domain instances and automatically put on the recent list.
The configuration parameter *recentDomain.autoscan.actions* specifies what controllers and actions to scan.

recentDomain.autoscan.actions = ['customer:show', 'project:show']

You can use asterisk (*) as a wildcard for both controller and action.

**white list**

You can restrict what domain classes are added to the recent list with the *recentDomain.include* parameter.

    recentDomain.include = ['com.mycompany.Person']

**black list**

You can restrict what domain classes are added to the recent list with the *recentDomain.exclude* parameter.
Domain classes specified are excluded and will not be added to a recent list.

    recentDomain.exclude = ['com.mycompany.Address']
        
**list size**

The size of a recent list defaults to 25 entries but you can change that with the *recentDomain.maxSize* parameter.

    recentDomain.maxSize = 10

**icons**

You can assign icon names to domain classes and display icons next to domain instances when you display a recent list on screen.
Add a *recentDomain.icon.<domainClassPropertyName>* parameter for each domain class.

    recentDomain.icon.person = 'user'
    recentDomain.icon.company = 'house'

When you retrieve a domain instance from the recent list you can read the *icon* property to find out what icon to render.

## RecentDomainService

*RecentDomainService* contains method for interaction with recent lists. If you have configured *autoscan*
and use the provided GSP tags you will probably not use the service, but if you need more programatic control you
can use the service to interact with recent lists.

A recent list does not store the actual domain instances. Instead a proxy is stored in the list. This proxy contains
attributes that you can use when rendering recent lists on the screen.

    String type // Class name of the domain instance
    Object id   // The primary key of the domain instance
    String label // The toString() representation of the domain instance
    String url   // URL in the application that displays the domain instance
    String icon  // Icon that represents the domain instance
    Set tags     // Optional set of tags

**remember(Object domainInstance, HttpServletRequest request, String tag = null)**

Add a domain instance to the recent list.

**scan(Object model, HttpServletRequest request)**

Scan a model and look for domain instances. The model can be a List or a Map.
Every domain instance found are added to the recent list.
White lists and black lists are checked before adding a domain instance.

**remove(Object domainInstance, HttpServletRequest request, String tag = null)**

**forget(Object domainInstance, HttpServletRequest request, String tag = null)**

Remove a domain instance form a recent list (*forget* is just an alias for *remove*).

**List getHistory(HttpServletRequest request, Object type = null, String tag = null)**

Returns a List containing all recent domain instances or if *type* is specified recent domain instances of a specific type.
The *type* parameter can be a domain Class or the class name of a domain.

**clearHistory(HttpServletRequest request, Object type = null)**

Clears/removes all entries in a recent list.
The *type* parameter can be a domain Class or the class name of a domain.


## GSP Tags

### hasHistory

The *hasHistory* tag is used to check if the recent list contains any domain instances for a specific type.
If domain instances exists the tag body is rendered.

Attribute | Description
--------- | --------------
type      | domain class or "property name" of a domain class
tag       | optional tag/sub-group name

    <recent:hasHistory type="${com.mycompany.Customer}">
        Recent viewed customers: ...
    </recent:hasHistory>

### each

The *each* tag is used to iterate over domain instances in the recent list.

Attribute | Description
--------- | --------------
type      | domain class or "property name" of a domain class
tag       | optional name of sub-list
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

    def personInstance = Person.get(params.id)
    rememberDomain(personInstance)
    
The tag feature can be used to temporary "remember" a domain instance in a (hidden) list.
Later in the same user session you can pick up (and remove) the domain instance from the list
for further processing. It's like the list becomes a long lived flash scope.

**forgetDomain(Object domainInstance, String tag = null)**

Remove a domain instance from the recent list.

    def personInstance = Person.get(params.id)
    forgetDomain(personInstance)

## Known issues

- If a domain instance is added to a recent list and then removed from the database
  it is not automatically removed from the recent list. You must add a call to *forgetDomain*
  when you remove domain instances that may be in a recent list.

## Miscellaneous

- The [GR8 CRM ecosystem](http://gr8crm.github.io) uses recent-domain plugin for crumb trail support.
