package net.moewes.quarkus.odata.repository;

import lombok.Data;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;

@Data
public class EntityProperty {

    private String name;
    private EdmPrimitiveTypeKind edmType;
    private boolean key;
    private String getterName;
}
