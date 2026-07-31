/*
 * Copyright (c) 2026.
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jast;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.Type;
import org.junit.jupiter.api.Test;
import org.reflections.Reflections;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Pure unit test (no Spring context, no database) that validates all JPA entity
 * definitions against MariaDB compatibility rules documented in AGENTS.md.
 *
 * <p>Catches issues like TEXT primary keys and missing equals/hashCode on
 * JSON-mapped inner classes before they reach deployment.
 */
class DatabaseCompatibilityTest {

    private static final String ENTITY_PACKAGE = "org.hkijena.jast.model.entities";

    @Test
    void entityIdFieldsMustNotUseTextColumnDefinition() {
        Set<Class<?>> entities = getEntityClasses();

        for (Class<?> entity : entities) {
            for (Field field : entity.getDeclaredFields()) {
                if (field.isAnnotationPresent(Id.class)) {
                    Column column = field.getAnnotation(Column.class);
                    if (column != null) {
                        String colDef = column.columnDefinition().toUpperCase();
                        assertTrue(
                                colDef.isEmpty() || !colDef.equals("TEXT") && !colDef.equals("BLOB"),
                                entity.getSimpleName() + "." + field.getName()
                                        + " is @Id but uses columnDefinition=\"TEXT\" (or BLOB). "
                                        + "MariaDB cannot index TEXT/BLOB columns without a key length. "
                                        + "Use VARCHAR(n) instead (e.g. VARCHAR(36) for UUIDs)."
                        );
                    }
                }
            }
        }
    }

    @Test
    void jsonMappedCollectionElementClassesMustOverrideEqualsAndHashCode() {
        Set<Class<?>> entities = getEntityClasses();

        for (Class<?> entity : entities) {
            for (Field field : entity.getDeclaredFields()) {
                if (!field.isAnnotationPresent(Type.class)) {
                    continue;
                }
                Type typeAnnotation = field.getAnnotation(Type.class);
                if (!typeAnnotation.value().equals(JsonType.class)) {
                    continue;
                }

                Class<?> elementType = resolveCollectionElementType(field);
                if (elementType == null || elementType.isEnum() || elementType.getName().startsWith("java.")) {
                    continue;
                }

                assertOverridesEqualsAndHashCode(entity, field.getName(), elementType);
            }
        }
    }

    @Test
    void allEntitiesAreScanned() {
        Set<Class<?>> entities = getEntityClasses();
        assertTrue(entities.size() >= 8,
                "Expected at least 8 entity classes in " + ENTITY_PACKAGE
                        + " but found " + entities.size() + ": " + entities);
    }

    private Set<Class<?>> getEntityClasses() {
        Reflections reflections = new Reflections(ENTITY_PACKAGE);
        return reflections.getTypesAnnotatedWith(Entity.class);
    }

    private Class<?> resolveCollectionElementType(Field field) {
        if (!Collection.class.isAssignableFrom(field.getType())) {
            return null;
        }
        if (field.getGenericType() instanceof ParameterizedType pt) {
            java.lang.reflect.Type[] typeArgs = pt.getActualTypeArguments();
            if (typeArgs.length > 0 && typeArgs[0] instanceof Class<?> elementClass) {
                return elementClass;
            }
        }
        return null;
    }

    private void assertOverridesEqualsAndHashCode(Class<?> entity, String fieldName, Class<?> elementType) {
        Set<String> declaredMethodNames = Arrays.stream(elementType.getDeclaredMethods())
                .map(Method::getName)
                .collect(Collectors.toSet());

        if (!declaredMethodNames.contains("equals")) {
            fail(entity.getSimpleName() + "." + fieldName
                    + " is JSON-mapped with element type " + elementType.getSimpleName()
                    + " which does not override equals(). "
                    + "Hibernate Types requires equals/hashCode for dirty checking.");
        }
        if (!declaredMethodNames.contains("hashCode")) {
            fail(entity.getSimpleName() + "." + fieldName
                    + " is JSON-mapped with element type " + elementType.getSimpleName()
                    + " which does not override hashCode(). "
                    + "Hibernate Types requires equals/hashCode for dirty checking.");
        }
    }
}
