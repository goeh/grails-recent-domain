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

import org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsHibernateUtil
import grails.util.GrailsNameUtils
import org.springframework.beans.factory.InitializingBean

class RecentDomainService implements InitializingBean {

    static transactional = false

    private int maxHistorySize = 25  // default is to store the last 25 domains

    def grailsApplication
    def currentTenant

    private List<String> domainClasses = []

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

    private List<DomainHandle> getList(session, type = null) {
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
        if (!type) {
            type = '*'
        }
        if (type instanceof Class) {
            type = type.name
        }
        def list = map[type]
        if (list == null) {
            synchronized (map) {
                list = map[type]
                if (list == null) {
                    list = map[type] = new LinkedList<DomainHandle>()
                }
            }
        }
        return list
    }

    private List<DomainHandle> getExcludeList(request) {
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

    private boolean isDomainClass(obj) {
        domainClasses.contains(GrailsHibernateUtil.unwrapIfProxy(obj).getClass().getName())
    }

    DomainHandle remember(domainInstance, request, String tag = null) {
        def session = request?.session
        if (session == null) {
            throw new IllegalArgumentException("No HTTP session")
        }

        if (domainInstance == null) {
            throw new NullPointerException("Argument [domainInstance] is null")
        }

        if (!isDomainClass(domainInstance)) {
            throw new IllegalArgumentException("${domainInstance.class.name} is not a valid recent-domain type")
        }

        if (!domainInstance.ident()) {
            throw new IllegalArgumentException("Domain instance must be persisted before it can be remembered")
        }

        def handle = createHandle(domainInstance)
        if (tag) {
            handle.addTag(tag)
        }
        if (log.isDebugEnabled()) {
            log.debug "Remembered: ${handle.type} \"${handle}\"${tag ? ' with tag ' + tag + ' ' + handle.tags : ''}"
        }

        add(handle, getList(session))
        add(handle, getList(session, handle.type))

        return handle
    }

    void scan(domains, request) {
        def session = request?.session
        if (session == null) {
            return
        }
        if (domains instanceof Map) {
            domains = domains.values().findAll {it}
        }
        def excluded = getExcludeList(request)
        for (obj in domains) {
            if (isDomainClass(obj) && ((!obj.hasProperty('version')) || obj.version != null)) {
                def handle = createHandle(obj)
                // Excluded list can contain both DomainHandle instances
                // and String instances (domain class name).
                if (!(excluded.contains(handle) || excluded.contains(handle.type))) {
                    if (log.isDebugEnabled()) {
                        log.debug "Remembered: ${handle.type} \"${handle}\""
                    }
                    add(handle, getList(session))
                    add(handle, getList(session, handle.type))
                }
            }
        }
    }

    private void add(DomainHandle handle, List<DomainHandle> fifo) {
        synchronized (fifo) {
            def existing = fifo.find {it == handle}
            if (existing) {
                for (tag in handle.tags) {
                    existing.addTag(tag)
                }
                handle = existing // Use existing instance, not the supplied one.
                fifo.remove(existing)
                log.debug "Moved to top: ${handle.type} \"${handle}\""
            }
            fifo << handle
            if (fifo.size() > maxHistorySize) {
                fifo.remove()
            }
        }
    }

    boolean forget(domainInstance, request, String tag = null) {
        removeHandle(createHandle(domainInstance), request, tag)
    }

    boolean remove(domainInstance, request, String tag = null) {
        removeHandle(createHandle(domainInstance), request, tag)
    }

    boolean removeHandle(DomainHandle handle, request, String tag = null) {
        def list = getList(request?.session)
        def rval1 = false
        def rval2 = false
        if (list) {
            if (tag) {
                list.find {it == handle}?.removeTag(tag)
            } else {
                rval1 = list.remove(handle)
            }
        }
        list = getList(request?.session, handle.type)
        if (list) {
            if (tag) {
                list.find {it == handle}?.removeTag(tag)
            } else {
                rval2 = list.remove(handle)
            }
        }
        log.debug "Forgot: ${handle.type} \"${handle}\"${tag ? ' with tag ' + tag + ' ' + handle.tags : ''}"
        return rval1 ?: rval2
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

    List<DomainHandle> getHistory(def request, def type = null, String tag = null) {
        def list = getList(request?.session, type ?: '*')
        if (list) {
            if (tag) {
                list = list.findAll {it.isTagged(tag)}
            } else {
                list = Collections.unmodifiableList(list)
            }
        }
        return list ?: Collections.EMPTY_LIST
    }

    void clearHistory(request, type = null) {
        def session = request?.session
        if (session == null) {
            return
        }
        if (type && type != '*') {
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
        } else {
            for (domainName in domainClasses) {
                clearHistory(request, domainName)
            }
            getList(session).clear()
            log.debug("Cleared recent domain history for all types")
        }
    }

    /**
     * Convert list of domain instances to list of DomainHandle instances.
     * @param domainInstanceList List of domain instances
     * @return List of DomainHandle instances
     */
    List<DomainHandle> convert(Collection domainInstanceList) {
        def result = []
        for (obj in domainInstanceList) {
            if (isDomainClass(obj) && obj.ident()) {
                result << createHandle(obj)
            }
        }
        return result
    }

    /**
     * Find existing domain handle based on type and id.
     *
     * @param type domain class name
     * @param id primary key
     * @param request request
     * @return DomainHandle or null if not found
     */
    DomainHandle find(String type, Object id, Object request) {
        getList(request?.session, type)?.find {it.type == type && it.id == id}
    }

    private DomainHandle createHandle(Object domainInstance) {
        domainInstance = GrailsHibernateUtil.unwrapIfProxy(domainInstance)
        def handle = new DomainHandle(domainInstance)
        if (domainInstance.hasProperty('icon')) {
            handle.icon = domainInstance.icon?.toString()
        }
        if (!handle.icon) {
            def prop = GrailsNameUtils.getPropertyName(domainInstance.getClass())
            def icon = grailsApplication.config.recentDomain.icon."$prop"
            if (icon) {
                handle.icon = icon
            }
        }
        return handle
    }
}
