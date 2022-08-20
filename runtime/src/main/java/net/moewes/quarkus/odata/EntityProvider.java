package net.moewes.quarkus.odata;

import org.apache.olingo.server.api.ODataApplicationException;

import java.util.Map;

public interface EntityProvider<T> {

    //Optional<T> find(Map<String, String> keys);

    T create(Object entity) throws ODataApplicationException; // TODO refactor errors?

    void update(Map<String, String> keys, Object entity);

    void delete(Map<String, String> keys);
}
