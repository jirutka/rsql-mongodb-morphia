package cz.jirutka.rsql.mongodb.morphia;

import cz.jirutka.rsql.mongodb.morphia.internal.MappedFieldPath;
import cz.jirutka.rsql.mongodb.morphia.internal.MappedFieldPathResolver;
import cz.jirutka.rsql.mongodb.morphia.internal.SimpleCriteriaContainer;
import cz.jirutka.rsql.mongodb.morphia.internal.SimpleFieldCriteria;
import cz.jirutka.rsql.mongodb.parser.AllNode;
import cz.jirutka.rsql.mongodb.parser.NoArgMongoRSQLVisitorAdapter;
import cz.jirutka.rsql.parser.ast.*;
import net.jcip.annotations.ThreadSafe;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.query.Criteria;
import org.mongodb.morphia.query.CriteriaContainer;
import org.mongodb.morphia.query.CriteriaJoin;
import org.mongodb.morphia.query.FilterOperator;

import java.util.EnumSet;
import java.util.List;

import static org.mongodb.morphia.query.CriteriaJoin.AND;
import static org.mongodb.morphia.query.CriteriaJoin.OR;
import static org.mongodb.morphia.query.FilterOperator.*;

/**
 * Implementation of {@link cz.jirutka.rsql.parser.ast.RSQLVisitor} that
 * converts {@linkplain cz.jirutka.rsql.parser.RSQLParser RSQL query} to
 * MongoDB/Morphia {@link Criteria}.
 */
@ThreadSafe
public class MorphiaRSQLVisitor extends NoArgMongoRSQLVisitorAdapter<Criteria> {

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

    public Criteria visit(EqualNode node) {
        return createCriteria(node, EQUAL);
    }

    public Criteria visit(InNode node) {
        return createCriteria(node, IN);
    }

    public Criteria visit(GreaterThanOrEqualNode node) {
        return createCriteria(node, GREATER_THAN_OR_EQUAL);
    }

    public Criteria visit(GreaterThanNode node) {
        return createCriteria(node, GREATER_THAN);
    }

    public Criteria visit(LessThanOrEqualNode node) {
        return createCriteria(node, LESS_THAN_OR_EQUAL);
    }

    public Criteria visit(LessThanNode node) {
        return createCriteria(node, LESS_THAN);
    }

    public Criteria visit(NotEqualNode node) {
        return createCriteria(node, NOT_EQUAL);
    }

    public Criteria visit(NotInNode node) {
        return createCriteria(node, NOT_IN);
    }

    public Criteria visit(AllNode node) {
        return createCriteria(node, ALL);
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
        Object mappedValue = convertToMappedValue(node.getArguments(), mfp, isSingleValueFilter(operator));

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
     * @throws RSQLValidationException if single argument was expected, but
     *         given none or multiple
     * @throws cz.jirutka.rsql.mongodb.morphia.RSQLArgumentFormatException
     */
    protected Object convertToMappedValue(List<String> arguments, MappedFieldPath mfp, boolean singleValue) {

        if (singleValue && arguments.size() != 1) {
            throw new RSQLValidationException("Single argument excepted, but got: " + arguments);
        }

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

    private boolean isSingleValueFilter(FilterOperator operator) {
        return ! EnumSet.of(IN, NOT_IN, ALL).contains(operator);
    }
}
