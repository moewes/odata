package net.moewes.quarkus.odata.runtime;

import net.moewes.quarkus.odata.EntityCollectionProvider;
import net.moewes.quarkus.odata.EntityProvider;
import net.moewes.quarkus.odata.repository.EntitySet;
import net.moewes.quarkus.odata.runtime.edm.EdmRepository;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.*;
import org.apache.olingo.server.api.deserializer.DeserializerException;
import org.apache.olingo.server.api.serializer.SerializerException;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriParameter;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class QuarkusEntityProcessor
        implements org.apache.olingo.server.api.processor.EntityProcessor {

    private final EdmRepository repository;
    private final ODataEntityConverter odataEntityConverter;
    private OData odata;
    private ServiceMetadata serviceMetadata;

    public QuarkusEntityProcessor(EdmRepository repository) {
        this.repository = repository;
        odataEntityConverter = new ODataEntityConverter(repository);
    }

    @Override
    public void init(OData odata, ServiceMetadata serviceMetadata) {
        this.odata = odata;
        this.serviceMetadata = serviceMetadata;
    }

    @Override
    public void readEntity(ODataRequest oDataRequest, ODataResponse oDataResponse,
                           UriInfo uriInfo, ContentType contentType)
            throws ODataApplicationException, ODataLibraryException {

        ODataRequestContext context = new ODataRequestContext(odata, oDataRequest,
                oDataResponse, uriInfo);
        EdmEntitySet entitySet;
        List<UriParameter> keyPredicates;

        Entity entity;
        if (context.isNavigation()) {
            entitySet = context.getParentContext().getEntitySet();
            keyPredicates = context.getParentContext().getKeyPredicates();
            entity = readNavigationData(entitySet, keyPredicates);
        } else {
            entitySet = context.getEntitySet();
            keyPredicates = context.getKeyPredicates();
            entity = readData(entitySet, keyPredicates);
        }

        context.respondWithEntity(entity, contentType, HttpStatusCode.OK, serviceMetadata);
    }

    private Entity readNavigationData(EdmEntitySet entitySet, List<UriParameter> keyPredicates) {
        return new Entity();
    }


    @Override
    public void createEntity(ODataRequest oDataRequest, ODataResponse oDataResponse,
                             UriInfo uriInfo,
                             ContentType requestFormat, ContentType responseFormat)
            throws ODataApplicationException, ODataLibraryException {

        ODataRequestContext context = new ODataRequestContext(odata, oDataRequest,
                oDataResponse, uriInfo);

        EdmEntitySet edmEntitySet = context.getEntitySet();
        EntitySet entitySet =
                repository.findEntitySet(edmEntitySet.getName())
                        .orElseThrow(() -> new ODataApplicationException("no entityset",
                                HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH));
        try {
            Entity entity = createData(entitySet, context.getEntityFromRequest(requestFormat));
            context.respondWithEntity(entity, responseFormat, HttpStatusCode.CREATED,
                    serviceMetadata);
        } catch (DeserializerException e) {
            throw new ODataApplicationException("Cannot deserialize request data",
                    HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
        } catch (SerializerException e) {
            throw new ODataApplicationException("Cannot serialize request data",
                    HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), Locale.ENGLISH);
        }
    }

    @Override
    public void updateEntity(ODataRequest oDataRequest, ODataResponse oDataResponse,
                             UriInfo uriInfo, ContentType contentType, ContentType contentType1)
            throws ODataApplicationException, ODataLibraryException {

        ODataRequestContext context = new ODataRequestContext(odata, oDataRequest,
                oDataResponse, uriInfo);

        EntitySet entitySet = repository.findEntitySet(context.getEntitySet().getName())
                .orElseThrow(() -> new ODataApplicationException("",
                        HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH));

        Object serviceBean = repository.getServiceBean(entitySet);
        if (serviceBean instanceof EntityProvider<?>) {
            Map<String, String> keys = new HashMap<>();
            odataEntityConverter.convertKeysToAppFormat(context.getKeyPredicates(), entitySet,
                    keys);
            Object old_data =
                    ((EntityCollectionProvider<?>) serviceBean).find(keys)
                            .orElseThrow(() -> new ODataApplicationException("Not found",
                                    HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH));

            Object data = null;
            if (context.isPatch()) {
                data = odataEntityConverter
                        .patchFrameworkEntityToAppData(context.getEntityFromRequest(contentType),
                                entitySet,
                                old_data);
            } else {
                data = odataEntityConverter
                        .convertFrameworkEntityToAppData(context.getEntityFromRequest(contentType),
                                entitySet);
            }
            ((EntityProvider<?>) serviceBean).update(keys, data);
        }
        context.respondWithNoContent();
    }

    @Override
    public void deleteEntity(ODataRequest oDataRequest, ODataResponse oDataResponse,
                             UriInfo uriInfo) throws ODataApplicationException,
            ODataLibraryException {

        ODataRequestContext context = new ODataRequestContext(odata, oDataRequest,
                oDataResponse, uriInfo);
        deleteData(context.getEntitySet(), context.getKeyPredicates());
        context.respondWithNoContent();
    }

    private Entity readData(EdmEntitySet edmEntitySet, List<UriParameter> keyPredicates) {

        Entity entity = new Entity();
        repository.findEntitySet(edmEntitySet.getName()).ifPresent(entitySet -> {

            Object serviceBean = repository.getServiceBean(entitySet);
            if (serviceBean instanceof EntityCollectionProvider<?>) {

                Map<String, String> keys = new HashMap<>();
                odataEntityConverter.convertKeysToAppFormat(keyPredicates, entitySet, keys);
                ((EntityCollectionProvider<?>) serviceBean).find(keys).ifPresent(data -> {
                    odataEntityConverter.convertDataToFrameworkEntity(entity,
                            repository.findEntityType(entitySet.getEntityType()).orElseThrow(),
                            data);
                });
            }
        });
        return entity;
    }

    private Entity createData(EntitySet entitySet, Entity entity) throws ODataApplicationException {
        Entity createdEntity = new Entity();

        Object serviceBean = repository.getServiceBean(entitySet);
        if (serviceBean instanceof EntityProvider<?>) {

            Object data = odataEntityConverter.convertFrameworkEntityToAppData(entity, entitySet);
            Object createdData = ((EntityProvider<?>) serviceBean).create(data);
            odataEntityConverter.convertDataToFrameworkEntity(createdEntity,
                    repository.findEntityType(entitySet.getEntityType()).orElseThrow(),
                    createdData);
        }
        return createdEntity;
    }

    private void deleteData(EdmEntitySet edmEntitySet, List<UriParameter> keyPredicates) {

        repository.findEntitySet(edmEntitySet.getName()).ifPresent(entitySet -> {

            Object serviceBean = repository.getServiceBean(entitySet);
            if (serviceBean instanceof EntityProvider<?>) {

                Map<String, String> keys = new HashMap<>();
                odataEntityConverter.convertKeysToAppFormat(keyPredicates, entitySet, keys);
                ((EntityProvider<?>) serviceBean).delete(keys);
            }
        });
    }
}
