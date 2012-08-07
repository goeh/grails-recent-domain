// configuration for plugin testing - will not be included in the plugin zip

log4j = {
    // Example of changing the log pattern for the default console
    // appender:
    //
    //appenders {
    //    console name:'stdout', layout:pattern(conversionPattern: '%c{2} %m%n')
    //}

    error  'org.codehaus.groovy.grails.web.servlet',  //  controllers
           'org.codehaus.groovy.grails.web.pages', //  GSP
           'org.codehaus.groovy.grails.web.sitemesh', //  layouts
           'org.codehaus.groovy.grails.web.mapping.filter', // URL mapping
           'org.codehaus.groovy.grails.web.mapping', // URL mapping
           'org.codehaus.groovy.grails.commons', // core / classloading
           'org.codehaus.groovy.grails.plugins', // plugins
           'org.codehaus.groovy.grails.orm.hibernate', // hibernate integration
           'org.springframework',
           'org.hibernate',
           'net.sf.ehcache.hibernate'

    warn   'org.mortbay.log'

    debug  'grails.app.services.grails.plugins.recentdomain'
}

recentDomain.autoscan.actions = ['*:show'] // ['*:show[0]'] to scan only first domain instance in model?
recentDomain.exclude = [grails.plugins.recentdomain.test.TestEntity2, "grails.plugins.recentdomain.test.TestEntity3"]
recentDomain.icon.testEntity1 = 'icon_heart'
recentDomain.icon.testEntity2 = 'icon_home'
recentDomain.icon.testEntity3 = 'icon_camera'
recentDomain.icon.testEntity4 = 'icon_asterisk'