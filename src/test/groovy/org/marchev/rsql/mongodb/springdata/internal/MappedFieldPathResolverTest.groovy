/*
 * The MIT License
 *
 * Copyright 2013-2014 Jakub Jirutka <jakub@jirutka.cz>.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.marchev.rsql.mongodb.springdata.internal

import org.marchev.rsql.mongodb.springdata.TestUtils
import org.marchev.rsql.mongodb.springdata.fixtures.ChildEntity
import org.marchev.rsql.mongodb.springdata.fixtures.RootEntity
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

@Unroll
class MappedFieldPathResolverTest extends Specification {

    @Shared dataStore = TestUtils.createDatastore()

    def resolver = new MappedFieldPathResolver(dataStore.mapper)


    def 'resolve mapped field path and value type for selector: #selector'() {
        when:
            def mfp = resolver.resolveFieldPath(selector, RootEntity)
        then:
            mfp.fieldPath                  == mongoFieldPath
            mfp.mappedField.javaFieldName  == javaFieldName
            mfp.mappedField.type           == javaFieldType
            mfp.mappedField.declaringClass == declClass
            mfp.targetValueType            == valueType
        where:
            selector             || mongoFieldPath      | javaFieldName | javaFieldType | declClass   | valueType
            'year'               || 'year'              | 'year'        | int           | RootEntity  | int
            'title'              || 'name'              | 'title'       | String        | RootEntity  | String
            'genres'             || 'genres'            | 'genres'      | Set           | RootEntity  | String
            'director'           || 'director'          | 'director'    | ChildEntity   | RootEntity  | ChildEntity
            'director.birthdate' || 'director.birthdate'| 'birthdate'   | Date          | ChildEntity | Date
            'actors'             || 'actors'            | 'actors'      | List          | RootEntity  | ChildEntity
            'actors.movies.name' || 'actors.movies.name'| 'title'       | String        | RootEntity  | String
            'parent'             || 'parent.$id'        | 'parent'      | RootEntity    | RootEntity  | Long
    }
}
