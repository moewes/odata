package net.moewes.quarkus.odata.runtime;

import net.moewes.quarkus.odata.EntityCollectionProvider;
import net.moewes.quarkus.odata.EntityProvider;
import net.moewes.quarkus.odata.repository.Action;
import net.moewes.quarkus.odata.repository.EntitySet;
import net.moewes.quarkus.odata.runtime.edm.EdmRepository;
import org.apache.olingo.commons.api.data.ContextURL;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.edm.EdmBindingTarget;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.edm.EdmNavigationProperty;
import org.apache.olingo.commons.api.ex.ODataRuntimeException;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpHeader;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.*;
import org.apache.olingo.server.api.processor.EntityCollectionProcessor;
import org.apache.olingo.server.api.serializer.EntityCollectionSerializerOptions;
import org.apache.olingo.server.api.serializer.ODataSerializer;
import org.apache.olingo.server.api.serializer.SerializerResult;
import org.apache.olingo.server.api.uri.*;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class QuarkusEntityCollectionProcessor implements EntityCollectionProcessor {

    private OData odata;
    private ServiceMetadata serviceMetadata;
    private final EdmRepository repository;
    private final ODataEntityConverter odataEntityConverter;

    public QuarkusEntityCollectionProcessor(EdmRepository repository) {
        this.repository = repository;
        odataEntityConverter = new ODataEntityConverter(repository);
    }

    @Override
    public void init(OData oData, ServiceMetadata serviceMetadata) {
        this.odata = oData;
        this.serviceMetadata = serviceMetadata;
    }

    @Override
    public void readEntityCollection(ODataRequest oDataRequest,
                                     ODataResponse oDataResponse,
                                     UriInfo uriInfo,
                                     ContentType contentType)
            throws ODataApplicationException, ODataLibraryException {

        ODataRequestContext context = new ODataRequestContext(odata, oDataRequest,
                oDataResponse, uriInfo);

        UriResource lastUriPart = context.getLastUriPart();

        EntityCollection entityCollection;
        EdmEntitySet edmEntitySet;
        EdmEntityType edmEntityType;
        if (lastUriPart instanceof UriResourceEntitySet) {
            UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) lastUriPart;
            edmEntitySet = uriResourceEntitySet.getEntitySet();
            edmEntityType = edmEntitySet.getEntityType();
            entityCollection = getData(edmEntitySet);
        } else if (lastUriPart instanceof UriResourceNavigation) {
            UriResourceNavigation uriResourceNavigation = (UriResourceNavigation) lastUriPart;
            EdmNavigationProperty navigationProperty = uriResourceNavigation.getProperty();

            String navigationPropertyName = navigationProperty.getName();
            UriResourceEntitySet parentEntitySet =
                    (UriResourceEntitySet) uriInfo.getUriResourceParts().get(0); // FIXME
            EdmBindingTarget relatedBindingTarget = parentEntitySet.getEntitySet()
                    .getRelatedBindingTarget(navigationProperty.getName());
            edmEntitySet = (EdmEntitySet) relatedBindingTarget;
            edmEntityType = navigationProperty.getType();
            entityCollection = getNavigationData(parentEntitySet.getEntitySet(),
                    parentEntitySet.getKeyPredicates(),
                    navigationProperty, edmEntitySet);
        } else {
            throw new ODataApplicationException("Not supported",
                    HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(),
                    Locale.ENGLISH);
        }

        /*
        context.respondWithEntityCollection(entityCollection,
                contentType,
                HttpStatusCode.OK,
                serviceMetadata);
        */
        ODataSerializer serializer = odata.createSerializer(contentType);

        ContextURL contextURL = ContextURL.with().entitySet(edmEntitySet).build();

        final String id = oDataRequest.getRawBaseUri() + "/" + edmEntitySet.getName();

        EntityCollectionSerializerOptions options =
                EntityCollectionSerializerOptions.with().id(id).contextURL(contextURL).build();
        SerializerResult serializerResult =
                serializer.entityCollection(serviceMetadata,
                        edmEntityType,
                        entityCollection,
                        options);

        InputStream serializedContent = serializerResult.getContent();

        oDataResponse.setContent(serializedContent);
        oDataResponse.setStatusCode(HttpStatusCode.OK.getStatusCode());
        oDataResponse.setHeader(HttpHeader.CONTENT_TYPE, contentType.toContentTypeString());
    }

    private EntityCollection getData(EdmEntitySet edmEntitySet) {

        EntityCollection collection = new EntityCollection();

        repository.findEntitySet(edmEntitySet.getName()).ifPresent(entitySet -> {
            Object serviceBean = repository.getServiceBean(entitySet);
            if (serviceBean instanceof EntityCollectionProvider<?>) {
                Object dataCollection = ((EntityCollectionProvider<?>) serviceBean).getCollection();

                if (dataCollection instanceof Collection) {
                    ((Collection<?>) dataCollection).forEach(data -> {
                        Entity entity = new Entity();
                        odataEntityConverter.convertDataToFrameworkEntity(entity,
                                repository.findEntityType(entitySet.getEntityType()).orElseThrow(),
                                data);

                        collection.getEntities().add(entity);
                    });
                }
            }
        });
        return collection;
    }

    private EntityCollection getNavigationData(EdmEntitySet parentEntitySet,
                                               List<UriParameter> keyPredicates,
                                               EdmNavigationProperty navigationProperty,
                                               EdmEntitySet edmEntitySet) {

        EntityCollection collection = new EntityCollection();

        repository.findEntitySet(parentEntitySet.getName()).ifPresent(entitySet -> {
            Object serviceBean = repository.getServiceBean(entitySet);

            if (serviceBean instanceof EntityProvider<?>) {

                Map<String, String> keys = new HashMap<>();
                odataEntityConverter.convertKeysToAppFormat(keyPredicates, entitySet, keys);
                ((EntityProvider<?>) serviceBean).find(keys).ifPresent(data -> {
                    try {
                        String entityTypeName = navigationProperty.getType().getName();
                        navigationProperty.getName();

                        Action action = entitySet.getNavigationBindings()
                                .stream()
                                .filter(action1 -> action1.getReturnType()
                                        .getEntityType()
                                        .equals(entityTypeName))
                                .findFirst().orElseThrow(() -> new ODataApplicationException("Can" +
                                        "'t find Navigation",
                                        HttpStatusCode.BAD_REQUEST.getStatusCode(),
                                        Locale.ENGLISH));

                        List<Class<?>> parameterClasses = new ArrayList<>();
                        action.getParameter().forEach(parameter -> {
                            try {
                                Class<?> aClass = Class.forName(parameter.getTypeName(), true,
                                        Thread.currentThread().getContextClassLoader());
                                parameterClasses.add(aClass);
                            } catch (ClassNotFoundException e) {
                                e.printStackTrace();
                            }
                        });
                        Method declaredMethod =
                                serviceBean.getClass().getDeclaredMethod(action.getName(),
                                        parameterClasses.toArray(Class[]::new));

                        List<Object> valueList = new ArrayList<>();
                        valueList.add(data);

                        Object result = declaredMethod.invoke(serviceBean, valueList.toArray());

                        EntitySet effectiveEntitySet =
                                repository.findEntitySet(edmEntitySet.getName())
                                        .orElseThrow(() -> new ODataRuntimeException(
                                                "Can't find Entityset"));
                        if (result instanceof Collection) {
                            ((Collection<?>) result).forEach(item -> {
                                Entity entity = new Entity();
                                odataEntityConverter.convertDataToFrameworkEntity(entity,
                                        repository.findEntityType(effectiveEntitySet.getEntityType())
                                                .orElseThrow(),
                                        item);

                                collection.getEntities().add(entity);
                            });
                        }
                    } catch (IllegalAccessException | InvocationTargetException |
                             NoSuchMethodException | ODataApplicationException e) {
                        e.printStackTrace(); // FIXME
                    }
                });
            }
        });
        return collection;
    }
}
