package net.moewes.quarkus.odata;

import java.util.Map;
import java.util.Optional;

public interface EntityProvider<T> {

    Optional<T> find(Map<String, String> keys);

    T create(Object entity); // TODO refactor errors?

    void delete(Map<String, String> keys);
}
