package net.moewes.quarkus.odata.repository;

import lombok.Data;
import lombok.ToString;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;

@Data
@ToString
public class Parameter {

    private String name;
    private String typeName;
    private String typeKind;
    private boolean bindingParameter;
    private String entityType;
    private EdmPrimitiveTypeKind edmType;
    private boolean collection;
}
