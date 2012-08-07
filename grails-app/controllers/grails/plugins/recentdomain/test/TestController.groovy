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

package grails.plugins.recentdomain.test

/**
 * Controller used by integration test RecentDomainFiltersTests.
 */
class TestController {

    def recentDomainService

    def show(Long id) {
        def m1 = TestEntity1.get(id) ?: new TestEntity1(name: "TestEntity1.$id").save(failOnError:true, flush:true)
        def m2 = TestEntity2.get(id) ?: new TestEntity2(name: "TestEntity2.$id").save(failOnError:true, flush:true)
        def m3 = TestEntity3.get(id) ?: new TestEntity3(name: "TestEntity3.$id").save(failOnError:true, flush:true)
        def m4 = TestEntity4.get(id) ?: new TestEntity4(name: "TestEntity4.$id").save(failOnError:true, flush:true)
        return [testEntity1:m1, testEntity2:m2, testEntity3:m3, testEntity4:m4, list:recentDomainService.getHistory(request, TestEntity1)]
    }
}
