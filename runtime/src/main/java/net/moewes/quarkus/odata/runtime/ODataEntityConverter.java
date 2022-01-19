package net.moewes.quarkus.odata.runtime;

import net.moewes.quarkus.odata.repository.EntityProperty;
import net.moewes.quarkus.odata.repository.EntitySet;
import net.moewes.quarkus.odata.repository.EntityType;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.ex.ODataRuntimeException;
import org.apache.olingo.server.api.uri.UriParameter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

import static org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind.String;

public class ODataEntityConverter {
    private final EdmRepository repository;

    public ODataEntityConverter(EdmRepository repository) {
        this.repository = repository;
    }

    void convertKeysToAppFormat(List<UriParameter> keyPredicates, EntitySet item, Map<String,
            String> keys) {
        for (UriParameter keyPredicate : keyPredicates) {
            String value = keyPredicate.getText();
            EntityType entityType =
                    repository.findEntityType(item.getEntityType()).orElseThrow(RuntimeException::new);

            EdmPrimitiveTypeKind edmType =
                    entityType.getPropertyMap().get(keyPredicate.getName()).getEdmType();
            if (edmType == String) { // TODO Refactor
                value = value.substring(1, value.length() - 1);
            }
            keys.put(keyPredicate.getName(), value);
        }
    }

    void convertDataToFrameworkEntity(Entity entity, EntitySet entitySet, Object data) {
        EntityType entityType = repository.findEntityType(entitySet.getEntityType())
                .orElseThrow(() -> new ODataRuntimeException("EntityType " + entitySet.getEntityType() + " not found"));

        for (EntityProperty entityProperty : entityType.getPropertyMap().values()) {
            try {
                Method getter = data.getClass().getDeclaredMethod("get" + entityProperty.getName());
                Object result2 = getter.invoke(data);
                /*
                switch (entityProperty.getEdmType()) {
                    case Date:
                        LocalDate date = (LocalDate) result2;
                        Instant instant = Instant.from(date.atStartOfDay(ZoneId.of("CET"))); // TODO
                        result2 = Date.from(instant);
                        break;
                    case TimeOfDay:
                        LocalTime time = (LocalTime) result2; //= null; // FIXME

                        break;
                    default:
                }
                */
                entity.addProperty(new Property(null, entityProperty.getName(),
                        ValueType.PRIMITIVE, result2));
            } catch (NoSuchMethodException e) {
                throw new ODataRuntimeException("Getter for " + entityProperty.getName() + " not " +
                        "found");
            } catch (InvocationTargetException e) {
                throw new ODataRuntimeException("Getter for " + entityProperty.getName() + " " +
                        "InvocationTargetException ");
            } catch (IllegalAccessException e) {
                throw new ODataRuntimeException("Getter for " + entityProperty.getName() + " " +
                        "IllegalAccessException ");
            }
        }
    }

    Object convertFrameworkEntityToAppData(Entity entity, EntitySet entitySet) {

        Object data = null;

        EntityType entityType = repository.findEntityType(entitySet.getEntityType())
                .orElseThrow(() -> new ODataRuntimeException("EntityType " + entitySet.getEntityType() + " not found"));

        try {
            Class dataClass =
                    Thread.currentThread().getContextClassLoader().loadClass(entityType.getClassName());
            data = dataClass.getDeclaredConstructor().newInstance();
        } catch (ClassNotFoundException e) {
            throw new ODataRuntimeException("Entity Class for " + entityType.getName() + " not " +
                    "found");
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }

        for (EntityProperty entityProperty : entityType.getPropertyMap().values()) {
            try {
                Property property = entity.getProperty(entityProperty.getName());
                if (property != null) {
                    Method setter =
                            data.getClass().getDeclaredMethod("set" + entityProperty.getName(),
                                    getValueClass(entityProperty.getEdmType()));
                    setter.invoke(data, getValue(entityProperty, property));
                }
            } catch (NoSuchMethodException e) {
                throw new ODataRuntimeException("Setter for " + entityProperty.getName() + " not " +
                        "found");
            } catch (InvocationTargetException e) {
                throw new ODataRuntimeException("Setter for " + entityProperty.getName() + " " +
                        "InvocationTargetException ");
            } catch (IllegalAccessException e) {
                throw new ODataRuntimeException("Setter for " + entityProperty.getName() + " " +
                        "IllegalAccessException ");
            }
        }
        return data;
    }

    public Object patchFrameworkEntityToAppData(Entity entity, EntitySet entitySet,
                                                Object old_data) {

        EntityType entityType = repository.findEntityType(entitySet.getEntityType())
                .orElseThrow(() -> new ODataRuntimeException("EntityType " + entitySet.getEntityType() + " not found"));

        for (EntityProperty entityProperty : entityType.getPropertyMap().values()) {
            try {
                Property property = entity.getProperty(entityProperty.getName());
                if (property != null) {
                    Method setter =
                            old_data.getClass().getDeclaredMethod("set" + entityProperty.getName(),
                                    getValueClass(entityProperty.getEdmType()));
                    setter.invoke(old_data, getValue(entityProperty, property));
                }
            } catch (NoSuchMethodException e) {
                throw new ODataRuntimeException("Setter for " + entityProperty.getName() + " not " +
                        "found");
            } catch (InvocationTargetException e) {
                throw new ODataRuntimeException("Setter for " + entityProperty.getName() + " " +
                        "InvocationTargetException ");
            } catch (IllegalAccessException e) {
                throw new ODataRuntimeException("Setter for " + entityProperty.getName() + " " +
                        "IllegalAccessException ");
            }
        }
        return old_data;
    }

    private Object getValue(EntityProperty entityProperty, Property property) {
        Object value = property.getValue();
        if (value instanceof GregorianCalendar) {
            if (entityProperty.getEdmType() == EdmPrimitiveTypeKind.Date) {
                value = ((GregorianCalendar) value).toZonedDateTime().toLocalDate();
            }
            if (entityProperty.getEdmType() == EdmPrimitiveTypeKind.TimeOfDay) {
                value = ((GregorianCalendar) value).toZonedDateTime().toLocalTime();
            }
        }
        return value;
    }

    private Class getValueClass(EdmPrimitiveTypeKind edmType) {

        switch (edmType) {
            case Date:
                return LocalDate.class;
            case TimeOfDay:
                return LocalTime.class;
            default:
                return String.class;
        }
    }
}