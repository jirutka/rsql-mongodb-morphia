package org.marchev.rsql.mongodb.springdata;

import org.springframework.data.mongodb.core.query.Criteria;

/**
 * Created by martin on 5/18/15.
 */
public interface SimpleCriteriaOperator {

    /**
     * Creates a simple criteria operator for a given
     * field and a value.
     *
     * @param field
     * @param value
     * @return
     */
    Criteria apply(String field, Object value);

}
