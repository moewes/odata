package net.moewes.quarkus.odata.runtime;

import net.moewes.quarkus.odata.EntityProvider;
import net.moewes.quarkus.odata.repository.Action;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.Parameter;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType;
import org.apache.olingo.commons.api.edm.EdmAction;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmPrimitiveType;
import org.apache.olingo.commons.api.edm.EdmReturnType;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.*;
import org.apache.olingo.server.api.deserializer.DeserializerException;
import org.apache.olingo.server.api.deserializer.ODataDeserializer;
import org.apache.olingo.server.api.processor.ActionPrimitiveProcessor;
import org.apache.olingo.server.api.serializer.SerializerException;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriParameter;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceAction;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class QuarkusActionProcessor implements ActionPrimitiveProcessor {

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
    public void processActionPrimitive(ODataRequest oDataRequest, ODataResponse oDataResponse, UriInfo uriInfo, ContentType contentType, ContentType contentType1) throws ODataApplicationException {

        ODataRequestContext context = new ODataRequestContext(oDataRequest, oDataResponse, uriInfo);

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
                            Map<String, Parameter> actionParameters = deserializer.actionParameters(oDataRequest.getBody(), edmAction).getActionParameters();

                            Action action = repository.findAction(actionName).orElseThrow(() -> new ODataApplicationException("Can't find Action", HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH));

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
                            Method declaredMethod = serviceBean.getClass().getDeclaredMethod(actionName,
                                    parameterClasses.toArray(Class[]::new));

                            List<Object> valueList = new ArrayList<>();
                            valueList.add(data);
                            action.getParameter().forEach(parameter -> {
                                if (!parameter.isBindingParameter()) {
                                    Object value = actionParameters.get(parameter.getName()).asPrimitive().toString();
                                    valueList.add(value);
                                }
                            });

                            Object result = declaredMethod.invoke(serviceBean, valueList.toArray());

                            Property property = new Property(null, "result", ValueType.PRIMITIVE, result);
                            context.respondWithPrimitive(property, (EdmPrimitiveType) returnType.getType(), contentType, HttpStatusCode.OK, odata, serviceMetadata);
                        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException | SerializerException | DeserializerException | ODataApplicationException e) {
                            e.printStackTrace(); // FIXME
                        }
                        odataEntityConverter.convertDataToFrameworkEntity(entity, entitySet, data);
                    });
                }
            });
        } else {
            throw new ODataApplicationException("Not Supported", HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
        }
    }
}
