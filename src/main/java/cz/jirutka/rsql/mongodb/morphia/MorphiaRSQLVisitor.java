package cz.jirutka.rsql.mongodb.morphia;

import cz.jirutka.rsql.mongodb.morphia.internal.MappedFieldPath;
import cz.jirutka.rsql.mongodb.morphia.internal.SimpleFieldCriteria;
import cz.jirutka.rsql.parser.ast.*;
import net.jcip.annotations.ThreadSafe;
import org.mongodb.morphia.annotations.Reference;
import org.mongodb.morphia.mapping.MappedField;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.query.*;

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
    }


    public Criteria visit(AndNode node) {
        return createContainer(node, AND);
    }

    public Criteria visit(OrNode node) {
        return createContainer(node, OR);
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


    /**
     * Creates a field criteria for the given node and operator.
     *
     * @param node The comparison node to extract selector and argument from.
     * @param operator The (Morphia) operator to create criteria for.
     * @return A field criteria for the given comparison.
     * @throws RSQLValidationException If the node contains more or less than
     *         one argument, or a matching field for the selector cannot be found.
     */
    protected AbstractCriteria createCriteria(ComparisonNode node, FilterOperator operator) {

        if (!isMultiValuesFilter(operator) && node.getArguments().size() != 1) {
            throw new RSQLValidationException("Single argument excepted, but got: " + node.getArguments());
        }
        MappedFieldPath mfp = resolveMappedField(node.getSelector());
        Class<?> targetType = determineFieldType(mfp.getMappedField());

        Object value = isMultiValuesFilter(operator)
                ? converter.convert(node.getArguments(), targetType)
                : converter.convert(node.getArguments().get(0), targetType);

        Object mappedValue = mapper.toMongoObject(mfp.getMappedField(), null, value);

        String fieldPath = mfp.isReference() ? mfp.getFieldPath() + ".$id" : mfp.getFieldPath();

        return new SimpleFieldCriteria(fieldPath, operator, mappedValue);
    }

    protected MappedFieldPath resolveMappedField(String selector) {
        try {
            return MappedFieldPath.resolveFieldPath(selector, entityClass, mapper);

        } catch (ValidationException ex) {
            throw new RSQLValidationException("Could not find matching field for selector: " + selector, ex);
        }
    }

    protected Class<?> determineFieldType(MappedField mf) {
        // subType/subClass is actually a generic type...
        Class<?> type = (mf.isMultipleValues() && mf.getSubType() != null) ? mf.getSubClass() : mf.getType();

        if (mf.hasAnnotation(Reference.class)) {
            MappedField idField = mapper.getMappedClass(type).getMappedIdField();
            return determineFieldType(idField);

        } else {
            return type;
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

    private boolean isMultiValuesFilter(FilterOperator operator) {
        return operator == FilterOperator.IN || operator == FilterOperator.NOT_IN;
    }
}
