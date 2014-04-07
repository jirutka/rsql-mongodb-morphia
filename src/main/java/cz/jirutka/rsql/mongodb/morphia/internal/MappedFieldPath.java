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

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.mongodb.morphia.annotations.Reference;
import org.mongodb.morphia.mapping.MappedField;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.query.ValidationException;

/**
 * Container for {@link MappedField} and field path.
 */
@Getter
@AllArgsConstructor
public class MappedFieldPath {

    private final MappedField mappedField;

    private final String fieldPath;


    /**
     * Resolves a mapped field and validates that it exists.
     *
     * @param fieldPath The field name or a path of the field inside
     *        a subdocument(s) (using dot notation).
     * @param entityClass A class of the {@link org.mongodb.morphia.annotations.Entity Entity}
     *        that hold the field.
     * @param mapper The Morphia mapper used for resolving the field.
     *
     * @return A {@code MappedFieldPath} instance that holds resolved
     *        {@link MappedField} and field path.
     * @throws ValidationException If the field does not exists or invalid use
     *         of dot notation.
     */
    public static MappedFieldPath resolveFieldPath(String fieldPath, Class<?> entityClass, Mapper mapper) throws ValidationException {

        // this will be modified by Mapper.validate()
        StringBuilder mutablePath = new StringBuilder(fieldPath);

        // we don't wanna validate the value, just find the mapped field
        MappedField mf = Mapper.validate(entityClass, mapper, mutablePath, null, "nullValue", true, false);

        return new MappedFieldPath(mf, mutablePath.toString());
    }

    /**
     * Whether the mapped field is a {@link Reference}.
     */
    public boolean isReference() {
        return mappedField.hasAnnotation(Reference.class);
    }
}
