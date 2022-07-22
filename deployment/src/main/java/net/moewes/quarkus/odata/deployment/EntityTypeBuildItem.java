package net.moewes.quarkus.odata.deployment;

import io.quarkus.builder.item.MultiBuildItem;
import net.moewes.quarkus.odata.repository.EntityType;

public final class EntityTypeBuildItem extends MultiBuildItem {

    final String name;
    final String className;
    final EntityType entityType;

    public EntityTypeBuildItem(String name, String className, EntityType entityType) {
        this.name = name;
        this.className = className;
        this.entityType = entityType;
    }

    public String getName() {
        return name;
    }

    public String getClassName() {
        return className;
    }

    public EntityType getEntityType() {
        return entityType;
    }
}
