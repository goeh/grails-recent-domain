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

import grails.plugins.recentdomain.test.*
import org.springframework.mock.web.MockHttpSession

class RecentDomainServiceTests extends GroovyTestCase {

    def grailsApplication
    def recentDomainService

    void testHistoryIsEmpty() {
        def session = new MockHttpSession()
        def request = [session: session]
        assert recentDomainService.getHistory(request).isEmpty()
    }

    void testUnique() {
        def session = new MockHttpSession()
        def request
        def instance = new TestEntity1(name: "Instance 1").save(failOnError: true, flush: true)
        5.times {
            request = [session: session] // Mock new request
            recentDomainService.scan([foo: instance], request)
        }
        def crumbTrail = recentDomainService.getHistory(request)
        assert crumbTrail.size() == 1
        assert crumbTrail[0]?.toString() == "Instance 1"
    }

    void testTrack() {
        def session = new MockHttpSession()
        def request
        def overflow = 7
        (recentDomainService.maxHistorySize + overflow).times {
            request = [session: session] // Mock new request
            def model = [foo: new TestEntity1(name: "Instance ${it + 1}").save(failOnError: true, flush: true), bar: new Date(), baz: "Hello World", nothing: null]
            recentDomainService.scan(model, request)
        }
        def crumbTrail = recentDomainService.getHistory(request)
        assert recentDomainService.maxHistorySize == crumbTrail.size()
        assert crumbTrail[0]?.toString() == "Instance ${overflow + 1}"
        assert crumbTrail[-1]?.toString() == "Instance ${recentDomainService.maxHistorySize + overflow}"
    }

    void testTrackExcluded() {
        def session = new MockHttpSession()
        def request = [session: session] // Mock new request
        // NOTE configuration cannot be set/changed here because it's to late.
        // RecentDomainService initializes config when Spring initializes the service.
        // Therefore (test) configurations must be added to Config.groovy
        // recentDomain.exclude = [grails.plugins.recentdomain.test.TestEntity2, "grails.plugins.recentdomain.test.TestEntity3"]
        recentDomainService.scan([
                foo: new TestEntity1(name: "Instance 1").save(failOnError: true, flush: true),
                bar: new TestEntity2(name: "Instance 2").save(failOnError: true, flush: true),
                baz: new TestEntity3(name: "Instance 3").save(failOnError: true, flush: true)],
                request)

        def crumbTrail = recentDomainService.getHistory(request)
        assert crumbTrail.size() == 1
        assert crumbTrail[0]?.toString() == "Instance 1"
    }

    void testMoveToTop() {
        def session = new MockHttpSession()
        def request
        5.times {
            request = [session: session] // Mock new request
            def model = [foo: new TestEntity1(name: "Instance ${it + 1}").save(failOnError: true, flush: true)]
            recentDomainService.scan(model, request)
        }
        def crumbTrail = recentDomainService.getHistory(request)
        assert crumbTrail.size() == 5
        assert crumbTrail[-1]?.toString() == "Instance 5"
        request = [session: session] // Mock new request
        recentDomainService.scan([foo: TestEntity1.findByName("Instance 3")], request)

        crumbTrail = recentDomainService.getHistory(request)
        assert crumbTrail.size() == 5
        assert crumbTrail[-1]?.toString() == "Instance 3"
    }

    void testExcludePermanent() {
        def session = new MockHttpSession()
        def request = [session: session] // Mock first request
        3.times {
            new TestEntity1(name: "Instance ${it + 1}").save(failOnError: true, flush: true)
        }
        def excluded = recentDomainService.exclude(TestEntity1.findByName("Instance 2"), request, true)
        3.times {
            request = [session: session] // Mock new request
            def model = [foo: TestEntity1.findByName("Instance ${it + 1}")]
            recentDomainService.scan(model, request)
        }
        def crumbTrail = recentDomainService.getHistory(request)
        assert crumbTrail.size() == 2
        assert crumbTrail[0]?.toString() == "Instance 1"
        assert crumbTrail[-1]?.toString() == "Instance 3"
        assert !crumbTrail.contains(excluded)
    }

    void testExcludeOnce() {
        def session = new MockHttpSession()
        def request = [session: session] // Mock first request
        def instance = new TestEntity1(name: "Instance 1").save(failOnError: true, flush: true)
        recentDomainService.exclude(instance, request, false)
        def model = [foo: instance]
        recentDomainService.scan(model, request)
        assert recentDomainService.getHistory(request).size() == 0

        request = [session: session] // Mock new request
        model = [foo: instance]
        recentDomainService.scan(model, request)
        def crumbTrail = recentDomainService.getHistory(request)
        assert crumbTrail.size() == 1
        assert crumbTrail.find {it.type == instance.class.name} != null
    }

    void testRemove() {
        def session = [:]
        def request
        3.times {
            request = [session: session] // Mock new request
            def model = [foo: new TestEntity1(name: "Instance ${it + 1}").save(failOnError: true, flush: true)]
            recentDomainService.scan(model, request)
        }
        def obj = recentDomainService.remove(TestEntity1.findByName("Instance 3"), request)
        assert obj != null
        assert !recentDomainService.getHistory(request).contains(obj)
    }

    void testClearHistory() {
        def request = [session: new MockHttpSession()] // Mock new request

        recentDomainService.remember(new TestEntity1(name: "TestEntity1").save(failOnError: true, flush: true), request)
        recentDomainService.remember(new TestEntity4(name: "TestEntity4").save(failOnError: true, flush: true), request)
        assert recentDomainService.getHistory(request).size() == 2
        assert recentDomainService.getHistory(request, TestEntity1).size() == 1
        assert recentDomainService.getHistory(request, TestEntity4).size() == 1

        recentDomainService.clearHistory(request, TestEntity4)

        assert recentDomainService.getHistory(request).size() == 1
        assert recentDomainService.getHistory(request, TestEntity1).size() == 1
        assert recentDomainService.getHistory(request, TestEntity4).size() == 0

        recentDomainService.clearHistory(request)

        assert recentDomainService.getHistory(request).size() == 0
        assert recentDomainService.getHistory(request, TestEntity1).size() == 0
        assert recentDomainService.getHistory(request, TestEntity4).size() == 0
    }

    void testConvert() {
        assert recentDomainService.convert([]).size() == 0

        def instance1 = new TestEntity1(name: "Instance 1").save(failOnError: true, flush: true)
        def instance2 = new TestEntity4(name: "Instance 2").save(failOnError: true, flush: true)
        assert instance1 != null
        assert instance2 != null
        def list = recentDomainService.convert([instance1, instance2])
        assert list.size() == 2
        def handle = list[0]
        assert handle != null
        assert (handle instanceof DomainHandle)
        assert handle.toString() == "Instance 1"
        assert instance1.id == handle.id
    }

    void testTags() {
        def testEntity1 = new TestEntity1(name: "Hello").save(failOnError: true, flush: true)
        def testEntity2 = new TestEntity1(name: "World").save(failOnError: true, flush: true)
        def request = [session: new MockHttpSession()] // Mock new request
        def tag = "test"
        assert recentDomainService.getHistory(request, null, tag).isEmpty()
        recentDomainService.remember(testEntity1, request)
        recentDomainService.remember(testEntity2, request, tag)

        assert recentDomainService.getHistory(request, null).size() == 2
        assert recentDomainService.getHistory(request, null, tag).size() == 1
        def obj = recentDomainService.getHistory(request, null, tag).first()
        assert obj.label == testEntity2.name

        recentDomainService.remember(testEntity2, request, 'foo')
        recentDomainService.remember(testEntity2, request, 'bar')
        obj = recentDomainService.getHistory(request, null, 'foo').first()
        assert obj.tags.size() == 3
        assert obj.tags.contains(tag)
        assert obj.tags.contains('foo')
        assert obj.tags.contains('bar')

        recentDomainService.forget(testEntity2, request, 'foo')
        assert obj.tags.size() == 2
        recentDomainService.forget(testEntity2, request, 'bar')
        assert obj.tags.size() == 1
        assert obj.tags.toList() == [tag]
    }

}
