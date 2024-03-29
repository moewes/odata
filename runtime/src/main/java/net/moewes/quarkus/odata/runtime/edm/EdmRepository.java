package net.moewes.quarkus.odata.runtime.edm;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.spi.CDI;
import net.moewes.quarkus.odata.repository.Callable;
import net.moewes.quarkus.odata.repository.EntitySet;
import net.moewes.quarkus.odata.repository.EntityType;
import net.moewes.quarkus.odata.repository.Function;
import org.apache.olingo.commons.api.ex.ODataRuntimeException;

import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class EdmRepository {

    private final Map<String, EntityType> entities = new HashMap<>();
    private final Map<String, EntitySet> entitySets = new HashMap<>();
    private final Map<String, Callable> actions = new HashMap<>();
    private final Map<String, Function> functions = new HashMap<>();

    public void registerEntity(final String name, final EntityType entityType) {

        entities.put(name, entityType);
    }

    public void registerEntitySet(final String name, final EntitySet entitySet) {

        entitySets.put(name, entitySet);
    }

    public void registerAction(String name, final String entitySetName, final Callable action) {

        actions.put(name, action);
    }

    public void registerFunction(String name, final String entitySetName, final Function function) {

        functions.put(name, function);
    }

    public List<String> getEntityTypeNames() {

        return entities.values().stream().map(EntityType::getName).collect(Collectors.toList());
    }

    public List<String> getEntitySetNames() {

        return entitySets.values().stream().map(EntitySet::getName).collect(Collectors.toList());
    }

    public List<String> getActionNames() {
        List<String> result = new ArrayList<>();
        result.addAll(actions.values()
                .stream()
                .map(Callable::getName)
                .collect(Collectors.toList()));
        return result;
    }

    public List<String> getFunctionNames() {
        List<String> result = new ArrayList<>();
        result.addAll(functions.values()
                .stream()
                .map(Function::getName)
                .collect(Collectors.toList()));
        return result;
    }

    public Optional<EntitySet> findEntitySet(String entitySetName) {
        return Optional.ofNullable(entitySets.get(entitySetName));
    }

    public Optional<EntityType> findEntityType(String entityTypeName) {
        return Optional.ofNullable(entities.get(entityTypeName));
    }

    public Optional<Callable> findAction(String actionName) {
        return Optional.ofNullable(actions.get(actionName));
    }

    @Deprecated
    public Object getServiceBean(EntitySet entitySet) {

        try {
            Class<?> beanClass = Class.forName(entitySet.getBeanClassName(), true,
                    Thread.currentThread().getContextClassLoader());
            return CDI.current().select(beanClass, Default.Literal.INSTANCE).get();
        } catch (ClassNotFoundException e) {
            throw new ODataRuntimeException("Service class " + entitySet.getBeanClassName() + " " +
                    "not found");
        }
    }

    public Optional<EntitySet> findEntitySetForEntityTypeName(String name) {
        for (EntitySet entitySet : entitySets.values()) {
            if (entitySet.getEntityType().equals(name)) {
                return Optional.of(entitySet);
            }
        }
        return Optional.empty();
    }
}

