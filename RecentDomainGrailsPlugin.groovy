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
    def version = "0.6.0-SNAPSHOT"
    def grailsVersion = "2.0 > *"
    def dependsOn = [:]
    def pluginExcludes = [
        "grails-app/views/error.gsp",
        "grails-app/views/test/**/*.gsp",
        "grails-app/domain/grails/plugins/recentdomain/test/*.groovy",
        "grails-app/controllers/grails/plugins/recentdomain/test/*.groovy"
    ]
    def title = "Recent Domain List"
    def author = "Goran Ehrsson"
    def authorEmail = "goran@technipelago.se"
    def description = '''\
This plugin remembers what domain instances has been viewed by a user and
provides tags for rendering "recent viewed" or "crumb trail" lists.
'''
    def documentation = "https://github.com/goeh/grails-recent-domain"
    def license = "APACHE"
    def organization = [ name: "Technipelago AB", url: "http://www.technipelago.se/" ]
    def issueManagement = [ system: "github", url: "https://github.com/goeh/grails-recent-domain/issues" ]
    def scm = [ url: "https://github.com/goeh/grails-recent-domain" ]

    def doWithDynamicMethods = { ctx ->
        def config = application.config
        def service = ctx.getBean('recentDomainService')
        for(c in application.controllerClasses) {
            addControllerMethods(config, c.clazz.metaClass, service)
        }
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
