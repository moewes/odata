package net.moewes.odata.it;

import net.moewes.quarkus.odata.EntityCollectionProvider;
import net.moewes.quarkus.odata.EntityProvider;
import net.moewes.quarkus.odata.annotations.ODataService;
import org.apache.olingo.server.api.ODataApplicationException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@ODataService(value = "MyService",entityType = "MyEntity")
public class MyEntityService implements EntityProvider<MyEntity>, EntityCollectionProvider<MyEntity> {
    @Override
    public List<MyEntity> getCollection() {
        return null;
    }

    @Override
    public Optional<MyEntity> find(Map<String, String> keys) {
        return Optional.empty();
    }

    @Override
    public MyEntity create(Object entity) throws ODataApplicationException {
        return null;
    }

    @Override
    public void update(Map<String, String> keys, Object entity) {

    }

    @Override
    public void delete(Map<String, String> keys) {

    }
}
