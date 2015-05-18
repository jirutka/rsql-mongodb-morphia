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

import org.marchev.rsql.mongodb.springdata.convert.SpringConversionServiceAdapter
import org.marchev.rsql.mongodb.springdata.exception.RSQLArgumentFormatException
import org.springframework.core.convert.ConversionFailedException
import org.springframework.core.convert.ConversionService
import spock.lang.Specification

class SpringConversionServiceAdapterTest extends Specification {

//    def conversionService = Mock(ConversionService)
//    def adapter = new SpringConversionServiceAdapter(conversionService)
//
//
//    def 'delegate conversion to ConversionService'() {
//        when:
//            adapter.convert('42', Integer)
//        then:
//            1 * conversionService.convert('42', Integer)
//    }
//
//    def "don't convert String"() {
//        when:
//            adapter.convert('allons-y!', String)
//        then:
//            0 * conversionService._
//    }
//
//    def 'throw ArgumentFormatException when conversion fails'() {
//        setup:
//            conversionService._ >> { throw new ConversionFailedException(null, null, null, new RuntimeException()) }
//        when:
//            adapter.convert('bang!', Date)
//        then:
//            thrown RSQLArgumentFormatException
//    }
}
