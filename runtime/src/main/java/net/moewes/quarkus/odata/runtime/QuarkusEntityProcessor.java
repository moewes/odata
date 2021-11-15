package net.moewes.quarkus.odata.runtime;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.moewes.quarkus.odata.EntityProvider;
import net.moewes.quarkus.odata.repository.EntitySet;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.ODataLibraryException;
import org.apache.olingo.server.api.ODataRequest;
import org.apache.olingo.server.api.ODataResponse;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.deserializer.DeserializerException;
import org.apache.olingo.server.api.serializer.SerializerException;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriParameter;

public class QuarkusEntityProcessor implements org.apache.olingo.server.api.processor.EntityProcessor {

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

        ODataRequestContext context = new ODataRequestContext(oDataRequest, oDataResponse, uriInfo);

        Entity entity = readData(context.getEntitySet(), context.getKeyPredicates());

        context.respondWithEntity(entity, contentType, HttpStatusCode.OK, odata, serviceMetadata);
    }

    @Override
    public void createEntity(ODataRequest oDataRequest, ODataResponse oDataResponse,
                             UriInfo uriInfo,
                             ContentType requestFormat, ContentType responseFormat)
            throws ODataApplicationException, ODataLibraryException {

        ODataRequestContext context = new ODataRequestContext(oDataRequest, oDataResponse, uriInfo);

        EdmEntitySet edmEntitySet = context.getEntitySet();
        EntitySet entitySet =
                repository.findEntitySetDefinition(edmEntitySet.getName())
                        .orElseThrow(() -> new ODataApplicationException("no entityset",
                                HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH));
        try {
            Entity entity = createData(entitySet, context.getEntityFromRequest(odata,
                    requestFormat));
            context.respondWithEntity(entity, responseFormat, HttpStatusCode.CREATED, odata,
                    serviceMetadata);
        } catch (DeserializerException e) {
            throw new ODataApplicationException("Cannot deserialize request data",
                    HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
        } catch (SerializerException e) {
            throw new ODataApplicationException("Cannot seserialize request data",
                    HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), Locale.ENGLISH);
        }
    }

    @Override
    public void updateEntity(ODataRequest oDataRequest, ODataResponse oDataResponse,
                             UriInfo uriInfo, ContentType contentType, ContentType contentType1)
            throws ODataApplicationException, ODataLibraryException {

        ODataRequestContext context = new ODataRequestContext(oDataRequest, oDataResponse, uriInfo);

        EntitySet entitySet = repository.findEntitySetDefinition(context.getEntitySet().getName())
                .orElseThrow(() -> new ODataApplicationException("",
                        HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH));

        Object serviceBean = repository.getServiceBean(entitySet);
        if (serviceBean instanceof EntityProvider<?>) {
            Map<String, String> keys = new HashMap<>();
            odataEntityConverter.convertKeysToAppFormat(context.getKeyPredicates(), entitySet,
                    keys);
            Object old_data =
                    ((EntityProvider<?>) serviceBean).find(keys)
                            .orElseThrow(() -> new ODataApplicationException("Not found",
                                    HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH));

            Object data = null;
            if (context.isPatch()) {
                data = odataEntityConverter
                        .patchFrameworkEntityToAppData(context.getEntityFromRequest(odata,
                                contentType), entitySet, old_data);
            } else {
                data = odataEntityConverter
                        .convertFrameworkEntityToAppData(context.getEntityFromRequest(odata,
                                contentType), entitySet);
            }
            ((EntityProvider<?>) serviceBean).update(keys, data);
        }
        context.respondWithNoContent();
    }

    @Override
    public void deleteEntity(ODataRequest oDataRequest, ODataResponse oDataResponse,
                             UriInfo uriInfo) throws ODataApplicationException,
            ODataLibraryException {

        ODataRequestContext context = new ODataRequestContext(oDataRequest, oDataResponse, uriInfo);
        deleteData(context.getEntitySet(), context.getKeyPredicates());
        context.respondWithNoContent();
    }

    private Entity readData(EdmEntitySet edmEntitySet, List<UriParameter> keyPredicates) {

        Entity entity = new Entity();
        repository.findEntitySetDefinition(edmEntitySet.getName()).ifPresent(entitySet -> {

            Object serviceBean = repository.getServiceBean(entitySet);
            if (serviceBean instanceof EntityProvider<?>) {

                Map<String, String> keys = new HashMap<>();
                odataEntityConverter.convertKeysToAppFormat(keyPredicates, entitySet, keys);
                ((EntityProvider<?>) serviceBean).find(keys).ifPresent(data -> {
                    odataEntityConverter.convertDataToFrameworkEntity(entity, entitySet, data);
                });
            }
        });
        return entity;
    }

    private Entity createData(EntitySet entitySet, Entity entity) {
        Entity createdEntity = new Entity();

        Object serviceBean = repository.getServiceBean(entitySet);
        if (serviceBean instanceof EntityProvider<?>) {

            Object data = odataEntityConverter.convertFrameworkEntityToAppData(entity, entitySet);
            Object createdData = ((EntityProvider<?>) serviceBean).create(data);
            odataEntityConverter.convertDataToFrameworkEntity(createdEntity, entitySet,
                    createdData);
        }
        return createdEntity;
    }

    private void deleteData(EdmEntitySet edmEntitySet, List<UriParameter> keyPredicates) {

        repository.findEntitySetDefinition(edmEntitySet.getName()).ifPresent(entitySet -> {

            Object serviceBean = repository.getServiceBean(entitySet);
            if (serviceBean instanceof EntityProvider<?>) {

                Map<String, String> keys = new HashMap<>();
                odataEntityConverter.convertKeysToAppFormat(keyPredicates, entitySet, keys);
                ((EntityProvider<?>) serviceBean).delete(keys);
            }
        });
    }
}
