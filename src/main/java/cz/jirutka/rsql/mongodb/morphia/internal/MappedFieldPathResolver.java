/*
 * The MIT License
 *
 * Copyright 2013-2014 Jakub Jirutka <jakub@jirutka.cz>.
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

import cz.jirutka.rsql.mongodb.morphia.RSQLValidationException;
import org.mongodb.morphia.annotations.Reference;
import org.mongodb.morphia.mapping.MappedField;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.query.ValidationException;

public class MappedFieldPathResolver {

    private final Mapper mapper;


    /**
     * @param mapper The Morphia mapper used for resolving the field.
     */
    public MappedFieldPathResolver(Mapper mapper) {
        this.mapper = mapper;
    }


    /**
     * Resolves a mapped field and validates whether it exists.
     *
     * @param fieldPath The field name or a path of the field inside
     *        a subdocument(s) (using dot notation).
     * @param entityClass A class of the {@link org.mongodb.morphia.annotations.Entity Entity}
     *        that hold the field.
     *
     * @return A {@code MappedFieldPath} instance that holds resolved
     *         field path, {@link MappedField} and
     *         {@linkplain #resolveTargetValueType(MappedField) target type}.
     * @throws RSQLValidationException If the field does not exists or invalid use
     *         of dot notation.
     */
    public MappedFieldPath resolveFieldPath(String fieldPath, Class<?> entityClass) throws RSQLValidationException {

        // this will be modified by Mapper.validate()
        StringBuilder mutablePath = new StringBuilder(fieldPath);

        try {
            // we don't wanna validate the value, just find the mapped field
            MappedField mf = Mapper.validate(entityClass, mapper, mutablePath, null, "nullValue", true, false);

            if (mf.hasAnnotation(Reference.class)) {
                mutablePath.append(".$id");
            }

            Class<?> type = resolveTargetValueType(mf);

            return new MappedFieldPath(mutablePath.toString(), mf, type);

        } catch (ValidationException ex) {
            throw new RSQLValidationException("Could not find matching field for path: " + fieldPath, ex);
        }
    }

    /**
     * Resolves a target type of the mapped field to which a query argument
     * should be converted. If the mapped field is a collection, then it
     * returns generic type of the collection. If the mapped field is
     * annotated as a {@linkplain Reference}, then it returns type of the
     * referenced entity {@linkplain org.mongodb.morphia.annotations.Id Id}.
     */
    protected Class<?> resolveTargetValueType(MappedField mf) {
        // subType/subClass is actually a generic type...
        Class<?> type = (mf.isMultipleValues() && mf.getSubType() != null) ? mf.getSubClass() : mf.getType();

        if (mf.hasAnnotation(Reference.class)) {
            MappedField idField = mapper.getMappedClass(type).getMappedIdField();
            return resolveTargetValueType(idField);

        } else {
            return type;
        }
    }
}
