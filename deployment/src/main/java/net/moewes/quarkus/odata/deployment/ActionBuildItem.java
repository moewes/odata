package net.moewes.quarkus.odata.deployment;

import io.quarkus.builder.item.MultiBuildItem;
import net.moewes.quarkus.odata.repository.Callable;

public final class ActionBuildItem extends MultiBuildItem {

    final String name;
    final String entitySet;
    final Callable callable;

    public ActionBuildItem(String name, String entitySet, Callable callable) {
        this.name = name;
        this.entitySet = entitySet;
        this.callable = callable;
    }
}
