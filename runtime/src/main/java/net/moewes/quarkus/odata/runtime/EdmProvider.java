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
import org.apache.olingo.commons.api.ex.ODataException;

public class EdmProvider extends CsdlAbstractEdmProvider {

    public static final String NAMESPACE = "OData.Demo";

    public static final String CONTAINER_NAME = "Container";
    public static final FullQualifiedName CONTAINER = new FullQualifiedName(NAMESPACE, CONTAINER_NAME);

    public static final String ET_TODO_NAME = "Todo";
    public static final FullQualifiedName ET_TODO = new FullQualifiedName(NAMESPACE, ET_TODO_NAME);
    public static final String ES_TODOS_NAME = "Todos";

    private EdmRepository repository;

    public EdmProvider(EdmRepository repository) {
        super();
        this.repository = repository;
    }

    @Override
    public CsdlEntityType getEntityType(FullQualifiedName entityTypeName) throws ODataException {

        return repository.findEntityType(entityTypeName).orElse(null);
    }

    @Override
    public CsdlEntitySet getEntitySet(FullQualifiedName entityContainer, String entitySetName) throws ODataException {

        CsdlEntitySet entitySet =
                repository.findEntitySet(entitySetName).orElse(null);
        /*
        if (entityContainer.equals(CONTAINER)) {
            if (entitySetName.equals(ES_TODOS_NAME)) {
                entitySet = new CsdlEntitySet()
                        .setName(ES_TODOS_NAME)
                        .setType(ET_TODO);

                return entitySet;
            }
        } */
        return entitySet;
    }

    @Override
    public CsdlEntityContainerInfo getEntityContainerInfo(FullQualifiedName entityContainerName) throws ODataException {

        if (entityContainerName == null || entityContainerName.equals(CONTAINER)) {
            CsdlEntityContainerInfo entityContainerInfo = new CsdlEntityContainerInfo();
            entityContainerInfo.setContainerName(CONTAINER);
            return entityContainerInfo;
        }
        return null;
    }

    @Override
    public List<CsdlSchema> getSchemas() throws ODataException {

        CsdlSchema schema = new CsdlSchema();
        schema.setNamespace(NAMESPACE);

        List<CsdlEntityType> entityTypes = new ArrayList<>();
        // entityTypes.add(getEntityType(ET_TODO));
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
    public CsdlEntityContainer getEntityContainer() throws ODataException {

        List<CsdlEntitySet> entitySets = new ArrayList<>();
        //  entitySets.add(getEntitySet(CONTAINER, ES_TODOS_NAME));

        for (String item : repository.getEntitySets()) {
            entitySets.add(getEntitySet(CONTAINER, item));
        }

        CsdlEntityContainer entityContainer = new CsdlEntityContainer();
        entityContainer.setName(CONTAINER_NAME);
        entityContainer.setEntitySets(entitySets);

        return entityContainer;
    }
}
