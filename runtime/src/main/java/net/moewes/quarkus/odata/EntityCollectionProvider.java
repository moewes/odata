package net.moewes.quarkus.odata;

import java.util.List;

public interface EntityCollectionProvider<T> {

    List<T> getCollection();
}
