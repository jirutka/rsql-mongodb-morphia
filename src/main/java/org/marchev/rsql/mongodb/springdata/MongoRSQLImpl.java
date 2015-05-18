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
package org.marchev.rsql.mongodb.springdata;

import cz.jirutka.rsql.parser.RSQLParser;
import cz.jirutka.rsql.parser.RSQLParserException;
import cz.jirutka.rsql.parser.ast.Node;
import org.marchev.rsql.mongodb.springdata.exception.RSQLException;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

public class MongoRSQLImpl implements MongoRSQL {

    private ConversionService conversionService;

    private RSQLParser rsqlParser = new RSQLParser(MongoRSQLOperators.mongoOperators());

    public MongoRSQLImpl(ConversionService conversionService) {
        this.conversionService = conversionService;
    }


    public Criteria createCriteria(String rsql, Class<?> entityClass) {
        Node rootNode = parse(rsql);
        MongoRSQLVisitor visitor = new MongoRSQLVisitor(entityClass, conversionService);
        return rootNode.accept(visitor);
    }

    public Query createQuery(String rsql, Class<?> entityClass) {
        Criteria criteria = createCriteria(rsql, entityClass);
        return new Query(criteria);
    }


    protected Node parse(String rsql) {
        try {
            return rsqlParser.parse(rsql);

        } catch (RSQLParserException ex) {
            throw new RSQLException(ex);
        }
    }
}
