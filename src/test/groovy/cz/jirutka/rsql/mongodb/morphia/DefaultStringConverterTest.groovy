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
package cz.jirutka.rsql.mongodb.morphia

import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import spock.lang.Specification
import spock.lang.Unroll

import java.util.concurrent.TimeUnit

@Unroll
class DefaultStringConverterTest extends Specification {

    static final DateTimeZone UTC = DateTimeZone.forOffsetHours(0)
    static final DateTimeZone CET = DateTimeZone.forOffsetHours(1)
    static final DateTimeZone GMT9 = DateTimeZone.forOffsetHours(9)

    def converter = new DefaultStringConverter()

    static {
        TimeZone.setDefault(TimeZone.getTimeZone('CET'))
    }


    def 'parse #type value: #value'() {
        expect:
            converter.convert(value, type) == expected
        where:
            value     | type     || expected
            'stringy' | String   || 'stringy'
            '42'      | Integer  || 42
            '84.4'    | Double   || 84.4
            '42.2'    | Float    || new Float(42.2)
            '6666666' | Long     || 6666666L
    }

    def 'parse Boolean value: #value'() {
        expect:
            converter.convert(value, Boolean) == expected
        where:
            value << ['true',  'TrUe',  'yes', 'Yes', 'y',
                      'false', 'FaLse', 'no',  'No',  'n' ]
            expected << [true] * 5 + [false] * 5
    }

    def 'parse Enum value case-insensitively'() {
        expect:
            converter.convert(value, TimeUnit) == expected
        where:
            value    | expected
            'DAYS'   | TimeUnit.DAYS
            'days'   | TimeUnit.DAYS
            'HoUrS'  | TimeUnit.HOURS
    }

    def 'parse Date value: #value'() {
        expect:
            converter.convert(value, Date) == expected
        where:
            value                       | expected
            '1989-11-17'                | new DateTime(1989, 11, 17, 0, 0).toDate()
            '2014-03-17T15:30:42'       | new DateTime(2014, 3, 17, 15, 30, 42, CET).toDate()
            '2014-03-17T15:30:42Z'      | new DateTime(2014, 3, 17, 15, 30, 42, UTC).toDate()
            '2014-03-17T15:30:42+09:00' | new DateTime(2014, 3, 17, 15, 30, 42, GMT9).toDate()
    }

    def 'parse using valueOf() method'() {
        when:
            def returned = converter.convert('allons-y!', MockValueOf)
        then:
            returned instanceof MockValueOf
            returned.value == 'allons-y!'
    }

    def 'throw ArgumentFormatException when valueOf throw any exception'() {
        when:
            converter.convert('throw!', MockValueOf)
        then:
            thrown RSQLArgumentFormatException
    }

    def 'throw ArgumentFormatException when value is illegal #type'() {
        when:
            converter.convert(value, type)
        then:
            def ex = thrown(RSQLArgumentFormatException)
        and:
            ex.value == value
            ex.targetType == type
        where:
            value        | type
            'NaN'        | Integer
            'foo'        | TimeUnit
            'foo'        | Boolean
            '17.11.1989' | Date

            typeMsg = type == TimeUnit ? 'Enum' : type.simpleName
    }

    def 'throw ArgumentFormatException when unknown type'() {
        when:
            converter.convert('foo', DefaultStringConverter)
        then:
            thrown RSQLArgumentFormatException
    }


    static class MockValueOf {
        def value
        static MockValueOf valueOf(String s) {
            if (s == 'throw!') throw new IllegalArgumentException()
            return new MockValueOf(value: s);
        }
    }
}
