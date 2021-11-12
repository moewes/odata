package net.moewes.quarkus.odata.runtime;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.CDI;

import net.moewes.quarkus.odata.EntityProvider;
import net.moewes.quarkus.odata.repository.EntityProperty;
import net.moewes.quarkus.odata.repository.EntityType;
import org.apache.olingo.commons.api.data.ContextURL;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
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
import org.apache.olingo.server.api.deserializer.DeserializerResult;
import org.apache.olingo.server.api.deserializer.ODataDeserializer;
import org.apache.olingo.server.api.serializer.EntitySerializerOptions;
import org.apache.olingo.server.api.serializer.ODataSerializer;
import org.apache.olingo.server.api.serializer.SerializerResult;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriParameter;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;

public class QuarkusEntityProcessor implements org.apache.olingo.server.api.processor.EntityProcessor {

    private final EdmRepository repository;
    private OData odata;
    private ServiceMetadata serviceMetadata;

    public QuarkusEntityProcessor(EdmRepository repository) {
        this.repository = repository;
    }

    @Override
    public void init(OData odata, ServiceMetadata serviceMetadata) {
        this.odata = odata;
        this.serviceMetadata = serviceMetadata;
    }

    @Override
    public void readEntity(ODataRequest oDataRequest, ODataResponse oDataResponse, UriInfo uriInfo, ContentType contentType) throws ODataApplicationException, ODataLibraryException {

        List<UriResource> resourcePaths = uriInfo.getUriResourceParts();
        UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) resourcePaths.get(0); // TODO
        EdmEntitySet entitySet = uriResourceEntitySet.getEntitySet();
        List<UriParameter> keyPredicates = uriResourceEntitySet.getKeyPredicates();
        EdmEntityType entityType = entitySet.getEntityType();

        ContextURL contextURL = ContextURL.with().entitySet(entitySet).build();
        EntitySerializerOptions options = EntitySerializerOptions.with().contextURL(contextURL).build();

        Entity entity = readData(entitySet, keyPredicates);

        ODataSerializer serializer = odata.createSerializer(contentType);
        SerializerResult result = serializer.entity(serviceMetadata, entityType, entity, options);
        InputStream stream = result.getContent();
        oDataResponse.setContent(stream);
        oDataResponse.setStatusCode(HttpStatusCode.OK.getStatusCode());
        oDataResponse.setHeader(HttpHeader.CONTENT_TYPE, contentType.toContentTypeString());
    }

    private Entity readData(EdmEntitySet edmEntitySet, List<UriParameter> keyPredicates) {

        Entity entity = new Entity();
        repository.findEntitySetDefinition(edmEntitySet.getName()).ifPresent(item -> {
            try {
                Class<?> beanClass = Class.forName(item.getBeanClassName(), true, Thread.currentThread().getContextClassLoader());
                Object serviceBean = CDI.current().select(beanClass, Default.Literal.INSTANCE).get();
                if (serviceBean instanceof EntityProvider<?, ?>) {

                    Map<String, String> keys = new HashMap<>();

                    for (UriParameter keyPredicate : keyPredicates) {
                        String value = keyPredicate.getText();
                        EntityType entityType = repository.findEntityTypeDefinition(item.getEntityType()).orElseThrow(RuntimeException::new);

                        EdmPrimitiveTypeKind edmType = entityType.getPropertyMap().get(keyPredicate.getName()).getEdmType();
                        if (edmType == EdmPrimitiveTypeKind.String) { // TODO Refactor
                            value = value.substring(1, value.length() - 1);
                        }
                        keys.put(keyPredicate.getName(), value);
                    }


                    ((EntityProvider<?, ?>) serviceBean).find(keys).ifPresent(item2 -> {

                        EntityType entityType = repository.findEntityTypeDefinition(item.getEntityType())
                                .orElseThrow(() -> new ODataRuntimeException("EntityType " + item.getEntityType() + " not found"));

                        for (EntityProperty entityProperty : entityType.getPropertyMap().values()) {
                            try {
                                Method getter = item2.getClass().getDeclaredMethod("get" + entityProperty.getName());
                                Object result2 = getter.invoke(item2);
                                entity.addProperty(new Property(null, entityProperty.getName(), ValueType.PRIMITIVE, result2));
                            } catch (NoSuchMethodException e) {
                                throw new ODataRuntimeException("Getter for " + entityProperty.getName() + " not found");
                            } catch (InvocationTargetException e) {
                                throw new ODataRuntimeException("Getter for " + entityProperty.getName() + " InvocationTargetException ");
                            } catch (IllegalAccessException e) {
                                throw new ODataRuntimeException("Getter for " + entityProperty.getName() + " IllegalAccessException ");
                            }
                        }
                    });
                }

            } catch (ClassNotFoundException e) {
                throw new ODataRuntimeException("Service class " + item.getBeanClassName() + " not found");
            }
        });
        return entity;
    }

    @Override
    public void createEntity(ODataRequest oDataRequest, ODataResponse oDataResponse, UriInfo uriInfo, ContentType requestFormat, ContentType responseFromat) throws ODataApplicationException, ODataLibraryException {

        EdmEntitySet edmEntitySet = Util.getEdmEntitySet(uriInfo);
        repository.findEntitySetDefinition(edmEntitySet.getName()).ifPresent(item -> {
            try {
                Class<?> beanClass = Class.forName(item.getBeanClassName(), true, Thread.currentThread().getContextClassLoader());
                Object serviceBean = CDI.current().select(beanClass, Default.Literal.INSTANCE).get();
                if (serviceBean instanceof EntityProvider<?, ?>) {

                    InputStream inputStream = oDataRequest.getBody();
                    ODataDeserializer deserializer = this.odata.createDeserializer(requestFormat);
                    DeserializerResult result = deserializer.entity(inputStream, edmEntitySet.getEntityType());
                    Entity entity = result.getEntity();
                    // TODO give to Service
                    //
                    ContextURL contextURL = ContextURL.with().entitySet(edmEntitySet).build();
                    EntitySerializerOptions options = EntitySerializerOptions.with().contextURL(contextURL).build();
                    ODataSerializer serializer = odata.createSerializer(responseFromat);
                    SerializerResult serializerResult = serializer.entity(serviceMetadata, edmEntitySet.getEntityType(), entity, options);

                    oDataResponse.setContent(serializerResult.getContent());
                    oDataResponse.setStatusCode(HttpStatusCode.CREATED.getStatusCode());
                    oDataResponse.setHeader(HttpHeader.CONTENT_TYPE, responseFromat.toContentTypeString());
                }
            } catch (Exception e) {

            }
        });
    }

    @Override
    public void updateEntity(ODataRequest oDataRequest, ODataResponse oDataResponse, UriInfo uriInfo, ContentType contentType, ContentType contentType1) throws ODataApplicationException, ODataLibraryException {

    }

    @Override
    public void deleteEntity(ODataRequest oDataRequest, ODataResponse oDataResponse, UriInfo uriInfo) throws ODataApplicationException, ODataLibraryException {

    }


}
