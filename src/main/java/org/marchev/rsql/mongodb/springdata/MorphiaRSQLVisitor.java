package org.marchev.rsql.mongodb.springdata;

import org.marchev.rsql.mongodb.springdata.internal.MappedFieldPath;
import org.marchev.rsql.mongodb.springdata.internal.MappedFieldPathResolver;
import org.marchev.rsql.mongodb.springdata.internal.SimpleCriteriaContainer;
import org.marchev.rsql.mongodb.springdata.internal.SimpleFieldCriteria;
import cz.jirutka.rsql.parser.ast.*;
import net.jcip.annotations.ThreadSafe;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.query.Criteria;
import org.mongodb.morphia.query.CriteriaContainer;
import org.mongodb.morphia.query.CriteriaJoin;
import org.mongodb.morphia.query.FilterOperator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mongodb.morphia.query.CriteriaJoin.AND;
import static org.mongodb.morphia.query.CriteriaJoin.OR;

/**
 * Implementation of {@link cz.jirutka.rsql.parser.ast.RSQLVisitor} that
 * converts {@linkplain cz.jirutka.rsql.parser.RSQLParser RSQL query} to
 * MongoDB/Morphia {@link Criteria}.
 */
@ThreadSafe
public class MorphiaRSQLVisitor extends NoArgRSQLVisitorAdapter<Criteria> {

    @SuppressWarnings("unchecked")
    private static final Map<ComparisonOperator, FilterOperator> OPERATORS_MAP = new HashMap() {{
            put( MongoRSQLOperators.EQUAL,                 FilterOperator.EQUAL                 );
            put( MongoRSQLOperators.IN,                    FilterOperator.IN                    );
            put( MongoRSQLOperators.GREATER_THAN_OR_EQUAL, FilterOperator.GREATER_THAN_OR_EQUAL );
            put( MongoRSQLOperators.GREATER_THAN,          FilterOperator.GREATER_THAN          );
            put( MongoRSQLOperators.LESS_THAN_OR_EQUAL,    FilterOperator.LESS_THAN_OR_EQUAL    );
            put( MongoRSQLOperators.LESS_THAN,             FilterOperator.LESS_THAN             );
            put( MongoRSQLOperators.NOT_EQUAL,             FilterOperator.NOT_EQUAL             );
            put( MongoRSQLOperators.NOT_IN,                FilterOperator.NOT_IN                );
            put( MongoRSQLOperators.ALL,                   FilterOperator.ALL                   );
    }};

    private final Class<?> entityClass;

    private final Mapper mapper;

    private final StringConverter converter;

    private final MappedFieldPathResolver fieldPathResolver;


    public MorphiaRSQLVisitor(Class<?> entityClass, Mapper mapper, StringConverter converter) {
        this(entityClass, mapper, converter, new MappedFieldPathResolver(mapper));
    }

    /**
     * Creates a new instance of {@code MorphiaRSQLVisitor} for the specified
     * entity class.
     *
     * @param entityClass A class of the {@link org.mongodb.morphia.annotations.Entity Entity}
     *                    to create a {@link Criteria} for.
     * @param mapper The Morphia mapper used for validation and determining
     *               a field type.
     * @param converter
     * @param fieldPathResolver
     */
    public MorphiaRSQLVisitor(Class<?> entityClass, Mapper mapper, StringConverter converter,
                              MappedFieldPathResolver fieldPathResolver) {
        this.entityClass = entityClass;
        this.mapper = mapper;
        this.converter = converter;
        this.fieldPathResolver = fieldPathResolver;
    }


    public Criteria visit(AndNode node) {
        return joinChildrenNodesInContainer(node, AND);
    }

    public Criteria visit(OrNode node) {
        return joinChildrenNodesInContainer(node, OR);
    }

    public Criteria visit(ComparisonNode node) {
        return createCriteria(node, OPERATORS_MAP.get(node.getOperator()));
    }


    /**
     * Creates a field criteria for the given node and operator.
     *
     * @param node The comparison node to extract selector and argument from.
     * @param operator The (Morphia) operator to create criteria for.
     * @return A field criteria for the given comparison.
     *
     * @throws RSQLValidationException If the node contains more or less than
     *         one argument, or a matching field for the selector cannot be found.
     */
    protected Criteria createCriteria(ComparisonNode node, FilterOperator operator) {

        MappedFieldPath mfp = resolveFieldPath(node.getSelector());
        Object mappedValue = convertToMappedValue(node.getArguments(), mfp, !node.getOperator().isMultiValue());

        return new SimpleFieldCriteria(mfp.getFieldPath(), operator, mappedValue);
    }

    /**
     * Resolves a mapped field path.
     *
     * @throws RSQLValidationException If the field does not exists or invalid use
     *         of dot notation.
     */
    protected MappedFieldPath resolveFieldPath(String selector) {
        return fieldPathResolver.resolveFieldPath(selector, entityClass);
    }

    /**
     * Converts the argument(s) to the target type (specified by {@code
     * MappedFieldPath}) and then to a Mongo object.
     *
     * @param arguments Single or multiple arguments in a list.
     * @param mfp The mapped field path.
     * @param singleValue Whether a single argument is expected.
     * @return An argument(s) converted to Mongo value.
     *
     * @throws RSQLArgumentFormatException
     */
    protected Object convertToMappedValue(List<String> arguments, MappedFieldPath mfp, boolean singleValue) {

        Object value = singleValue
                ? converter.convert(arguments.get(0), mfp.getTargetValueType())
                : converter.convert(arguments, mfp.getTargetValueType());

        return mapper.toMongoObject(mfp.getMappedField(), null, value);
    }


    private Criteria joinChildrenNodesInContainer(LogicalNode node, CriteriaJoin cj) {

        CriteriaContainer parent = new SimpleCriteriaContainer(cj);
        for (Node child : node) {
            parent.add( child.accept(this) );
        }
        return parent;
    }
}
