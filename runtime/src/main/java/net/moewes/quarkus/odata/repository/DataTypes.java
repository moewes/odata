package net.moewes.quarkus.odata.repository;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.UUID;

@Getter
@AllArgsConstructor
public enum DataTypes {

    STRING(EdmPrimitiveTypeKind.String, String.class),
    INT(EdmPrimitiveTypeKind.Int32, int.class),
    BOOLEAN(EdmPrimitiveTypeKind.Boolean, boolean.class),
    DATE(EdmPrimitiveTypeKind.Date, LocalDate.class),
    TIME(EdmPrimitiveTypeKind.TimeOfDay, LocalTime.class),
    GUID(EdmPrimitiveTypeKind.Guid, UUID.class);

    private final EdmPrimitiveTypeKind edmPrimitiveTypeKind;
    private final Class clazz;

    public static EdmPrimitiveTypeKind getEdmTypeForClassName(String className) {
        return Arrays.stream(values())
                .filter(item -> item.clazz.getName().equals(className))
                .findFirst()
                .map(item -> item.edmPrimitiveTypeKind)
                .orElse(EdmPrimitiveTypeKind.String);
    }

    public static Class getClassForEdmType(EdmPrimitiveTypeKind edmPrimitiveTypeKind) {
        return Arrays.stream(values())
                .filter(item -> item.edmPrimitiveTypeKind.equals(edmPrimitiveTypeKind))
                .findFirst()
                .map(item -> item.clazz)
                .orElse(String.class);
    }
}
