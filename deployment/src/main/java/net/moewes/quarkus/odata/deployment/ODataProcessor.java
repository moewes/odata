package net.moewes.quarkus.odata.deployment;

import io.quarkus.arc.deployment.*;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.undertow.deployment.ServletBuildItem;
import jakarta.enterprise.context.ApplicationScoped;
import net.moewes.quarkus.odata.annotations.*;
import net.moewes.quarkus.odata.repository.*;
import net.moewes.quarkus.odata.runtime.ODataServiceRecorder;
import net.moewes.quarkus.odata.runtime.ODataServlet;
import net.moewes.quarkus.odata.runtime.edm.CsdlBuilder;
import net.moewes.quarkus.odata.runtime.edm.EdmRepository;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.jboss.jandex.*;
import org.jboss.logging.Logger;

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
                    createEntityType(indexView, name, annotationInstance.target().asClass())));
        });
    }

    @BuildStep
    void scanForEntitySets(BeanArchiveIndexBuildItem beanArchiveIndex,
                           BuildProducer<EntitySetBuildItem> buildProducer) {

        IndexView indexView = beanArchiveIndex.getIndex();
        Collection<AnnotationInstance> entitySets = indexView.getAnnotations(ENTITY_SET);

        entitySets.forEach(annotationInstance -> {
            String name = annotationInstance.value().asString();
            String className = annotationInstance.target().asClass().name().toString();
            log.debug("EntitySet " + name + " ; " + className);


            //    buildProducer.produce(new EntitySetBuildItem(name, className, entitySet));
        });
    }

    @BuildStep
    void scanForActions(BeanArchiveIndexBuildItem beanArchiveIndex,
                        List<EntityTypeBuildItem> entityTypeBuildItems,
                        BuildProducer<ActionBuildItem> buildProducer) {

        IndexView indexView = beanArchiveIndex.getIndex();
        Collection<AnnotationInstance> actions =
                indexView.getAnnotations(ACTION);

        actions.forEach(annotationInstance -> {
            MethodInfo methodInfo = annotationInstance.target().asMethod();
            Callable callable = initCallableFromMethodInfo(annotationInstance, methodInfo);

            List<Parameter> parameters = new ArrayList<>();
            int i = 0;
            for (Type bindingType : methodInfo.parameterTypes()) {
                Parameter bindingParameter =
                        createParameter(entityTypeBuildItems, bindingType);
                bindingParameter.setName(methodInfo.parameterName(i));
                parameters.add(bindingParameter);
                i++;
            }
            callable.setParameter(parameters);

            AnnotationInstance classAnnotation =
                    methodInfo.declaringClass().classAnnotation(ENTITY_SET);
            String entitySet =
                    (classAnnotation != null && classAnnotation.value() != null) ?
                            classAnnotation.value().asString() :
                            null;
            callable.setEntitySet(entitySet);

            Type returnType = methodInfo.returnType();
            Parameter returnParameter =
                    createParameter(entityTypeBuildItems, returnType);

            callable.setReturnType(returnParameter);

            buildProducer.produce(new ActionBuildItem(callable.getName(),
                    entitySet,
                    callable));
        });
    }

    @BuildStep
    void scanForNavigationBindings(BeanArchiveIndexBuildItem beanArchiveIndex,
                                   List<EntityTypeBuildItem> entityTypeBuildItems,
                                   BuildProducer<NavigationBindingBuildItem> buildProducer) {

        IndexView indexView = beanArchiveIndex.getIndex();
        Collection<AnnotationInstance> navigationBindings =
                indexView.getAnnotations(NAVIGATION_BINDING);

        navigationBindings.forEach(annotationInstance -> {
            MethodInfo methodInfo = annotationInstance.target().asMethod();
            Callable callable = initCallableFromMethodInfo(annotationInstance, methodInfo);

            if (methodInfo.parameterTypes().size() != 1) {
                throw new RuntimeException("Navigation binding method should have exactly one " +
                        "parameter");
            }
            Type bindingType = methodInfo.parameterTypes().get(0);
            Parameter bindingParameter =
                    createParameter(entityTypeBuildItems, bindingType);

            if (!bindingParameter.isBindingParameter()) {
                // throw new RuntimeException("Cannot find entity type " + bindingType); // FIXME
            }

            List<Parameter> parameters = new ArrayList<>();
            parameters.add(bindingParameter);
            callable.setParameter(parameters);

            Type returnType = methodInfo.returnType();
            Parameter returnParameter =
                    createParameter(entityTypeBuildItems, returnType);

            if (!returnParameter.isBindingParameter()) {
                //  throw new RuntimeException("Cannot find entity type " + returnType); // FIXME
            }
            callable.setReturnType(returnParameter);

            buildProducer.produce(new NavigationBindingBuildItem(callable.getName(), callable));
        });
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void scanForServices(ODataServiceRecorder recorder, // TODO
                         List<EntityTypeBuildItem> entityTypeBuildItems,
                         List<NavigationBindingBuildItem> navigationBindingBuildItems, // TODO
                         BeanArchiveIndexBuildItem beanArchiveIndex,
                         BeanContainerBuildItem beanContainer,
                         BuildProducer<ReflectiveClassBuildItem> reflectiveClassBuildItemProducer
            , BuildProducer<EntitySetBuildItem> buildProducer) {

        IndexView index = beanArchiveIndex.getIndex();

        Collection<AnnotationInstance> services = index.getAnnotations(ENTITY_SET);

        for (AnnotationInstance service : services) {
            log.debug("found " + service.target().toString());
            String name = service.value().asString();
            List<Callable> navigationBindings = new ArrayList<>();

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
            });

            navigationBindingBuildItems.stream()
                    .filter(navigationBindingBuildItem -> navigationBindingBuildItem.callable.getParameter()
                            .get(0)
                            .getEntityType()
                            .equals(service.value("entityType").asString()))
                    .forEach(navigationBindingBuildItem -> {
                        navigationBindingBuildItem.callable.setEntitySet(name);
                        navigationBindings.add(navigationBindingBuildItem.callable);
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
                          List<NavigationBindingBuildItem> navigationBindingBuildItems,
                          List<ActionBuildItem> actionBuildItems,
                          BeanContainerBuildItem beanContainer, ODataServiceRecorder recorder) {

        entityTypeBuildItems.forEach(entityTypeBuildItem -> recorder.registerEntityType(
                beanContainer.getValue(),
                entityTypeBuildItem.getName(),
                entityTypeBuildItem.getEntityType()));
        entitySetBuildItems.forEach(entitySetBuildItem -> recorder.registerEntitySet(beanContainer.getValue(),
                entitySetBuildItem.getName(),
                entitySetBuildItem.getEntitySet()));
        actionBuildItems.forEach(actionBuildItem -> recorder.registerAction(beanContainer.getValue(),
                actionBuildItem.name,
                actionBuildItem.entitySet,
                actionBuildItem.callable));
    }

    private Callable initCallableFromMethodInfo(AnnotationInstance annotationInstance,
                                                MethodInfo methodInfo) {

        String name = (annotationInstance.value() != null) ?
                annotationInstance.value().asString() : methodInfo.name();
        String methodName = methodInfo.name();
        ClassInfo declaringClass = methodInfo.declaringClass();
        String className = declaringClass.simpleName();
        Callable callable = new Callable();
        callable.setName(name);
        callable.setClassName(className);
        callable.setMethodName(methodName);
        return callable;
    }

    private Parameter createParameter(List<EntityTypeBuildItem> entityTypeBuildItems,
                                      Type parameterType) {
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
        parameter.setTypeKind(getDataTypeKind(parameterType.kind()));

        Optional<EntityTypeBuildItem> entityTypeOptional = entityTypeBuildItems.stream()
                .filter(entityTypeBuildItem -> entityTypeBuildItem.entityType.getClassName()
                        .equals(parameter.getTypeName()))
                .findFirst();

        if (entityTypeOptional.isPresent()) {
            parameter.setBindingParameter(true);
            parameter.setEntityType(entityTypeOptional.get().entityType.getName());
        } else {
            parameter.setEdmType(getEdmType(parameter.getTypeName()));
        }
        log.debug("Parameter " + parameter);
        return parameter;
    }

    private EdmPrimitiveTypeKind getEdmType(String typeName) {
        return DataTypes.getEdmTypeForClassName(typeName);
    }

    private EntityType createEntityType(IndexView indexView, String name, ClassInfo classInfo) {

        final Map<String, EntityProperty> propertyMap = new HashMap<>();
        final DotName javaLangObject = DotName.createSimple("java.lang.Object");

        final List<MethodInfo> methods = new ArrayList<>(classInfo.methods());
        final Map<DotName, List<AnnotationInstance>> annotationsMap =
                new HashMap<>(classInfo.annotationsMap());

        fillMethodsAndAnnotationsFromSuperClasses(indexView,
                classInfo,
                methods,
                annotationsMap,
                javaLangObject);

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
                    .startsWith("is")) && method.parameterTypes().size() == 0) {
                Type returnType = method.returnType();
                log.debug("Prop: " + propertyName + "; Type: " + returnType.toString());
                property.setEdmType(getEdmType(returnType.toString()));
                property.setGetterName(method.name());

            } else if (method.name().startsWith("set") && method.parameterTypes().size() == 1
            ) {
                //property.setSetter(method); // FIXME
            }

            propertyMap.put(propertyName, property);
        }

        annotationsMap.forEach((dotName, annotationInstances) -> {
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

    private void fillMethodsAndAnnotationsFromSuperClasses(IndexView indexView,
                                                           ClassInfo classInfo,
                                                           List<MethodInfo> methods,
                                                           Map<DotName, List<AnnotationInstance>> annotationsMap,
                                                           DotName javaLangObject) {
        Type type = classInfo.superClassType();

        while (!javaLangObject.equals(type.name())) {
            ClassInfo superClass = indexView.getClassByName(type.name());
            methods.addAll(superClass.methods());
            superClass.annotationsMap().forEach((dotName, annotationInstances) -> {
                List<AnnotationInstance> instances = annotationsMap.get(dotName);
                if (instances == null) {
                    annotationsMap.put(dotName, annotationInstances);
                } else {
                    instances.addAll(annotationInstances);
                }
            });
            type = superClass.superClassType();
        }
    }

    private DataTypeKind getDataTypeKind(Type.Kind kind) {

        switch (kind) {
            case CLASS:
                return DataTypeKind.CLASS;
            case PRIMITIVE:
                return DataTypeKind.PRIMITIVE;
            default:
                return DataTypeKind.UNSUPPORTED;
        }
    }
}
