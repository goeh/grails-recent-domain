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

import org.hibernate.Hibernate
import grails.util.GrailsNameUtils
import org.springframework.beans.factory.InitializingBean

class RecentDomainService implements InitializingBean {

    static transactional = false

    private int maxHistorySize = 25  // default is to store the last 25 domains

    def grailsApplication
    def currentTenant

    private List domainClasses = []

    int getMaxHistorySize() {
        return this.@maxHistorySize
    }

    void afterPropertiesSet() {
        def config = grailsApplication.config.recentDomain
        // Configure max history size.
        def maxSize = config.maxSize
        if (maxSize) {
            maxHistorySize = Integer.valueOf(maxSize.toString())
        }
        // Configure included domain classes.
        def include = config.include
        if (include) {
            include.each {domain ->
                if (domain instanceof Class) {
                    domainClasses << domain.name
                } else {
                    domainClasses << domain.toString()
                }
            }
        } else {
            domainClasses = grailsApplication.domainClasses*.clazz.name
        }
        // Configure excluded domain classes.
        def exclude = config.exclude
        if (exclude) {
            exclude.each {domain ->
                if (domain instanceof Class) {
                    domainClasses.remove(domain.name)
                } else {
                    domainClasses.remove(domain.toString())
                }
            }
        }
    }

    private List getList(session, type = '*') {
        if (session == null) {
            return null
        }
        def tenant = currentTenant?.get() ?: 0
        def key = 'RECENT_DOMAIN.' + tenant
        def map = session[key]
        if (map == null) {
            synchronized (session) {
                map = session[key]
                if (map == null) {
                    map = session[key] = [:]
                }
            }
        }
        if (type instanceof Class) {
            type = type.name
        }
        def list = map[type]
        if (list == null) {
            synchronized (map) {
                list = map[type]
                if (list == null) {
                    list = map[type] = new LinkedList()
                }
            }
        }
        return list
    }

    private List getExcludeList(request) {
        def tenant = currentTenant?.get() ?: 0
        def key = 'RECENT_DOMAIN_EXCLUDE.' + tenant
        def set = new HashSet()
        if (request) {
            def list = request[key]
            if (list) {
                set.addAll(list)
            }
            if (request.session) {
                list = request.session[key]
                if (list) {
                    set.addAll(list)
                }
            }
        }
        return set as List
    }

    DomainHandle remember(domainInstance, request) {
        def session = request?.session
        if (session == null) {
            throw new IllegalArgumentException("No HTTP session")
        }

        if (domainInstance == null) {
            throw new NullPointerException("Argument [domainInstance] is null")
        }

        if (!domainClasses.contains(Hibernate.getClass(domainInstance)?.name)) {
            throw new IllegalArgumentException("${domainInstance.class.name} is not a valid recent-domain type")
        }

        if (!domainInstance.ident()) {
            throw new IllegalArgumentException("Domain instance must be persisted before it can be remembered")
        }

        def handle = createHandle(domainInstance)

        if (log.isDebugEnabled()) {
            log.debug "Remembering: ${handle.type} \"${handle}\""
        }

        add(handle, getList(session, handle.type))
        add(handle, getList(session))

        return handle
    }

    void scan(domains, request) {
        def session = request?.session
        if (session == null) {
            return
        }
        if (domains instanceof Map) {
            domains = domains.values()
        }
        def excluded = getExcludeList(request)
        for (obj in domains) {
            if (obj != null && domainClasses.contains(Hibernate.getClass(obj)?.name) && ((!obj.hasProperty('version')) || obj.version != null)) {
                def handle = createHandle(obj)
                // Excluded list can contain both DomainHandle instances
                // and String instances (domain class name).
                if (!(excluded.contains(handle) || excluded.contains(handle.type))) {
                    if (log.isDebugEnabled()) {
                        log.debug "Remembering: ${handle.type} \"${handle}\""
                    }
                    add(handle, getList(session, handle.type))
                    add(handle, getList(session))
                }
            }
        }
    }

    private void add(handle, fifo) {
        if (fifo.remove(handle)) {
            log.debug "Moved to top: ${handle.type} \"${handle}\""
        }
        fifo << handle
        if (fifo.size() > maxHistorySize) {
            fifo.remove()
        }
    }

    def remove(domainInstance, request) {
        def handle = createHandle(domainInstance)
        def obj1 = getList(request?.session, handle.type)?.remove(handle)
        def obj2 = getList(request?.session)?.remove(handle)
        log.debug("Removed: (${handle.type}) \"${handle}\"")
        return obj1 ?: obj2
    }

    DomainHandle exclude(domainInstance, request, permanent = false) {
        def session = request?.session
        if (session == null) {
            return null
        }
        def tenant = currentTenant?.get() ?: 0
        def key = 'RECENT_DOMAIN_EXCLUDE.' + tenant
        def set
        if (permanent) {
            set = session[key]
            if (set == null) {
                synchronized (session) {
                    set = session[key]
                    if (set == null) {
                        set = session[key] = new HashSet()
                    }
                }
            }
        } else {
            set = request[key]
            if (set == null) {
                set = request[key] = new HashSet()
            }
        }
        def handle = createHandle(domainInstance)
        set << handle
        log.debug("Excluded ${permanent ? 'permanent' : 'once'}: (${handle.type}) \"${handle}\"")
        return handle
    }

    List getHistory(request, type = '*') {
        Collections.unmodifiableList(getList(request?.session, type) ?: Collections.EMPTY_LIST)
    }

    void clearHistory(request, type = '*') {
        def session = request?.session
        if (session == null) {
            return
        }
        if (type == '*') {
            for (domainName in domainClasses) {
                clearHistory(request, domainName)
            }
            getList(session).clear()
            log.debug("Cleared recent domain history for all types")
        } else {
            if (type instanceof Class) {
                type = type.name
            }
            synchronized (session) {
                getList(session, type).clear()
                def itor = getList(session).iterator()
                while (itor.hasNext()) {
                    def handle = itor.next()
                    if (handle.type == type) {
                        itor.remove()
                    }
                }
            }
            log.debug("Cleared recent domain history for type ${type}")
        }
    }

    List convert(Collection domainInstanceList) {
        def result = []
        for (obj in domainInstanceList) {
            if (domainClasses.contains(Hibernate.getClass(obj)?.name) && obj.ident()) {
                result << createHandle(obj)
            }
        }
        return result
    }

    private DomainHandle createHandle(Object domainInstance) {
        def handle = new DomainHandle(domainInstance)
        if (domainInstance.hasProperty('icon')) {
            handle.icon = domainInstance.icon?.toString()
        }
        if (!handle.icon) {
            def prop = GrailsNameUtils.getPropertyName(domainInstance.class)
            handle.icon = grailsApplication.config.recentDomain.icon."$prop"
        }
        return handle
    }
}
