package net.moewes.quarkus.odata.runtime;

import net.moewes.quarkus.odata.EntityProvider;
import net.moewes.quarkus.odata.repository.Action;
import net.moewes.quarkus.odata.repository.DataTypeKind;
import net.moewes.quarkus.odata.repository.DataTypes;
import net.moewes.quarkus.odata.repository.EntitySet;
import net.moewes.quarkus.odata.runtime.edm.EdmRepository;
import org.apache.olingo.commons.api.data.*;
import org.apache.olingo.commons.api.edm.*;
import org.apache.olingo.commons.api.ex.ODataRuntimeException;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.*;
import org.apache.olingo.server.api.deserializer.DeserializerException;
import org.apache.olingo.server.api.deserializer.ODataDeserializer;
import org.apache.olingo.server.api.processor.ActionEntityCollectionProcessor;
import org.apache.olingo.server.api.processor.ActionEntityProcessor;
import org.apache.olingo.server.api.processor.ActionPrimitiveCollectionProcessor;
import org.apache.olingo.server.api.processor.ActionPrimitiveProcessor;
import org.apache.olingo.server.api.serializer.SerializerException;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriParameter;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceAction;

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
                                       ContentType contentType,
                                       ContentType contentType1) throws ODataApplicationException {

        ODataRequestContext context = new ODataRequestContext(odata, oDataRequest,
                oDataResponse, uriInfo); // FIXME ActionRequestContext

        UriResource lastUriPart = context.getLastUriPart();

        if (lastUriPart instanceof UriResourceAction) {
            EdmAction edmAction = ((UriResourceAction) lastUriPart).getAction();
            String actionName = edmAction.getName();
            EdmReturnType returnType = edmAction.getReturnType();

            List<UriParameter> keyPredicates = context.getKeyPredicates();

            EdmEntitySet edmEntitySet = context.getEntitySet();

            Entity entity = new Entity();
            repository.findEntitySet(edmEntitySet.getName()).ifPresent(entitySet -> {

                Object serviceBean = repository.getServiceBean(entitySet);
                if (serviceBean instanceof EntityProvider<?>) {

                    Map<String, String> keys = new HashMap<>();
                    odataEntityConverter.convertKeysToAppFormat(keyPredicates, entitySet, keys);
                    ((EntityProvider<?>) serviceBean).find(keys).ifPresent(data -> {
                        try {
                            ODataDeserializer deserializer = odata.createDeserializer(contentType);
                            Map<String, Parameter> actionParameters =
                                    deserializer.actionParameters(oDataRequest.getBody(), edmAction)
                                            .getActionParameters();

                            Action action = repository.findAction(actionName)
                                    .orElseThrow(() -> new ODataApplicationException(
                                            "Can't find Action",
                                            HttpStatusCode.BAD_REQUEST.getStatusCode(),
                                            Locale.ENGLISH));

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
                                        e.printStackTrace();
                                    }
                                }
                            });
                            Method declaredMethod =
                                    serviceBean.getClass().getDeclaredMethod(actionName,
                                            parameterClasses.toArray(Class[]::new));

                            List<Object> valueList = new ArrayList<>();
                            valueList.add(data);
                            action.getParameter().forEach(parameter -> {
                                if (!parameter.isBindingParameter()) {
                                    Object value = actionParameters.get(parameter.getName())
                                            .asPrimitive();
                                    valueList.add(value);
                                }
                            });

                            Object result = declaredMethod.invoke(serviceBean, valueList.toArray());

                            // FIXME

                            //action.getReturnType().getEdmType()

                            Property property =
                                    new Property(null, "result", ValueType.PRIMITIVE, result);
                            context.respondWithPrimitive(property,
                                    (EdmPrimitiveType) returnType.getType(),
                                    contentType,
                                    HttpStatusCode.OK,
                                    serviceMetadata);


                        } catch (IllegalAccessException | InvocationTargetException |
                                 NoSuchMethodException | SerializerException |
                                 DeserializerException | ODataApplicationException e) {
                            e.printStackTrace(); // FIXME
                        }
                        // odataEntityConverter.convertDataToFrameworkEntity(entity, entitySet,
                        // data);
                    });
                }
            });
        } else {
            throw new ODataApplicationException("Not Supported",
                    HttpStatusCode.BAD_REQUEST.getStatusCode(),
                    Locale.ENGLISH);
        }
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

        try {
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

        } catch (SerializerException | DeserializerException e) {
            throw new ODataRuntimeException(e);
        }
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
        try {
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

        } catch (SerializerException | DeserializerException e) {
            throw new ODataRuntimeException(e);
        }
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

        try {
            Object result = processAction(importContentType, context);

            /*
            Entity entity = new Entity();
            odataEntityConverter.convertDataToFrameworkEntity(entity,
                    getEntitySet(context),
                    result);
*/
            context.respondWithNoContent();
            if (1 != 1) {
                throw new SerializerException(null, null);
            }
            /* // FIXME
            context.respondWithEntity(entity,
                    exportContentType,
                    HttpStatusCode.OK,
                    odata,
                    serviceMetadata);

             */

        } catch (SerializerException | DeserializerException e) {
            throw new ODataRuntimeException(e);
        }
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
                context.getActionParameter(importContentType, odata));
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
