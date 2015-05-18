package org.marchev.rsql.mongodb.springdata

import cz.jirutka.rsql.parser.RSQLParser
import org.marchev.rsql.mongodb.springdata.exception.RSQLValidationException
import org.marchev.rsql.mongodb.springdata.fixtures.RootEntity
import org.springframework.core.convert.ConversionService
import org.springframework.data.mongodb.core.query.Query
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import static MongoRSQLOperators.mongoOperators

class MongoRSQLVisitorTest extends Specification {

    @Shared mongoTemplate = TestUtils.createMongoTemplate()

    // Fake ConversionService that returns the source value
    def fakeConverter = Stub(ConversionService) {
        convert(_, _) >> { val, type -> val }
    }

    def query = new Query()
    def visitor = new MongoRSQLVisitor(RootEntity, fakeConverter)


    def 'throw RSQLValidationException when field could not be found'() {
        setup:
            def rootNode = parse('illegal==666')
        when:
            rootNode.accept(visitor)
        then:
            thrown RSQLValidationException
    }

    def 'convert argument value through Converter'() {
        setup:
            def converter = Mock(StringConverter)
            def visitor = new MongoRSQLVisitor(RootEntity, mongoTemplate.mapper, converter)

        when:
            parse('year==2014').accept(visitor)
        then:
            1 * converter.convert('2014', int)

        when:
            parse('genres=in=(sci-fi,thriller)').accept(visitor)
        then:
           1 * converter.convert(['sci-fi', 'thriller'], String)
    }


    @Unroll
    def 'convert complex RSQL to Mongo query: #rsql'() {
        setup:
            def rootNode = parse(rsql)
        when:
            query.and( rootNode.accept(visitor) )
        then:
            query.queryObject == expected
        where:
            rsql                    | expected
            'a==u;b==v;c!=w'        | [ a:'u', b:'v', c: [ $ne:'w' ] ]
            'a=gt=u;a=lt=v;c==w'    | [ $and: [ [a:[$gt:'u']], [a:[$lt:'v']], [c:'w']] ]
            'a==u,b==v;c==w,d==x'   | [ $or: [ [a:'u'], [b:'v', c:'w'], [d:'x']] ]
            '(a=gt=u,a=le=v);c==d'  | [ $or: [[a:[$gt:'u']], [a:[$lte:'v']]], c:'d']
    }

    @Unroll
    def 'convert RSQL to Mongo query when key is not the same as field name: #rsql'() {
        setup:
            def rootNode = parse(rsql)
        when:
            query.and( rootNode.accept(visitor) )
        then:
            query.queryObject == expected
        where:
            rsql            | expected
            'title==Matrix' | [ name: 'Matrix' ]
            'entityId==123' | [ _id: '123' ]
    }

    def 'convert RSQL to Mongo query with <field>.$id when field is @Reference'() {
        setup:
            def rootNode = parse(rsql)
        when:
            query.and( rootNode.accept(visitor) )
        then:
            query.queryObject == expected
        where:
            rsql          | expected
            'parent==123' | [ 'parent.$id': '123' ]
    }


    ////// Helpers ////////

    def query(Closure c) {
        def query = mongoTemplate.createQuery(RootEntity)
        c.call(query)
        query
    }

    def fieldCriteria(field, operator, value) {
        new FieldCriteria(mongoTemplate.createQuery(RootEntity), field, operator, value, false, false)
    }

    def parse(String rsql) {
        new RSQLParser(mongoOperators()).parse(rsql)
    }
}
