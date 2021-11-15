package net.moewes.quarkus.odata.runtime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.CDI;

import net.moewes.quarkus.odata.repository.EntitySet;
import net.moewes.quarkus.odata.repository.EntityType;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlEntitySet;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityType;
import org.apache.olingo.commons.api.edm.provider.CsdlProperty;
import org.apache.olingo.commons.api.edm.provider.CsdlPropertyRef;
import org.apache.olingo.commons.api.ex.ODataRuntimeException;

@ApplicationScoped
public class EdmRepository {

    public static final String NAMESPACE = "Quarkus.OData"; // FIXME

    private final Map<String, EntityType> entities = new HashMap<>();
    private final Map<String, EntitySet> entitySets = new HashMap<>();

    public void registerEntity(final String name, final EntityType entityType) {

        entities.put(name, entityType);
    }

    public void registerEntitySet(final String name, final EntitySet entitySet) {

        entitySets.put(name, entitySet);
    }

    public Optional<CsdlEntityType> findEntityType(FullQualifiedName entityTypeName) {

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
                    .filter(item -> item.isKey())
                    .map(item1 -> new CsdlPropertyRef().setName(item1.getName()))
                    .collect(Collectors.toList());

            csdlEntityType = new CsdlEntityType()
                    .setName(entityType.getName())
                    .setProperties(csdlPropertyList)
                    .setKey(keys);
        }
        return Optional.ofNullable(csdlEntityType);
    }

    public List<String> getEntityTypes() {

        return entities.values().stream().map(item -> item.getName()).collect(Collectors.toList());
    }

    public List<String> getEntitySets() {

        return entitySets.values().stream().map(item -> item.getName()).collect(Collectors.toList());
    }

    public Optional<CsdlEntitySet> findEntitySet(String entitySetName) {
        // TODO Refactor -> findEntitySetDefinition
        CsdlEntitySet csdlEntitySet = null;

        if (entitySets.containsKey(entitySetName)) {
            EntitySet entitySet = entitySets.get(entitySetName);

            csdlEntitySet = new CsdlEntitySet()
                    .setName(entitySet.getName())
                    .setType(new FullQualifiedName(NAMESPACE, entitySet.getEntityType()));
        }
        return Optional.ofNullable(csdlEntitySet);
    }

    public Optional<EntitySet> findEntitySetDefinition(String entitySetName) {
        return Optional.ofNullable(entitySets.get(entitySetName));
    }

    public Optional<EntityType> findEntityTypeDefinition(String entityTypeName) {
        return Optional.ofNullable(entities.get(entityTypeName));
    }

    public Object getServiceBean(EntitySet entitySet) {

        try {
            Class<?> beanClass = Class.forName(entitySet.getBeanClassName(), true,
                    Thread.currentThread().getContextClassLoader());
            Object serviceBean = CDI.current().select(beanClass, Default.Literal.INSTANCE).get();
            return serviceBean;
        } catch (ClassNotFoundException e) {
            throw new ODataRuntimeException("Service class " + entitySet.getBeanClassName() + " " +
                    "not found");
        }
    }
}
