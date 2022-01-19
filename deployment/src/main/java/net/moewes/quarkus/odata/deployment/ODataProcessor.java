package net.moewes.quarkus.odata.deployment;

import io.quarkus.arc.deployment.*;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.undertow.deployment.ServletBuildItem;
import net.moewes.quarkus.odata.annotations.*;
import net.moewes.quarkus.odata.repository.*;
import net.moewes.quarkus.odata.runtime.EdmRepository;
import net.moewes.quarkus.odata.runtime.ODataServiceRecorder;
import net.moewes.quarkus.odata.runtime.ODataServlet;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.jboss.jandex.*;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import java.util.*;

class ODataProcessor {

    private static final String FEATURE = "OData V4";
    private static final DotName SERVICE = DotName.createSimple(ODataService.class.getName());
    private static final DotName ENTITY_TYPE = DotName.createSimple(ODataEntity.class.getName());
    private static final DotName ACTION = DotName.createSimple(ODataAction.class.getName());
    private static final DotName FUNCTION = DotName.createSimple(ODataFunction.class.getName());
    static private final DotName APPLICATION_SCOPED = DotName.createSimple(ApplicationScoped.class.getName());

    private static final Logger log = Logger.getLogger(ODataProcessor.class);

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    ServletBuildItem createODataServlet() {
        return ServletBuildItem.builder("odata", ODataServlet.class.getName())
                .addMapping("/odata/*")
                .build();
    }

    @BuildStep
    AdditionalBeanBuildItem beans() {
        return new AdditionalBeanBuildItem(EdmRepository.class, ODataServlet.class);
    }

    @BuildStep
    UnremovableBeanBuildItem unremovable() {

        return UnremovableBeanBuildItem.beanTypes(ODataServlet.class);
    }

    @BuildStep
    BeanDefiningAnnotationBuildItem registerServiceAnnotation() {
        return new BeanDefiningAnnotationBuildItem(SERVICE, APPLICATION_SCOPED, false);
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void scanForServices(ODataServiceRecorder recorder,
                         BeanArchiveIndexBuildItem beanArchiveIndex,
                         BeanContainerBuildItem beanContainer,
                         BuildProducer<ReflectiveClassBuildItem> reflectiveClassBuildItemProducer) {

        Map<String, String> entityTypes = new HashMap<>();

        IndexView index = beanArchiveIndex.getIndex();

        for (AnnotationInstance entityType : index.getAnnotations(ENTITY_TYPE)) {
            log.debug("found EntityType " + entityType.target().toString());
            String name = entityType.value().asString();
            recorder.registerEntityType(beanContainer.getValue(),
                    name, createEntityType(name, entityType.target().asClass()));
            entityTypes.put(entityType.target().toString(), name);
        }

        Collection<AnnotationInstance> services = index.getAnnotations(SERVICE);

        for (AnnotationInstance service : services) {
            log.debug("found " + service.target().toString());
            String name = service.value().asString();

            service.target().asClass().methods().forEach(methodInfo -> {
                if (methodInfo.hasAnnotation(FUNCTION)) {
                    String functionName = methodInfo.name();
                    log.debug("found function " + functionName);
                    Function function = new Function();
                    function.setName(functionName);
                    function.setEntitySet(name);
                    // TODO Parameter
                    recorder.registerFunction(beanContainer.getValue(), functionName, name, function);
                }
                if (methodInfo.hasAnnotation(ACTION)) {
                    String actionName = methodInfo.name();
                    log.debug("found action " + actionName);

                    Action action = new Action();
                    action.setName(actionName);
                    action.setEntitySet(name);
                    List<Parameter> actionParameters = new ArrayList<>();
                    int i = 0;
                    for (Type parameter : methodInfo.parameters()) {
                        log.debug(parameter.toString());
                        Parameter actionParameter = new Parameter();

                        actionParameter.setName(methodInfo.parameterName(i));
                        actionParameter.setTypeKind(parameter.kind().name());
                        actionParameter.setTypeName(parameter.name().toString());
                        if (entityTypes.containsKey(parameter.name().toString())) {
                            actionParameter.setBindingParameter(true);
                            actionParameter.setEntityType(entityTypes.get(parameter.name().toString()));
                        } else {
                            actionParameter.setEdmType(getEdmType(parameter.name().toString()));
                        }
                        actionParameters.add(actionParameter);
                        log.debug("Parameter " + actionParameter);
                        i++;
                    }
                    action.setParameter(actionParameters);

                    Type returnType = methodInfo.returnType();
                    Parameter returnParameter = new Parameter();
                    returnParameter.setTypeName(returnType.name().toString());
                    returnParameter.setTypeKind(returnType.kind().name());
                    if (entityTypes.containsKey(returnType.name().toString())) {
                        returnParameter.setBindingParameter(true);
                        returnParameter.setEntityType(entityTypes.get(returnType.name().toString()));
                    } else {
                        returnParameter.setEdmType(getEdmType(returnType.name().toString()));
                    }
                    action.setReturnType(returnParameter);
                    recorder.registerAction(beanContainer.getValue(), actionName, name, action);
                }
            });

            recorder.registerEntitySet(beanContainer.getValue(), name,
                    new EntitySet(name, service.value("entityType").asString(),
                            service.target().asClass().name().toString()));
        }
    }

    private EdmPrimitiveTypeKind getEdmType(String typeName) {
        switch (typeName) {
            case "int":
                return EdmPrimitiveTypeKind.Int32;
            case "java.time.LocalDate":
                return EdmPrimitiveTypeKind.Date;
            case "java.time.LocalTime":
                return EdmPrimitiveTypeKind.TimeOfDay;
            default:
                return EdmPrimitiveTypeKind.String;
        }
    }

    private EntityType createEntityType(String name, ClassInfo classInfo) {

        final List<MethodInfo> methods = classInfo.methods();
        final Map<String, EntityProperty> propertyMap = new HashMap<>();

        String propertyName;
        EntityProperty property;

        for (final MethodInfo method : methods) {

            if (method.name().length() < 4 ||
                    (!method.name().startsWith("get") && !method.name().startsWith("set"))) {
                continue; // TODO consider has and is
            }

            propertyName = method.name().substring(3);

            property = propertyMap.computeIfAbsent(propertyName, key -> {
                EntityProperty p = new EntityProperty();
                p.setName(key);
                return p;
            });

            if (method.name().startsWith("get") && method.parameters().size() == 0) {
                Type returnType = method.returnType();
                log.debug("Prop: " + propertyName + "; Type: " + returnType.toString());
                property.setEdmType(getEdmType(returnType.toString()));

            } else if (method.name().startsWith("set") && method.parameters().size() == 1
            ) {
                //property.setSetter(method); // FIXME
            }

            propertyMap.put(propertyName, property);
        }

        classInfo.annotations().forEach((dotName, annotationInstances) -> {
            log.debug("Found " + dotName.toString());
            if (DotName.createSimple(EntityKey.class.getName()).equals(dotName)) {
                annotationInstances.forEach(annotationInstance -> {
                    char[] chars = annotationInstance.target().asField().name().toCharArray();
                    chars[0] = Character.toUpperCase(chars[0]);
                    EntityProperty entityProperty = propertyMap.get(new String(chars));
                    entityProperty.setKey(true);
                });
            }
        });
        return new EntityType(name, classInfo.name().toString(), propertyMap);
    }
}
