package net.moewes.quarkus.odata;

import java.util.Map;
import java.util.Optional;

public interface EntityProvider<T, K> {

    Optional<T> find(Map<String, String> keys);
}
