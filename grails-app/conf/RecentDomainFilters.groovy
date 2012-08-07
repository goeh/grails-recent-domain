import java.util.regex.Pattern

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

class RecentDomainFilters {

    def grailsApplication
    def recentDomainService

    private static final Pattern regex = ~/^([\w\*]+):([\w\*]+)\[?([\d\.\-]*)\]?$/

    def filters = {
        def autoscan = grailsApplication.config.recentDomain.autoscan.actions
        for (ca in autoscan) {
            def m = regex.matcher(ca)
            def (c, a, n) = m[0][1..-1]
            "${c.replace('*', 'all')}_${a.replace('*', 'all')}"(controller: c, action: a) {
                after = {model ->
                    if (model) {
                        def (from, to) = n ? n.split('..').toList() : [null, null]
                        from = from ? Integer.valueOf(from) : 0
                        to = to ? Integer.valueOf(to) : -1
                        recentDomainService.scan(model.values().toList()[(from)..(to)], request)
                    }
                }
            }
        }
    }
}