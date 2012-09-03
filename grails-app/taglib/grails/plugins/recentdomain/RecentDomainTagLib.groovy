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

package grails.plugins.recentdomain

/**
 * Recent domain tags.
 */
class RecentDomainTagLib {

    static namespace = "recent"

    def recentDomainService

    /**
     * Render tag body if recent domain history exists.
     *
     * @attr type type of domain to look for
     * @attr tag optional tag filter
     */

    def hasHistory = {attrs, body ->
        def type = attrs.type ? (attrs.type instanceof Class ? attrs.type.name : attrs.type.toString()) : null
        def tag = attrs.tag ?: null
        def list = recentDomainService.getHistory(request, type, tag)
        request.RECENT_DOMAIN_LIST = list
        if (!list.isEmpty()) {
            out << body()
        }
    }

    /**
     * Iterate over domain history and render tag body for each domain found.
     *
     * @attr type type of domain to iterate over
     * @attr tag optional tag filter
     * @attr reverse true for reversed list (last visited first)
     * @attr max max number of domain to iterate
     * @attr var name of attribute holding reference to domain
     * @attr status name of attribute holding iteration counter
     */
    def each = {attrs, body ->
        def type = attrs.type ? (attrs.type instanceof Class ? attrs.type.name : attrs.type.toString()) : null
        def tag = attrs.tag ?: null
        def list = request.RECENT_DOMAIN_LIST ?: recentDomainService.getHistory(request, type, tag)
        if (list) {
            renderList(list, attrs, body)
        }
        request.RECENT_DOMAIN_LIST = null
    }

    private void renderList(List list, Map attrs, Closure body) {
        if (!list) {
            return
        }
        def var = attrs.var?.toString()
        def status = attrs.status?.toString()
        def counter = 0
        def max = attrs.max?.toInteger()
        if (!max) {
            max = list.size()
        } else if (max > list.size()) {
            max = list.size()
        }
        list = list[-max..-1]
        if (attrs.reverse.toString().toBoolean()) {
            list = list.reverse()
        }
        for (handle in list) {
            def args
            if (var || status) {
                args = [:]
                args[var ?: 'it'] = handle
                if (status) {
                    args[status] = counter++
                }
            } else {
                args = handle
            }
            out << body(args)
        }
    }

}
