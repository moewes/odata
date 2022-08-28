package net.moewes.quarkus.odata.deployment;

import io.quarkus.builder.item.MultiBuildItem;
import net.moewes.quarkus.odata.repository.Callable;

public final class NavigationBindingBuildItem extends MultiBuildItem {

    final String name;

    final Callable callable;

    public NavigationBindingBuildItem(String name, Callable callable) {
        this.name = name;
        this.callable = callable;
    }
}
