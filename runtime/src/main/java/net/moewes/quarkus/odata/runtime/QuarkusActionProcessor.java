package net.moewes.quarkus.odata.runtime;

import net.moewes.quarkus.odata.repository.Callable;
import net.moewes.quarkus.odata.runtime.edm.EdmRepository;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType;
import org.apache.olingo.commons.api.edm.EdmPrimitiveType;
import org.apache.olingo.commons.api.edm.EdmType;
import org.apache.olingo.commons.api.ex.ODataRuntimeException;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.*;
import org.apache.olingo.server.api.deserializer.DeserializerException;
import org.apache.olingo.server.api.processor.ActionEntityCollectionProcessor;
import org.apache.olingo.server.api.processor.ActionEntityProcessor;
import org.apache.olingo.server.api.processor.ActionPrimitiveCollectionProcessor;
import org.apache.olingo.server.api.processor.ActionPrimitiveProcessor;
import org.apache.olingo.server.api.uri.UriInfo;

import java.util.Collection;
import java.util.Locale;

public class QuarkusActionProcessor implements ActionPrimitiveProcessor,
        ActionPrimitiveCollectionProcessor, ActionEntityProcessor, ActionEntityCollectionProcessor {

    private final EdmRepository repository;
    private final ODataEntityConverter odataEntityConverter;
    private OData odata;
    private ServiceMetadata serviceMetadata;

    public QuarkusActionProcessor(EdmRepository repository) {
        this.repository = repository;
        odataEntityConverter = new ODataEntityConverter(repository);
    }

    @Override
    public void init(OData oData, ServiceMetadata serviceMetadata) {
        this.odata = oData;
        this.serviceMetadata = serviceMetadata;
    }

    @Override
    public void processActionPrimitive(ODataRequest oDataRequest,
                                       ODataResponse oDataResponse,
                                       UriInfo uriInfo,
                                       ContentType importContentType,
                                       ContentType exportContentType)
            throws ODataApplicationException, ODataLibraryException {

        ActionRequestContext context = new ActionRequestContext(odata, oDataRequest, oDataResponse,
                uriInfo);

        Object result = processAction(importContentType, context);

        Property property =
                new Property(null, "result", ValueType.PRIMITIVE, result);

        context.respondWithPrimitive(property,
                (EdmPrimitiveType) context.getEdmReturnType().getType(),
                exportContentType,
                HttpStatusCode.OK,
                serviceMetadata);
    }

    @Override
    public void processActionEntityCollection(ODataRequest oDataRequest,
                                              ODataResponse oDataResponse,
                                              UriInfo uriInfo,
                                              ContentType importContentType,
                                              ContentType exportContentType)
            throws ODataApplicationException, ODataLibraryException {

        ActionRequestContext context = new ActionRequestContext(odata, oDataRequest, oDataResponse,
                uriInfo);

        Object result = processAction(importContentType, context);

        EntityCollection collection = new EntityCollection();

        EdmType edmType = context.getEdmReturnType().getType();
        if (result instanceof Collection) {
            ((Collection<?>) result).forEach(data -> {
                Entity entity = new Entity();
                odataEntityConverter.convertDataToFrameworkEntity(entity,
                        repository.findEntityType(edmType.getName())
                                .orElseThrow(),
                        data);
                collection.getEntities().add(entity);
            });
        }

        context.respondWithEntityCollection(collection,
                exportContentType,
                HttpStatusCode.OK,
                serviceMetadata);
    }

    @Override
    public void processActionEntity(ODataRequest oDataRequest,
                                    ODataResponse oDataResponse,
                                    UriInfo uriInfo,
                                    ContentType importContentType,
                                    ContentType exportContentType)
            throws ODataApplicationException, ODataLibraryException {

        ActionRequestContext context = new ActionRequestContext(odata, oDataRequest, oDataResponse,
                uriInfo);

        Object result = processAction(importContentType, context);

        EdmType edmType = context.getEdmReturnType().getType();
        Entity entity = new Entity();
        odataEntityConverter.convertDataToFrameworkEntity(entity,
                repository.findEntityType(edmType.getName()).orElseThrow(),
                result);

        context.respondWithEntity(entity,
                exportContentType,
                HttpStatusCode.OK,
                serviceMetadata);
    }

    @Override
    public void processActionPrimitiveCollection(ODataRequest oDataRequest,
                                                 ODataResponse oDataResponse,
                                                 UriInfo uriInfo,
                                                 ContentType importContentType,
                                                 ContentType exportContentType)
            throws ODataApplicationException, ODataLibraryException {

        ActionRequestContext context = new ActionRequestContext(odata, oDataRequest, oDataResponse,
                uriInfo);

        Object result = processAction(importContentType, context);

        Property property =
                new Property(null, "result", ValueType.COLLECTION_PRIMITIVE, result);

        context.respondWithPrimitiveCollection(property,
                (EdmPrimitiveType) context.getEdmReturnType().getType(),
                exportContentType,
                HttpStatusCode.OK,
                serviceMetadata);
    }

    private Object processAction(ContentType importContentType, ActionRequestContext context)
            throws ODataApplicationException, DeserializerException {
        Callable action = repository.findAction(context.getActionName())
                .orElseThrow(() -> new ODataApplicationException(
                        "Can't find action " + context.getActionName(),
                        HttpStatusCode.BAD_REQUEST.getStatusCode(),
                        Locale.ENGLISH));

        ServiceBean serviceBean;
        Object boundEntityData;

        // is action bound
        if (action.getEntitySet() != null) {
            serviceBean =
                    new ServiceBean(repository.findEntitySet(action.getEntitySet()).orElseThrow());
            boundEntityData = serviceBean.getBoundEntityData(context,
                    new ODataEntityConverter(repository));
        } else {
            throw new ODataRuntimeException("not supported");
        }

        return serviceBean.callAction(context, action,
                boundEntityData,
                context.getActionParameter(importContentType));
    }
}
