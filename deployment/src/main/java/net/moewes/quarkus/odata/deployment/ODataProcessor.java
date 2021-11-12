package net.moewes.quarkus.odata.deployment;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.BeanDefiningAnnotationBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.undertow.deployment.ServletBuildItem;
import net.moewes.quarkus.odata.annotations.EntityKey;
import net.moewes.quarkus.odata.annotations.ODataEntity;
import net.moewes.quarkus.odata.annotations.ODataService;
import net.moewes.quarkus.odata.repository.EntityProperty;
import net.moewes.quarkus.odata.repository.EntitySet;
import net.moewes.quarkus.odata.repository.EntityType;
import net.moewes.quarkus.odata.runtime.EdmRepository;
import net.moewes.quarkus.odata.runtime.ODataServiceRecorder;
import net.moewes.quarkus.odata.runtime.ODataServlet;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;

class ODataProcessor {

    private static final String FEATURE = "OData V4";
    private static final DotName SERVICE = DotName.createSimple(ODataService.class.getName());
    private static final DotName ENTITYTYPE = DotName.createSimple(ODataEntity.class.getName());
    static private final DotName APPLICATION_SCOPED = DotName.createSimple(ApplicationScoped.class.getName());

    private static final Logger log = Logger.getLogger(ODataProcessor.class);

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    ServletBuildItem createODataServlet() {
        ServletBuildItem servletBuildItem = ServletBuildItem.builder("odata", ODataServlet.class.getName())
                .addMapping("/odata/*")
                .build();
        return servletBuildItem;
    }

    @BuildStep
    AdditionalBeanBuildItem beans() {
        return new AdditionalBeanBuildItem(EdmRepository.class, ODataServlet.class);
    }

    @BuildStep
    UnremovableBeanBuildItem unremovable() {

        // Any bean that has MyService in its set of bean types is considered unremovable
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

        IndexView index = beanArchiveIndex.getIndex();

        for (AnnotationInstance entityType : index.getAnnotations(ENTITYTYPE)) {
            log.info("found EntityType " + entityType.target().toString());
            String name = entityType.value().asString();
            recorder.registerEntityType(beanContainer.getValue(),
                    name, createEntityType(name, entityType.target().asClass()));
        }

        Collection<AnnotationInstance> services = index.getAnnotations(SERVICE);

        for (AnnotationInstance service : services) {
            log.info("found " + service.target().toString());
            String name = service.value().asString();
            recorder.registerEntitySet(beanContainer.getValue(), name,
                    new EntitySet(name, service.value("entityType").asString(),
                            service.target().asClass().name().toString()));
        }
    }

    private EntityType createEntityType(String name, ClassInfo classInfo) {

        final List<MethodInfo> methods = classInfo.methods();
        final Map<String, EntityProperty> propertyMap = new HashMap<>();

        String propertyName = null;
        EntityProperty property = null;

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
                log.info("Prop: " + propertyName + "; Type: " + returnType.toString());
                switch (returnType.toString()) {
                    case "int":
                        property.setEdmType(EdmPrimitiveTypeKind.Int32);
                        break;
                    default:
                        property.setEdmType(EdmPrimitiveTypeKind.String);
                }
            } else if (method.name().startsWith("set") && method.parameters().size() == 1
            ) {
                //property.setSetter(method); // FIXME
            }

            propertyMap.put(propertyName, property);
        }

        classInfo.annotations().forEach((dotName, annotationInstances) -> {
            log.info("Found " + dotName.toString());
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
