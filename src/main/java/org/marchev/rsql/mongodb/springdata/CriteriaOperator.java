package org.marchev.rsql.mongodb.springdata;

import org.springframework.data.mongodb.core.query.Criteria;

/**
 * Created by martin on 5/18/15.
 */
public interface CriteriaOperator {

    /**
     * Applies an operator to a given Criteria.
     *
     * @param argument
     * @return
     */
    Criteria apply(Criteria criteria, Object argument);

}
