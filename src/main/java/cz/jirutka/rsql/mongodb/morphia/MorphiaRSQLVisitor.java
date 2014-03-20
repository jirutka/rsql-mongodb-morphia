package cz.jirutka.rsql.mongodb.morphia;

import cz.jirutka.rsql.parser.ast.*;
import net.jcip.annotations.ThreadSafe;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.mapping.MappedField;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.query.*;

import java.util.Collection;

import static org.mongodb.morphia.query.CriteriaJoin.AND;
import static org.mongodb.morphia.query.CriteriaJoin.OR;
import static org.mongodb.morphia.query.FilterOperator.*;

/**
 * Implementation of {@link cz.jirutka.rsql.parser.ast.RSQLVisitor} that
 * converts {@linkplain cz.jirutka.rsql.parser.RSQLParser RSQL query} to
 * MongoDB/Morphia {@link Criteria}.
 */
@ThreadSafe
public class MorphiaRSQLVisitor extends NoArgRSQLVisitorAdapter<Criteria> {

    private final Class<?> entityClass;

    private final Mapper mapper;

    private final StringConverter converter;

    /**
     * Query object is used only, and only to create FieldCriteria that
     * requires it, but uses it just to get {@link Mapper} and an entity class.
     * The inner state of the Query object is not modified by this class, nor
     * FieldCriteria, so it's thread safe.
     */
    private final QueryImpl<?> query;


    /**
     * Creates a new instance of {@code MorphiaRSQLVisitor} for the specified
     * entity class.
     *
     * @param entityClass A class of the {@link org.mongodb.morphia.annotations.Entity Entity}
     *                    to create a {@link Criteria} for.
     * @param mapper The Morphia mapper used for validation and determining
     *               a field type.
     * @param converter
     */
    public MorphiaRSQLVisitor(Class<?> entityClass, Mapper mapper, StringConverter converter) {
        this.entityClass = entityClass;
        this.mapper = mapper;
        this.converter = converter;

        Datastore ds = mapper.getDatastoreProvider().get();
        this.query = (QueryImpl<?>) ds.createQuery(entityClass);
    }

    /**
     * The {@link Query} object should not be used in this class, it's just an
     * workaround for {@link FieldCriteria}, so it should not be used outside
     * of this code.
     *
     * @param query The query object that hold an entity class and
     *              {@link Mapper}. It'll be directly used to create
     *              instances of {@link FieldCriteria}, but not bound with it.
     * @param converter The arguments parser to use.
     */
    MorphiaRSQLVisitor(Query<?> query, StringConverter converter) {
        if (! (query instanceof QueryImpl)) {
            throw new IllegalStateException("query object is not instance of QueryImpl");
        }
        this.query = (QueryImpl) query;
        this.mapper = this.query.getDatastore().getMapper();
        this.entityClass = query.getEntityClass();
        this.converter = converter;
    }


    public Criteria visit(AndNode node) {
        return createContainer(node, AND);
    }

    public Criteria visit(OrNode node) {
        return createContainer(node, OR);
    }

    public Criteria visit(EqualNode node) {
        return createSingleValueCriteria(node, EQUAL);
    }

    public Criteria visit(InNode node) {
        return createMultiValueCriteria(node, IN);
    }

    public Criteria visit(GreaterThanOrEqualNode node) {
        return createSingleValueCriteria(node, GREATER_THAN_OR_EQUAL);
    }

    public Criteria visit(GreaterThanNode node) {
        return createSingleValueCriteria(node, GREATER_THAN);
    }

    public Criteria visit(LessThanOrEqualNode node) {
        return createSingleValueCriteria(node, LESS_THAN_OR_EQUAL);
    }

    public Criteria visit(LessThanNode node) {
        return createSingleValueCriteria(node, LESS_THAN);
    }

    public Criteria visit(NotEqualNode node) {
        return createSingleValueCriteria(node, NOT_EQUAL);
    }

    public Criteria visit(NotInNode node) {
        return createMultiValueCriteria(node, NOT_IN);
    }


    /**
     * Creates a field criteria for the operator that expects exactly one
     * argument; throws exception when the node contains more than one argument
     * or no argument at all.
     *
     * @param node The comparison node to extract selector and argument from.
     * @param operator The (Morphia) operator to create criteria for.
     * @return A field criteria for the given comparison.
     * @throws RSQLValidationException If the node contains more or less than
     *         one argument, or a matching field for the selector cannot be found.
     */
    protected FieldCriteria createSingleValueCriteria(ComparisonNode node, FilterOperator operator) {

        if (node.getArguments().size() != 1) {
            throw new RSQLValidationException("Single argument excepted, but got: " + node.getArguments());
        }
        String selector = node.getSelector();
        Class<?> targetType = determineFieldType(selector);
        Object value = converter.convert(node.getArguments().get(0), targetType);

        return createCriteria(selector, operator, value);
    }

    /**
     * Creates a field criteria for the operator that can accept multiple
     * arguments.
     *
     * @param node The comparison node to extract selector and argument from.
     * @param operator The (Morphia) operator to create criteria for.
     * @return A field criteria for the given comparison.
     * @throws RSQLValidationException If a matching field for the selector
     *         cannot be found.
     */
    protected FieldCriteria createMultiValueCriteria(ComparisonNode node, FilterOperator operator) {

        String selector = node.getSelector();
        Class<?> targetType = determineFieldType(selector);
        Collection<?> values = converter.convert(node.getArguments(), targetType);

        return createCriteria(selector, operator, values);
    }

    protected FieldCriteria createCriteria(String fieldPath, FilterOperator operator, Object value) {

        return new FieldCriteria(query, fieldPath, operator, value, false, false) {
            // subclass is needed just to access the protected constructor.
        };
    }

    protected Class<?> determineFieldType(String selector) {
        try {
            // we don't wanna validate the value, just find the mapped field
            MappedField mf = Mapper.validate(entityClass, mapper,
                    new StringBuilder(selector), null, "nullValue", true, false);

            // subType/subClass is actually a generic type...
            return mf.isMultipleValues() && mf.getSubType() != null ? mf.getSubClass() : mf.getType();

        } catch (ValidationException ex) {
            throw new RSQLValidationException("Could not find matching field for selector: " + selector, ex);
        }
    }


    private Criteria createContainer(LogicalNode node, CriteriaJoin cj) {

        CriteriaContainer parent = new CriteriaContainerImpl(cj) {
            // subclass is needed just to access the protected constructor.
        };
        for (Node child : node) {
            parent.add( child.accept(this) );
        }
        return parent;
    }
}
