package org.marchev.rsql.mongodb.springdata;

import cz.jirutka.rsql.parser.ast.*;
import net.jcip.annotations.ThreadSafe;
import org.marchev.rsql.mongodb.springdata.exception.RSQLValidationException;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of {@link cz.jirutka.rsql.parser.ast.RSQLVisitor} that
 * converts {@linkplain cz.jirutka.rsql.parser.RSQLParser RSQL query} to
 * MongoDB/Morphia {@link Criteria}.
 */
@ThreadSafe
public class MongoRSQLVisitor extends NoArgRSQLVisitorAdapter<Criteria> {

    @SuppressWarnings("unchecked")
    private static final Map<ComparisonOperator, SimpleCriteriaOperator> OPERATORS_MAP = new HashMap<ComparisonOperator, SimpleCriteriaOperator>() {{
            put( MongoRSQLOperators.EQUAL,                 (f, v)-> { return Criteria.where(f).is(v);       });
            put( MongoRSQLOperators.IN,                    (f, v)-> { return Criteria.where(f).in(v);       });
            put( MongoRSQLOperators.GREATER_THAN_OR_EQUAL, (f, v)-> { return Criteria.where(f).gte(v);      });
            put( MongoRSQLOperators.GREATER_THAN,          (f, v)-> { return Criteria.where(f).gt(v);       });
            put( MongoRSQLOperators.LESS_THAN_OR_EQUAL,    (f, v)-> { return Criteria.where(f).lte(v);      });
            put( MongoRSQLOperators.LESS_THAN,             (f, v)-> { return Criteria.where(f).lt(v);       });
            put( MongoRSQLOperators.NOT_EQUAL,             (f, v)-> { return Criteria.where(f).ne(v);       });
            put( MongoRSQLOperators.NOT_IN,                (f, v)-> { return Criteria.where(f).nin(v);      });
            put( MongoRSQLOperators.ALL,                   (f, v)-> { return Criteria.where(f).all(v);      });
    }};

    private final Class<?> entityClass;

    private final ConversionService converter;

    private Criteria criteria;

    /**
     * Creates a new instance of {@code MongoRSQLVisitor} for the specified
     * entity class.
     *
     * @param entityClass A class of the {@link org.mongodb.morphia.annotations.Entity Entity}
     *                    to create a {@link Criteria} for.
     * @param converter A ConversionService implementation to be used.
     */
    public MongoRSQLVisitor(Class<?> entityClass, ConversionService converter) {
        this.entityClass = entityClass;
        this.converter = converter;
        this.criteria = new Criteria();
    }


    public Criteria visit(AndNode node) {
        return joinChildrenNodesInCriteria(node);
    }

    public Criteria visit(OrNode node) {
        return joinChildrenNodesInCriteria(node);
    }

    public Criteria visit(ComparisonNode node) {
        return createCriteria(node);
    }


    /**
     * Creates a field criteria for the given node and operator.
     *
     * @param node The comparison node to extract selector and argument from.
     * @return A field criteria for the given comparison.
     *
     * @throws RSQLValidationException If the node contains more or less than
     *         one argument, or a matching field for the selector cannot be found.
     */
    protected Criteria createCriteria(ComparisonNode node) {
        SimpleCriteriaOperator criteriaOperator = OPERATORS_MAP.get(node.getOperator());
        return criteriaOperator.apply(node.getSelector(), criteriaOperator);
    }


    private Criteria joinChildrenNodesInCriteria(LogicalNode node) {
        for (Node childNode : node) {
            if (node instanceof  OrNode) {
                criteria.orOperator(childNode.accept(this));
            } else {
                criteria.andOperator(childNode.accept(this));
            }
        }

        return criteria;
    }
}
