package net.moewes.quarkus.odata.repository;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Action {

    private String name;
    private String entitySet;
    private List<Parameter> parameter;
    private Parameter returnType;
}
