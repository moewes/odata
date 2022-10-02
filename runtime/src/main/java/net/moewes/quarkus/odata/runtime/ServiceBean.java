package net.moewes.quarkus.odata.runtime;

import net.moewes.quarkus.odata.EntityCollectionProvider;
import net.moewes.quarkus.odata.repository.Callable;
import net.moewes.quarkus.odata.repository.DataTypeKind;
import net.moewes.quarkus.odata.repository.DataTypes;
import net.moewes.quarkus.odata.repository.EntitySet;
import org.apache.olingo.commons.api.data.Parameter;
import org.apache.olingo.commons.api.ex.ODataRuntimeException;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriParameter;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.CDI;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class ServiceBean {

    private final Object serviceBean;
    private EntitySet entitySet;

    public ServiceBean(EntitySet entitySet) {

        this(entitySet.getBeanClassName());
        this.entitySet = entitySet;
    }

    public ServiceBean(String className) {
        try {
            Class<?> beanClass = Class.forName(className, true,
                    Thread.currentThread().getContextClassLoader());
            serviceBean = CDI.current().select(beanClass, Default.Literal.INSTANCE).get();
        } catch (ClassNotFoundException e) {
            throw new ODataRuntimeException("Service class " + className + " " +
                    "not found");
        }
    }

    @Deprecated
    public Object getServiceBean() {
        return serviceBean;
    }

    public Object getBoundEntityData(ODataRequestContext context,
                                     ODataEntityConverter odataEntityConverter)
            throws ODataApplicationException {

        if (serviceBean instanceof EntityCollectionProvider<?> && entitySet != null) {
            List<UriParameter> keyPredicates = context.getKeyPredicates();
            Map<String, String> keys = new HashMap<>();
            odataEntityConverter.convertKeysToAppFormat(keyPredicates, entitySet, keys);
            return ((EntityCollectionProvider<?>) serviceBean).find(keys)
                    .orElseThrow(() -> new ODataApplicationException("could not find bound entity " +
                            "data", 404, Locale.ENGLISH));
        } else {
            return null; // FIXME better throw exception
        }
    }

    public Object callAction(ActionRequestContext context,
                             Callable action,
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

    public Object call(
            Callable action,
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
                    serviceBean.getClass().getDeclaredMethod(action.getMethodName(),
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
