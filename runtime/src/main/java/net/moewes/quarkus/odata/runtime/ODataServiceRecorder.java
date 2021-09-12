package net.moewes.quarkus.odata.runtime;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.annotations.Recorder;
import net.moewes.quarkus.odata.repository.EntitySet;
import net.moewes.quarkus.odata.repository.EntityType;

@Recorder
public class ODataServiceRecorder {

    public void registerEntityType(BeanContainer beanContainer, String name, EntityType entityType) {
        EdmRepository repository = beanContainer.instance(EdmRepository.class);
        repository.registerEntity(name, entityType);
    }

    public void registerEntitySet(BeanContainer beanContainer, String name, EntitySet entitySet) {
        EdmRepository repository = beanContainer.instance(EdmRepository.class);
        repository.registerEntitySet(name, entitySet);
    }
}
