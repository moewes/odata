package net.moewes.quarkus.odata.repository;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EntitySet {

    private String name;
    private String entityType;
    private String beanClassName;
    private List<Action> navigationBindings; // TODO Rething type
}
