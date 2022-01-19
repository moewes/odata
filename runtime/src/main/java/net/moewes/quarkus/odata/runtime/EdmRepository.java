package net.moewes.quarkus.odata.runtime;

import net.moewes.quarkus.odata.repository.*;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.*;
import org.apache.olingo.commons.api.ex.ODataRuntimeException;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.CDI;
import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class EdmRepository {

    public static final String NAMESPACE = "Quarkus.OData"; // FIXME

    private final Map<String, EntityType> entities = new HashMap<>();
    private final Map<String, EntitySet> entitySets = new HashMap<>();
    private final Map<String, Action> actions = new HashMap<>();
    private final Map<String, Function> functions = new HashMap<>();

    public void registerEntity(final String name, final EntityType entityType) {

        entities.put(name, entityType);
    }

    public void registerEntitySet(final String name, final EntitySet entitySet) {

        entitySets.put(name, entitySet);
    }

    public void registerAction(String name, final String entitySetName, final Action action) {

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
        result.addAll(actions.values().stream().map(Action::getName).collect(Collectors.toList()));
        return result;
    }

    public List<String> getFunctionNames() {
        List<String> result = new ArrayList<>();
        result.addAll(functions.values().stream().map(Function::getName).collect(Collectors.toList()));
        return result;
    }

    public Optional<EntitySet> findEntitySet(String entitySetName) {
        return Optional.ofNullable(entitySets.get(entitySetName));
    }

    public Optional<EntityType> findEntityType(String entityTypeName) {
        return Optional.ofNullable(entities.get(entityTypeName));
    }

    public Optional<Action> findAction(String actionName) {
        return Optional.ofNullable(actions.get(actionName));
    }

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

    public Optional<CsdlEntitySet> findCsdlForEntitySet(String entitySetName) {

        EntitySet entitySet = findEntitySet(entitySetName).orElse(null);
        return Optional.ofNullable(entitySet != null ? getCsdlForEntitySet(entitySet) : null);
    }

    public CsdlEntitySet getCsdlForEntitySet(EntitySet entitySet) {

        return new CsdlEntitySet()
                .setName(entitySet.getName())
                .setType(new FullQualifiedName(NAMESPACE, entitySet.getEntityType()));
    }

    public Optional<CsdlEntityType> findCsdlForEntityType(FullQualifiedName entityTypeName) {

        CsdlEntityType csdlEntityType = null;

        if (entities.containsKey(entityTypeName.getName())) {
            EntityType entityType = entities.get(entityTypeName.getName());
            List<CsdlProperty> csdlPropertyList = new ArrayList<>();
            entityType.getPropertyMap().values().forEach(property -> {
                CsdlProperty csdlProperty = new CsdlProperty()
                        .setName(property.getName())
                        .setType(property.getEdmType().getFullQualifiedName());
                csdlPropertyList.add(csdlProperty);
            });
            List<CsdlPropertyRef> keys = entityType.getPropertyMap().values().stream()
                    .filter(EntityProperty::isKey)
                    .map(item1 -> new CsdlPropertyRef().setName(item1.getName()))
                    .collect(Collectors.toList());

            csdlEntityType = new CsdlEntityType()
                    .setName(entityType.getName())
                    .setProperties(csdlPropertyList)
                    .setKey(keys);
        }
        return Optional.ofNullable(csdlEntityType);
    }

    public Optional<CsdlAction> findCsdlForAction(FullQualifiedName actionName) {

        CsdlAction csdlAction = null;

        if (actions.containsKey(actionName.getName())) {
            Action action = actions.get(actionName.getName());

            csdlAction = new CsdlAction()
                    .setName(action.getName());

            List<CsdlParameter> parameters = new ArrayList<>();
            CsdlAction finalCsdlAction = csdlAction;
            action.getParameter().forEach(parameter -> {
                CsdlParameter csdlParameter = new CsdlParameter();

                if (parameter.isBindingParameter()) {
                    csdlParameter.setName(parameter.getEntityType());
                    csdlParameter.setType(new FullQualifiedName(NAMESPACE, parameter.getEntityType()));
                    finalCsdlAction.setBound(true);
                } else {
                    csdlParameter.setName(parameter.getName());
                    csdlParameter.setType(parameter.getEdmType().getFullQualifiedName());
                }

                csdlParameter.setCollection(false);
                parameters.add(csdlParameter);
            });

            csdlAction.setParameters(parameters);

            csdlAction.setReturnType(new CsdlReturnType().setType(action.getReturnType().getEdmType().getFullQualifiedName()).setCollection(false));
        }
        return Optional.ofNullable(csdlAction);
    }

    public Optional<CsdlFunction> findCsdlForFunction(FullQualifiedName functionName) {

        CsdlFunction csdlFunction = null;

        if (functions.containsKey(functionName.getName())) {
            Function function = functions.get(functionName.getName());

            List<CsdlParameter> parameters = new ArrayList<>();
            // First Parameter bound Entity
            String entitySet = function.getEntitySet();
            EntitySet entitySet1 = findEntitySet(entitySet).orElseThrow(() -> new ODataRuntimeException("FIXME"));
            CsdlParameter es_param = new CsdlParameter();
            es_param.setName(entitySet1.getEntityType());
            es_param.setType(new FullQualifiedName(NAMESPACE, entitySet1.getEntityType()));
            es_param.setCollection(false);
            parameters.add(es_param);

            csdlFunction = new CsdlFunction()
                    .setName(function.getName())
                    .setParameters(parameters)
                    .setBound(true)
                    .setReturnType(new CsdlReturnType().setType(EdmPrimitiveTypeKind.String.getFullQualifiedName()).setCollection(false));
        }
        return Optional.ofNullable(csdlFunction);
    }
}

