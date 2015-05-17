/*
 * The MIT License
 *
 * Copyright 2013-2014 Czech Technical University in Prague.
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
package org.marchev.rsql.mongodb.springdata

import spock.lang.Specification

class AbstractStringConverterTest extends Specification {

    private converter = Spy(AbstractStringConverter)


    def 'convert collection of values'() {
        given:
            def values = ['123', '456', '789']
        when:
            def result = converter.convert(values, Integer)
        then:
            values.each { val ->
                1 * converter.convert(_ as String, Integer) >> { src -> val.toInteger() }
            }
        and:
           result == values*.toInteger()
    }

    def 'return collection of values unchanged when String'() {
        given:
            def values = ['chunky', 'bacon', 'here!']
        when:
            def result = converter.convert(values, String)
        then:
            0 * converter.convert(_ as String, Integer)
        and:
            result == values
    }
}
