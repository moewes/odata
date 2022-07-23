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
import net.moewes.quarkus.odata.runtime.CsdlBuilder;
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
    private static final DotName ENTITY_SET = DotName.createSimple(ODataEntitySet.class.getName());
    private static final DotName ENTITY_TYPE = DotName.createSimple(ODataEntity.class.getName());
    private static final DotName ACTION = DotName.createSimple(ODataAction.class.getName());
    private static final DotName NAVIGATION_BINDING =
            DotName.createSimple(ODataNavigationBinding.class.getName());
    private static final DotName FUNCTION = DotName.createSimple(ODataFunction.class.getName());
    static private final DotName APPLICATION_SCOPED =
            DotName.createSimple(ApplicationScoped.class.getName());

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
        return new AdditionalBeanBuildItem(EdmRepository.class,
                CsdlBuilder.class, ODataServlet.class);
    }

    @BuildStep
    UnremovableBeanBuildItem unremovable() {

        return UnremovableBeanBuildItem.beanTypes(ODataServlet.class);
    }

    @BuildStep
    BeanDefiningAnnotationBuildItem registerServiceAnnotation() {
        return new BeanDefiningAnnotationBuildItem(ENTITY_SET, APPLICATION_SCOPED, false);
    }

    @BuildStep
    void scanForEntities(BeanArchiveIndexBuildItem beanArchiveIndex,
                         BuildProducer<EntityTypeBuildItem> buildProducer) {

        IndexView indexView = beanArchiveIndex.getIndex();
        Collection<AnnotationInstance> entityTypes = indexView.getAnnotations(ENTITY_TYPE);

        entityTypes.forEach(annotationInstance -> {
            String name = annotationInstance.value().asString();
            String className = annotationInstance.target().asClass().name().toString();

            buildProducer.produce(new EntityTypeBuildItem(name,
                    className,
                    createEntityType(name, annotationInstance.target().asClass())));
        });
    }

    @BuildStep
    void scanForEntitySets(BeanArchiveIndexBuildItem beanArchiveIndex,
                           BuildProducer<EntitySetBuildItem> buildProducer) {

        IndexView indexView = beanArchiveIndex.getIndex();
        Collection<AnnotationInstance> integrationCards = indexView.getAnnotations(ENTITY_SET);

        integrationCards.forEach(annotationInstance -> {
            String name = annotationInstance.value().asString();
            String className = annotationInstance.target().asClass().name().toString();
            log.info("EntitySet " + name + " ; " + className);


            //    buildProducer.produce(new EntitySetBuildItem(name, className, entitySet));
        });
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void scanForServices(ODataServiceRecorder recorder, // TODO
                         List<EntityTypeBuildItem> entityTypeBuildItems,
                         BeanArchiveIndexBuildItem beanArchiveIndex,
                         BeanContainerBuildItem beanContainer,
                         BuildProducer<ReflectiveClassBuildItem> reflectiveClassBuildItemProducer
            , BuildProducer<EntitySetBuildItem> buildProducer) {

        Map<String, String> entityTypes = new HashMap<>();

        IndexView index = beanArchiveIndex.getIndex();

        for (AnnotationInstance entityType : index.getAnnotations(ENTITY_TYPE)) {
            log.debug("found EntityType " + entityType.target().toString());
            String name = entityType.value().asString();

            entityTypes.put(entityType.target().toString(), name);
        }

        Collection<AnnotationInstance> services = index.getAnnotations(ENTITY_SET);

        for (AnnotationInstance service : services) {
            log.debug("found " + service.target().toString());
            String name = service.value().asString();
            List<Action> navigationBindings = new ArrayList<>();

            service.target().asClass().methods().forEach(methodInfo -> {
                if (methodInfo.hasAnnotation(FUNCTION)) {
                    String functionName = methodInfo.name();
                    log.debug("found function " + functionName);
                    Function function = new Function();
                    function.setName(functionName);
                    function.setEntitySet(name);
                    // TODO Parameter
                    recorder.registerFunction(beanContainer.getValue(),
                            functionName,
                            name,
                            function);
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
                        Optional<EntityTypeBuildItem> optional =
                                findEntityType(entityTypeBuildItems, parameter);
                        if (optional.isPresent()) {
                            actionParameter.setBindingParameter(true);
                            actionParameter.setEntityType(optional.get().getName());
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
                    Optional<EntityTypeBuildItem> optional =
                            findEntityType(entityTypeBuildItems, returnType);
                    if (optional.isPresent()) {
                        returnParameter.setBindingParameter(true);
                        returnParameter.setEntityType(optional.get().getName());
                    } else {
                        returnParameter.setEdmType(getEdmType(returnType.name().toString()));
                    }
                    action.setReturnType(returnParameter);
                    recorder.registerAction(beanContainer.getValue(), actionName, name, action);
                }
                if (methodInfo.hasAnnotation(NAVIGATION_BINDING)) {
                    String actionName = methodInfo.name();
                    if (actionName.startsWith("get")) {
                        actionName = actionName.substring(3);
                    }
                    log.debug("found Navigation" + actionName);

                    Action action = new Action();
                    action.setName(actionName);
                    action.setEntitySet(name);
                    List<Parameter> actionParameters = new ArrayList<>();
                    int i = 0;
                    for (Type parameterType : methodInfo.parameters()) { // FIXME es sollte nur einen Parameter geben! und der sollte dem EntityType des Sets entsprechen
                        Parameter actionParameter = createParameter(entityTypes, parameterType);
                        actionParameter.setName(methodInfo.parameterName(i));
                        actionParameters.add(actionParameter);
                        i++;
                    }
                    action.setParameter(actionParameters);

                    Type returnType = methodInfo.returnType();
                    Parameter returnParameter = createParameter(entityTypes, returnType);
                    action.setReturnType(returnParameter);
                    navigationBindings.add(action);
                }
            });

            buildProducer.produce(new EntitySetBuildItem(name,
                    service.value("entityType").asString(),
                    new EntitySet(name, service.value("entityType").asString(),
                            service.target().asClass().name().toString(), navigationBindings)));
        }
    }


    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void registerElements(List<EntityTypeBuildItem> entityTypeBuildItems,
                          List<EntitySetBuildItem> entitySetBuildItems,
                          BeanContainerBuildItem beanContainer, ODataServiceRecorder recorder) {

        entityTypeBuildItems.forEach(entityTypeBuildItem -> {
            recorder.registerEntityType(beanContainer.getValue(),
                    entityTypeBuildItem.getName(), entityTypeBuildItem.getEntityType());
        });
        entitySetBuildItems.forEach(entitySetBuildItem -> {
            recorder.registerEntitySet(beanContainer.getValue(), entitySetBuildItem.getName(),
                    entitySetBuildItem.getEntitySet());
        });
    }

    private Parameter createParameter(Map<String, String> entityTypes, Type parameterType) {
        log.debug(parameterType.toString());
        Parameter parameter = new Parameter();

        if ("java.util.List".equals(parameterType.name().toString()) && parameterType.kind()
                .equals(Type.Kind.PARAMETERIZED_TYPE)) {
            parameter.setCollection(true);
            parameter.setTypeName(parameterType.asParameterizedType()
                    .arguments()
                    .get(0)
                    .name()
                    .toString());
        } else {
            parameter.setTypeName(parameterType.name().toString());
        }
        parameter.setTypeKind(parameterType.kind().name());

        if (entityTypes.containsKey(parameter.getTypeName())) {
            parameter.setBindingParameter(true);
            parameter.setEntityType(entityTypes.get(parameter.getTypeName()));
        } else {
            parameter.setEdmType(getEdmType(parameter.getTypeName()));
        }
        log.debug("Parameter " + parameter);
        return parameter;
    }

    private EdmPrimitiveTypeKind getEdmType(String typeName) {
        return DataTypes.getEdmTypeForClassName(typeName);
    }

    private EntityType createEntityType(String name, ClassInfo classInfo) {

        final List<MethodInfo> methods = classInfo.methods();
        final Map<String, EntityProperty> propertyMap = new HashMap<>();

        String propertyName;
        EntityProperty property;

        for (final MethodInfo method : methods) {

            if (method.name().length() < 4 ||
                    (!method.name().startsWith("get") && !method.name()
                            .startsWith("is") && !method.name()
                            .startsWith("set"))) {
                continue;
            }

            propertyName =
                    method.name().startsWith("is") ? method.name().substring(2) : method.name()
                            .substring(3);

            property = propertyMap.computeIfAbsent(propertyName, key -> {
                EntityProperty p = new EntityProperty();
                p.setName(key);
                return p;
            });

            if ((method.name().startsWith("get") || method.name()
                    .startsWith("is")) && method.parameters().size() == 0) {
                Type returnType = method.returnType();
                log.info("Prop: " + propertyName + "; Type: " + returnType.toString());
                property.setEdmType(getEdmType(returnType.toString()));
                property.setGetterName(method.name());

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

    private Optional<EntityTypeBuildItem> findEntityType(List<EntityTypeBuildItem> entityTypeBuildItems,
                                                         Type parameter) {
        return entityTypeBuildItems.stream()
                .filter(entityTypeBuildItem -> entityTypeBuildItem.getClassName()
                        .equals(parameter.name().toString()))
                .findFirst();
    }
}
