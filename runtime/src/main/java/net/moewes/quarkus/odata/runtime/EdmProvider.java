package net.moewes.quarkus.odata.runtime;

import java.util.ArrayList;
import java.util.List;

import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlAbstractEdmProvider;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityContainer;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityContainerInfo;
import org.apache.olingo.commons.api.edm.provider.CsdlEntitySet;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityType;
import org.apache.olingo.commons.api.edm.provider.CsdlSchema;

public class EdmProvider extends CsdlAbstractEdmProvider {

    public static final String NAMESPACE = "Quarkus.OData";

    public static final String CONTAINER_NAME = "Container";
    public static final FullQualifiedName CONTAINER = new FullQualifiedName(NAMESPACE, CONTAINER_NAME);

    private final EdmRepository repository;

    public EdmProvider(EdmRepository repository) {
        super();
        this.repository = repository;
    }

    @Override
    public CsdlEntityType getEntityType(FullQualifiedName entityTypeName) {

        return repository.findEntityType(entityTypeName).orElse(null);
    }

    @Override
    public CsdlEntitySet getEntitySet(FullQualifiedName entityContainer, String entitySetName) {

        return repository.findEntitySet(entitySetName).orElse(null);
    }

    @Override
    public CsdlEntityContainerInfo getEntityContainerInfo(FullQualifiedName entityContainerName) {

        if (entityContainerName == null || entityContainerName.equals(CONTAINER)) {
            CsdlEntityContainerInfo entityContainerInfo = new CsdlEntityContainerInfo();
            entityContainerInfo.setContainerName(CONTAINER);
            return entityContainerInfo;
        }
        return null;
    }

    @Override
    public List<CsdlSchema> getSchemas() {

        CsdlSchema schema = new CsdlSchema();
        schema.setNamespace(NAMESPACE);

        List<CsdlEntityType> entityTypes = new ArrayList<>();
        for (String item : repository.getEntityTypes()) {
            entityTypes.add(getEntityType(new FullQualifiedName(NAMESPACE, item)));
        }
        schema.setEntityTypes(entityTypes);

        schema.setEntityContainer(getEntityContainer());

        List<CsdlSchema> schemas = new ArrayList<>();
        schemas.add(schema);
        return schemas;
    }

    @Override
    public CsdlEntityContainer getEntityContainer() {

        List<CsdlEntitySet> entitySets = new ArrayList<>();

        for (String item : repository.getEntitySets()) {
            entitySets.add(getEntitySet(CONTAINER, item));
        }

        CsdlEntityContainer entityContainer = new CsdlEntityContainer();
        entityContainer.setName(CONTAINER_NAME);
        entityContainer.setEntitySets(entitySets);

        return entityContainer;
    }
}
