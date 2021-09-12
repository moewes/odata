package net.moewes.quarkus.odata.runtime;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.CDI;

import net.moewes.quarkus.odata.EntityCollectionProvider;
import net.moewes.quarkus.odata.repository.EntityProperty;
import net.moewes.quarkus.odata.repository.EntityType;
import org.apache.olingo.commons.api.data.ContextURL;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.ex.ODataRuntimeException;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpHeader;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.ODataLibraryException;
import org.apache.olingo.server.api.ODataRequest;
import org.apache.olingo.server.api.ODataResponse;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.processor.EntityCollectionProcessor;
import org.apache.olingo.server.api.serializer.EntityCollectionSerializerOptions;
import org.apache.olingo.server.api.serializer.ODataSerializer;
import org.apache.olingo.server.api.serializer.SerializerResult;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;

public class QuarkusEntityCollectionProcessor implements EntityCollectionProcessor {

    private OData odata;
    private ServiceMetadata serviceMetadata;
    private EdmRepository repository;

    public QuarkusEntityCollectionProcessor(EdmRepository repository) {
        this.repository = repository;
    }

    @Override
    public void readEntityCollection(ODataRequest oDataRequest, ODataResponse oDataResponse, UriInfo uriInfo, ContentType contentType)
            throws ODataApplicationException, ODataLibraryException {

        List<UriResource> resourcePaths = uriInfo.getUriResourceParts();
        UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) resourcePaths.get(0);
        EdmEntitySet edmEntitySet = uriResourceEntitySet.getEntitySet();

        EntityCollection entitySet = getData(edmEntitySet);

        ODataSerializer serializer = odata.createSerializer(contentType);

        EdmEntityType edmEntityType = edmEntitySet.getEntityType();
        ContextURL contextURL = ContextURL.with().entitySet(edmEntitySet).build();

        final String id = oDataRequest.getRawBaseUri() + "/" + edmEntitySet.getName();

        EntityCollectionSerializerOptions options = EntityCollectionSerializerOptions.with().id(id).contextURL(contextURL).build();
        SerializerResult serializerResult = serializer.entityCollection(serviceMetadata, edmEntityType, entitySet, options);

        InputStream serializedContent = serializerResult.getContent();

        oDataResponse.setContent(serializedContent);
        oDataResponse.setStatusCode(HttpStatusCode.OK.getStatusCode());
        oDataResponse.setHeader(HttpHeader.CONTENT_TYPE, contentType.toContentTypeString());
    }

    private EntityCollection getData(EdmEntitySet edmEntitySet) {

        EntityCollection collection = new EntityCollection();

        repository.findEntitySetDefinition(edmEntitySet.getName()).ifPresent(item -> {
            try {
                Class<?> beanClass = Class.forName(item.getBeanClassName(), true, Thread.currentThread().getContextClassLoader());
                Object serviceBean = CDI.current().select(beanClass, Default.Literal.INSTANCE).get();
                if (serviceBean instanceof EntityCollectionProvider<?>) {
                    Object collection1 = ((EntityCollectionProvider<?>) serviceBean).getCollection();

                    EntityType entityType = repository.findEntityTypeDefinition(item.getEntityType())
                            .orElseThrow(() -> new ODataRuntimeException("EntityType " + item.getEntityType() + " not found"));

                    if (collection1 instanceof Collection) {
                        ((Collection<?>) collection1).forEach(item2 -> {
                            Entity entity = new Entity();

                            for (EntityProperty entityProperty : entityType.getPropertyMap().values()) {
                                try {
                                    Method getter = item2.getClass().getDeclaredMethod("get" + entityProperty.getName());
                                    Object result = getter.invoke(item2);
                                    entity.addProperty(new Property(null, entityProperty.getName(), ValueType.PRIMITIVE, result));
                                } catch (NoSuchMethodException e) {
                                    throw new ODataRuntimeException("Getter for " + entityProperty.getName() + " not found");
                                } catch (InvocationTargetException e) {
                                    throw new ODataRuntimeException("Getter for " + entityProperty.getName() + " InvocationTargetException ");
                                } catch (IllegalAccessException e) {
                                    throw new ODataRuntimeException("Getter for " + entityProperty.getName() + " IllegalAccessException ");
                                }
                            }
                            collection.getEntities().add(entity);
                        });
                    }
                }

            } catch (ClassNotFoundException e) {
                throw new ODataRuntimeException("Service class " + item.getBeanClassName() + " not found");
            }
        });
        /*
        if (EdmProvider.ES_TODOS_NAME.equals(edmEntitySet.getName())) {
            List<Entity> entities = collection.getEntities();
            Entity todo = new Entity();
            UUID id = UUID.randomUUID();
            todo.addProperty(new Property(null, "ID", ValueType.PRIMITIVE, id));
            todo.addProperty(new Property(null, "Description", ValueType.PRIMITIVE, "Test Todeo"));
            todo.setId(createId("Todos", id));

            entities.add(todo);
        }*/
        return collection;
    }

    private URI createId(String entitySetName, UUID id) {
        try {
            return new URI(entitySetName + "(" + id.toString() + ")");
        } catch (URISyntaxException e) {
            throw new ODataRuntimeException("xxx");
        }
    }

    @Override
    public void init(OData oData, ServiceMetadata serviceMetadata) {
        this.odata = oData;
        this.serviceMetadata = serviceMetadata;
    }
}
