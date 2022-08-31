package net.moewes.quarkus.odata.deployment;

import io.quarkus.builder.item.MultiBuildItem;
import net.moewes.quarkus.odata.repository.EntitySet;

public final class EntitySetBuildItem extends MultiBuildItem {

    final String name;
    final String className;
    final EntitySet entitySet;

    public EntitySetBuildItem(String name, String className, EntitySet entitySet) {
        this.name = name;
        this.className = className;
        this.entitySet = entitySet;
    }

    public String getName() {
        return name;
    }

    public String getClassName() {
        return className;
    }

    public EntitySet getEntitySet() {
        return entitySet;
    }

}
