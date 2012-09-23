/*
 * Copyright (c) 2012 Goran Ehrsson.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

class RecentDomainGrailsPlugin {
    // the plugin version
    def version = "0.4.0"
    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "1.3 > *"
    // the other plugins this plugin depends on
    def dependsOn = [:]
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
        "grails-app/views/error.gsp",
        "grails-app/views/test/**/*.gsp",
        "grails-app/domain/grails/plugins/recentdomain/test/*.groovy",
        "grails-app/controllers/grails/plugins/recentdomain/test/*.groovy"
    ]

    def title = "Recent Domain Plugin"
    def author = "Goran Ehrsson"
    def authorEmail = "goran@technipelago.se"
    def description = '''\
This plugin remembers what domain instances has been viewed by a user and
provides tags for rendering "recent viewed" lists.
'''

    // URL to the plugin's documentation
    def documentation = "http://grails.org/plugin/recent-domain"

    // Extra (optional) plugin metadata

    // License: one of 'APACHE', 'GPL2', 'GPL3'
    def license = "APACHE"

    // Details of company behind the plugin (if there is one)
    def organization = [ name: "Technipelago AB", url: "http://www.technipelago.se/" ]

    // Any additional developers beyond the author specified above.
//    def developers = [ [ name: "Joe Bloggs", email: "joe@bloggs.net" ]]

    // Location of the plugin's issue tracker.
    def issueManagement = [ system: "github", url: "https://github.com/goeh/grails-recent-domain/issues" ]

    // Online location of the plugin's browseable source code.
    def scm = [ url: "https://github.com/goeh/grails-recent-domain" ]

    def doWithDynamicMethods = { ctx ->
        def config = application.config
        def service = ctx.getBean('recentDomainService')
        for(c in application.controllerClasses) {
            addControllerMethods(config, c.clazz.metaClass, service)
        }
    }

    def doWithApplicationContext = { applicationContext ->
        // TODO Implement post initialization spring config (optional)
    }

    def onChange = { event ->
        def ctx = event.ctx
        if (event.source && ctx && event.application) {
            def config = application.config
            def service = ctx.getBean('recentDomainService')
            // enhance controller
            if ((event.source instanceof Class) && application.isControllerClass(event.source)) {
                def c = application.getControllerClass(event.source.name)
                addControllerMethods config, c.metaClass, service
            }
        }
    }

    private void addControllerMethods(config, mc, service) {

        mc.rememberDomain = {Object domainInstance, String tag = null ->
            service.remember(domainInstance, request, tag)
        }
        mc.forgetDomain = {Object domainInstance, String tag = null ->
            service.remove(domainInstance, request, tag)
        }
    }
}
