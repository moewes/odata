package net.moewes.quarkus.odata.runtime;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.annotations.Recorder;
import net.moewes.quarkus.odata.repository.Action;
import net.moewes.quarkus.odata.repository.EntitySet;
import net.moewes.quarkus.odata.repository.EntityType;
import net.moewes.quarkus.odata.repository.Function;

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

    public void registerAction(BeanContainer beanContainer, String name, String entitySet, Action action) {
        EdmRepository repository = beanContainer.instance(EdmRepository.class);
        repository.registerAction(name, entitySet, action);
    }

    public void registerFunction(BeanContainer beanContainer, String name, String entitySetName, Function function) {
        EdmRepository repository = beanContainer.instance(EdmRepository.class);
        repository.registerFunction(name, entitySetName, function);
    }
}
