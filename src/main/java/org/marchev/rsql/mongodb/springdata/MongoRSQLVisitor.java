package org.marchev.rsql.mongodb.springdata;

import cz.jirutka.rsql.parser.ast.*;
import net.jcip.annotations.ThreadSafe;
import org.marchev.rsql.mongodb.springdata.exception.RSQLValidationException;
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
    private static final Map<ComparisonOperator, CriteriaOperator> OPERATORS_MAP = new HashMap<ComparisonOperator, CriteriaOperator>() {{
        put( MongoRSQLOperators.EQUAL,                 (criteria, arg)-> { return criteria.is(arg);  });
        put( MongoRSQLOperators.GREATER_THAN_OR_EQUAL, (criteria, arg)-> { return criteria.gte(arg); });
        put( MongoRSQLOperators.GREATER_THAN,          (criteria, arg)-> { return criteria.gt(arg);  });
        put( MongoRSQLOperators.LESS_THAN_OR_EQUAL,    (criteria, arg)-> { return criteria.lte(arg); });
        put( MongoRSQLOperators.LESS_THAN,             (criteria, arg)-> { return criteria.lt(arg);  });
        put( MongoRSQLOperators.NOT_EQUAL,             (criteria, arg)-> { return criteria.ne(arg);  });
        put( MongoRSQLOperators.IN,                    (criteria, arg)-> { return criteria.in(arg);  });
        put( MongoRSQLOperators.NOT_IN,                (criteria, arg)-> { return criteria.nin(arg); });
        put( MongoRSQLOperators.ALL,                   (criteria, arg)-> { return criteria.all(arg); });
    }};

    private Criteria criteria;

    /**
     * Creates a new instance of {@code MongoRSQLVisitor}.
     */
    public MongoRSQLVisitor() {
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
        Criteria criteria = new Criteria().where(extractCriteriaField(node));
        CriteriaOperator criteriaOperator = OPERATORS_MAP.get(node.getOperator());
        return criteriaOperator.apply(criteria, extractArguments(node));
    }

    private String extractCriteriaField(ComparisonNode node) {
            return node.getSelector();
    }

    private Object extractArguments(ComparisonNode node) {
        return node.getArguments().size() == 1 ? node.getArguments().get(0) : node.getArguments().toArray();
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
