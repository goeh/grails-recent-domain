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

/**
 * An instance of DomainHandle represents an in-memory "blueprint" of a domain instance.
 */
class DomainHandle implements Serializable {
    String type
    Object id
    String label
    String url
    String icon
    Set tags

    private DomainHandle() {}

    DomainHandle(Object obj) {
        type = Hibernate.getClass(obj).name
        id = obj.ident()
        label = obj.toString()
    }

    def getObject() {
        def clazz = getClass().getClassLoader().loadClass(type)
        return clazz.get(id)
    }

    def getObject(ClassLoader classLoader) {
        def clazz = classLoader.loadClass(type)
        return clazz.get(id)
    }

    def ident() {
        id
    }

    String getDomain() {
        def idx = type.lastIndexOf('.') + 1
        return type[idx..-1]
    }

    String getController() {
        GrailsNameUtils.getPropertyNameRepresentation(type)
    }

    String getAction() {
        'show'
    }

    void addTag(String tag) {
        if(tags == null) {
            tags = [] as Set
        }
        tags << tag
    }

    boolean removeTag(String tag) {
        tags ? tags.remove(tag) : false
    }

    boolean isTagged(String tag) {
        tags?.contains(tag)
    }

    void removeAllTags() {
        tags = null
    }

    String toString() {
        return label ?: domain
    }

    int hashCode() {
        int hash = 31 + type.hashCode()
        hash = hash * 31 + id.hashCode()
        return hash
    }

    boolean equals(Object other) {
        if (this.is(other)) {
            return true
        }
        if (other instanceof DomainHandle) {
            return other.type == this.type && other.id == this.id
        } else if (other.getClass().getName() == this.type) {
            return other.ident() == this.id
        }
        return false
    }
}
