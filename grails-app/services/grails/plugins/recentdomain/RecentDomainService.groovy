package grails.plugins.recentdomain

import org.hibernate.Hibernate
import grails.util.GrailsNameUtils
import org.springframework.beans.factory.InitializingBean

class RecentDomainService implements InitializingBean {

    static transactional = false

    private int maxHistorySize = 25  // default is to store the last 25 domains

    def grailsApplication
    def messageSource

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
        def map = session.RECENT_DOMAIN
        if (map == null) {
            map = session.RECENT_DOMAIN = [:]
        }
        if(type instanceof Class) {
            type = type.name
        }
        def list = map[type]
        if (list == null) {
            list = map[type] = new LinkedList()
        }
        return list
    }

    private List getExcludeList(request) {
        def set = new HashSet()
        def list = request?.RECENT_DOMAIN_EXCLUDE
        if (list) {
            set.addAll(list)
        }
        list = request?.session?.RECENT_DOMAIN_EXCLUDE
        if (list) {
            set.addAll(list)
        }
        return set as List
    }

    def remember(domainInstance, request) {
        def session = request?.session
        if (session == null) {
            throw new IllegalArgumentException("No HTTP session")
        }
        if (domainInstance == null) {
            throw new NullPointerException("Argument [domainInstance] is null")
        }
        if (!domainClasses.contains(Hibernate.getClass(domainInstance)?.name)) {
            throw new IllegalArgumentException("${domainInstance.class.name} is not a domain instance")
        }
        if ((!domainInstance.hasProperty('version')) || domainInstance.version != null) {
            throw new IllegalArgumentException("Domain instance must be persistent before it can be remembered")
        }
        def locale = request.locale ?: Locale.getDefault()
        def handle = createHandle(domainInstance, locale)
        if (log.isDebugEnabled()) {
            log.debug "Remembering: ${handle.type}) \"${handle}\""
        }
        add(handle, getList(session, handle.type))
        add(handle, getList(session))
    }

    def scan(domains, request) {
        def session = request?.session
        if (session == null) {
            return
        }
        if(domains instanceof Map) {
            domains = domains.values()
        }
        def excluded = getExcludeList(request)
        def locale = request.locale ?: Locale.getDefault()
        for(obj in domains) {
            if (obj != null && domainClasses.contains(Hibernate.getClass(obj)?.name) && ((!obj.hasProperty('version')) || obj.version != null)) {
                def handle = createHandle(obj, locale)
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
        def locale = request.locale ?: Locale.getDefault()
        def handle = createHandle(domainInstance, locale)
        def obj1 = getList(request?.session, handle.type)?.remove(handle)
        def obj2 = getList(request?.session)?.remove(handle)
        log.debug("Removed: (${handle.type}) \"${handle}\"")
        return obj1 ?: obj2
    }

    def exclude(domainInstance, request, permanent = false) {
        def session = request?.session
        if (session == null) {
            return null
        }
        def set
        if (permanent) {
            set = session.RECENT_DOMAIN_EXCLUDE
            if (set == null) {
                set = session.RECENT_DOMAIN_EXCLUDE = new HashSet()
            }
        } else {
            set = request.RECENT_DOMAIN_EXCLUDE
            if (set == null) {
                set = request.RECENT_DOMAIN_EXCLUDE = new HashSet()
            }
        }
        def locale = request.locale ?: Locale.getDefault()
        def handle = createHandle(domainInstance, locale)
        set << handle
        log.debug("Excluded ${permanent ? 'permanent' : 'once'}: (${handle.type}) \"${handle}\"")
        return handle
    }

    List getHistory(request, type = '*') {
        Collections.unmodifiableList(getList(request?.session, type) ?: Collections.EMPTY_LIST)
    }

    List convert(Collection domainInstanceList, Locale locale) {
        def result = []
        for (obj in domainInstanceList) {
            if (obj != null && domainClasses.contains(Hibernate.getClass(obj)?.name) && ((!obj.hasProperty('version')) || obj.version != null)) {
                result << createHandle(obj, locale)
            }
        }
        return result
    }

    def createHandle(Object domainInstance, Locale locale) {
        def handle = new DomainHandle(domainInstance)
        if (domainInstance.hasProperty('icon')) {
            handle.icon = domainInstance.icon?.toString()
        }
        if (!handle.icon) {
            def prop = GrailsNameUtils.getPropertyName(domainInstance.class)
            handle.icon = messageSource.getMessage(prop + '._icon', [] as Object[], null, locale)
        }
        return handle
    }
}
