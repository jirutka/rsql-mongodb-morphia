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
package cz.jirutka.rsql.mongodb.morphia.internal;

import com.mongodb.DBObject;
import lombok.Getter;
import org.mongodb.morphia.query.AbstractCriteria;
import org.mongodb.morphia.query.FilterOperator;

import java.util.HashMap;
import java.util.Map;

/**
 * A simple replacement for the {@link org.mongodb.morphia.query.FieldCriteria}
 * that just holds values and implements the {@link #addTo(com.mongodb.DBObject)}
 * method, i.e. omits the resolving magic.
 */
@Getter
public class SimpleFieldCriteria extends AbstractCriteria {

    private final String fieldName;
    private final FilterOperator operator;
    private final Object value;


    /**
     * @param fieldName The field name or a path of the field inside
     *        a subdocument(s) (using dot notation). It must be already
     *        translated to the actual name of the field in DB.
     * @param operator The filter operator.
     * @param value The value, must be a mongo-compatible object (convert it
     *        with the {@link org.mongodb.morphia.mapping.Mapper Mapper}).
     */
    public SimpleFieldCriteria(String fieldName, FilterOperator operator, Object value) {
        this.fieldName = fieldName;
        this.operator = operator;
        this.value = value;
    }

    // copied from FieldCriteria
    @Override
    public void addTo(DBObject obj) {
        if (FilterOperator.EQUAL == operator) {
            obj.put(fieldName, value); // no operator, prop equals value

        } else {
            Object object = obj.get(fieldName); // operator within inner object
            Map<String, Object> inner;
            if (!(object instanceof Map)) {
                inner = new HashMap<>();
                obj.put(fieldName, inner);
            } else {
                inner = (Map<String, Object>) object;
            }
            inner.put(operator.val(), value);
        }
    }

    @Override
    public String toString() {
        return fieldName + " " + operator.val() + " " + value;
    }
}
