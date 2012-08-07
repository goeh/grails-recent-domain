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

    def hasHistory = {attrs, body ->
        def type = attrs.type ? (attrs.type instanceof Class ? attrs.type.name : attrs.type.toString()) : null
        def list = type ? recentDomainService.getHistory(request, type) : recentDomainService.getHistory(request)
        if (!list.isEmpty()) {
            out << body()
        }
    }

    /**
     * Iterate over domain history.
     * @param type type of domain to iterate over
     * @param reverse true for reversed list (last visited first)
     * @param max max number of domain to iterate
     * @param var name of attribute holding reference to domain
     * @param status name of attribute holding iteration counter
     */
    def each = {attrs, body ->
        def type = attrs.type ? (attrs.type instanceof Class ? attrs.type.name : attrs.type.toString()) : null
        def list = type ? recentDomainService.getHistory(request, type) : recentDomainService.getHistory(request)
        if (list) {
            renderList(list, attrs, body)
        }
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

    def eachList = {attrs, body ->
        def list = attrs.remove('list')
        if (list == null) {
            throwTagError("Tag [eachList] is missing required attribute [list]")
        }
        if (!list.isEmpty()) {
            def locale = request.locale ?: Locale.getDefault()
            renderList(recentDomainService.convert(list, locale), attrs, body)
        }
    }
}
