#Grails Recent Domain Plugin

This plugin can be used to present to the user a list of domain instances that he/she has previously visited/looked at.
Domain instances used in views are pushed onto a FIFO. A list of instances can later be retrieved from this FIFO.
Tags are provided that render a list of domain instances last visited.
This plugin provides a crumb trail feature for domain instances. Can be used to give the user an option list to select from
when performing tasks that operate on domain instances.

Domain instances can be added to the recent list programatically or models
returned from actions can be automatically scanned for domain instances.
