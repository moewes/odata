package net.moewes.quarkus.odata;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface EntityCollectionProvider<T> {

    List<T> getCollection();

    Optional<T> find(Map<String, String> keys);
}
