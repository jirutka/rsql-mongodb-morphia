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
package cz.jirutka.rsql.mongodb.morphia;

import cz.jirutka.rsql.mongodb.parser.MongoRSQLNodesFactory;
import cz.jirutka.rsql.parser.RSQLParser;
import cz.jirutka.rsql.parser.RSQLParserException;
import cz.jirutka.rsql.parser.ast.Node;
import lombok.Getter;
import lombok.Setter;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.DatastoreImpl;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.query.Criteria;
import org.mongodb.morphia.query.Query;

public class MorphiaRSQL {

    @Getter
    private final Datastore datastore;

    @Getter @Setter
    private StringConverter converter = new DefaultStringConverter();

    @Getter @Setter
    private RSQLParser rsqlParser = new RSQLParser(new MongoRSQLNodesFactory());

    // lazy initialized
    private Mapper mapper;


    public MorphiaRSQL(Datastore datastore) {
        this.datastore = datastore;
    }


    public Criteria createCriteria(String rsql, Class<?> entityClass) {

        Node rootNode = parse(rsql);

        MorphiaRSQLVisitor visitor = new MorphiaRSQLVisitor(entityClass, getMapper(), converter);

        return rootNode.accept(visitor);
    }

    public <T> Query<T> createQuery(String rsql, Class<T> entityClass) {

        Node rootNode = parse(rsql);

        Query<T> query = datastore.createQuery(entityClass);
        MorphiaRSQLVisitor visitor = new MorphiaRSQLVisitor(entityClass, getMapper(), converter);

        query.and(rootNode.accept(visitor));

        return query;
    }


    protected Node parse(String rsql) {
        try {
            return rsqlParser.parse(rsql);

        } catch (RSQLParserException ex) {
            throw new RSQLException(ex);
        }
    }

    private Mapper getMapper() {
        if (mapper == null) {
            if (! (datastore instanceof DatastoreImpl)) {
                throw new IllegalStateException("datestore is not instance of DatastoreImpl");
            }
            mapper = ((DatastoreImpl) datastore).getMapper();
        }
        return mapper;
    }
}
