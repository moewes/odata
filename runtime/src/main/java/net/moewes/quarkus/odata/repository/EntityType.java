package net.moewes.quarkus.odata.repository;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EntityType {

    private String name;
    private String className;
    private Map<String, EntityProperty> propertyMap;
}
