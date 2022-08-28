package net.moewes.quarkus.odata.repository;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Callable {

    private String name;
    private String entitySet;
    private String ClassName;
    private String methodName;
    private List<Parameter> parameter;
    private Parameter returnType;

    public String getMethodName() {
        return methodName != null ? methodName : name;
    }
}
