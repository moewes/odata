package net.moewes.quarkus.odata.runtime;

import net.moewes.quarkus.odata.EntityProvider;
import net.moewes.quarkus.odata.repository.Action;
import net.moewes.quarkus.odata.repository.DataTypeKind;
import net.moewes.quarkus.odata.repository.DataTypes;
import net.moewes.quarkus.odata.repository.EntitySet;
import net.moewes.quarkus.odata.runtime.edm.EdmRepository;
import org.apache.olingo.commons.api.data.*;
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
import org.apache.olingo.server.api.uri.UriParameter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

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
        Action action = repository.findAction(context.getActionName())
                .orElseThrow(() -> new ODataApplicationException(
                        "Can't find action " + context.getActionName(),
                        HttpStatusCode.BAD_REQUEST.getStatusCode(),
                        Locale.ENGLISH));

        Object serviceBean;
        Object boundEntityData;

        // is action bound
        if (action.getEntitySet() != null) {
            serviceBean = getServiceBeanForEntitySet(context);
            boundEntityData = getBoundEntityData(serviceBean, context);
        } else {
            throw new ODataRuntimeException("not supported");
        }

        return callAction(context, action, serviceBean, boundEntityData,
                context.getActionParameter(importContentType));
    }

    private Object getServiceBeanForEntitySet(ActionRequestContext context) {

        EntitySet entitySet = getEntitySet(context);

        Object serviceBean = repository.getServiceBean(entitySet);
        if (serviceBean instanceof EntityProvider<?>) {
            return serviceBean;
        } else {
            throw new ODataRuntimeException("service class for bound action must implement " +
                    "EntityProvider<?>");
        }
    }

    private Object getBoundEntityData(Object serviceBean, ActionRequestContext context)
            throws ODataApplicationException {

        List<UriParameter> keyPredicates = context.getKeyPredicates();
        Map<String, String> keys = new HashMap<>();
        odataEntityConverter.convertKeysToAppFormat(keyPredicates, getEntitySet(context), keys);
        return ((EntityProvider<?>) serviceBean).find(keys)
                .orElseThrow(() -> new ODataApplicationException("could  not find bound entity " +
                        "data", 404, Locale.ENGLISH));
    }

    private EntitySet getEntitySet(ActionRequestContext context) {
        return repository.findEntitySet(context.getEntitySet().getName())
                .orElseThrow(() -> new ODataRuntimeException("could not happen"));
    }

    private Object callAction(ActionRequestContext context,
                              Action action,
                              Object serviceBean,
                              Object boundEntityData,
                              Map<String, Parameter> actionParameters) {

        try {
            List<Class<?>> parameterClasses = new ArrayList<>();
            action.getParameter().forEach(parameter -> {
                if (parameter.getTypeKind().equals(DataTypeKind.PRIMITIVE)) {
                    parameterClasses.add(DataTypes.getClassForEdmType(parameter.getEdmType()));
                } else {
                    try {
                        Class<?> aClass =
                                Class.forName(parameter.getTypeName(), true,
                                        Thread.currentThread()
                                                .getContextClassLoader());
                        parameterClasses.add(aClass);
                    } catch (ClassNotFoundException e) {
                        throw new ODataRuntimeException(e);
                    }
                }
            });
            Method declaredMethod =
                    serviceBean.getClass().getDeclaredMethod(context.getActionName(),
                            parameterClasses.toArray(Class[]::new));

            List<Object> valueList = new ArrayList<>();
            if (boundEntityData != null) {
                valueList.add(boundEntityData);
            }
            action.getParameter().forEach(parameter -> {
                if (!parameter.isBindingParameter()) {
                    Object value = actionParameters.get(parameter.getName())
                            .asPrimitive();
                    valueList.add(value);
                }
            });

            return declaredMethod.invoke(serviceBean, valueList.toArray());
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new ODataRuntimeException(e);
        }
    }
}
