package net.moewes.quarkus.odata;

import java.util.Optional;

public interface EntityProvider<T, K> {

    Optional<T> find(String key);
}
