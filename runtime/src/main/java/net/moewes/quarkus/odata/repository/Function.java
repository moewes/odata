package net.moewes.quarkus.odata.repository;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Function {

    private String name;
    private String entitySet;
    private Map<String, EntityProperty> parameterMap;
    private EntityProperty returnType;
}
