package net.moewes.quarkus.odata;

import java.util.List;

public interface EntityCollectionProvider<T> {

    public List<T> getCollection();
}
