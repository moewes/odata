package net.moewes.odata.it.basic;

import net.moewes.quarkus.odata.EntityCollectionProvider;
import net.moewes.quarkus.odata.EntityProvider;
import net.moewes.quarkus.odata.annotations.ODataEntitySet;
import org.apache.olingo.server.api.ODataApplicationException;

import javax.annotation.PostConstruct;
import java.util.*;

@ODataEntitySet(value = "BasicSet", entityType = "BasicEntity")
public class MyBasicEnttySet
        implements EntityCollectionProvider<MyBasicEntity>, EntityProvider<MyBasicEntity> {

    private final Map<String, MyBasicEntity> repository = new HashMap<>();

    @PostConstruct
    public void init() {
        MyBasicEntity entity1 = new MyBasicEntity();
        entity1.setId("E1");
        entity1.setFlag(true);
        entity1.setNumber(10);
        entity1.setText("FooBar");
        entity1.setGuid(UUID.fromString("bdc8ffcb-02d7-4a94-93a6-458e35bc7a39"));
        repository.put(entity1.getId(), entity1);

        MyBasicEntity entity2 = new MyBasicEntity();
        entity2.setId("EwDV");
        repository.put(entity2.getId(), entity2);
    }

    @Override
    public List<MyBasicEntity> getCollection() {

        return new ArrayList<>(repository.values());
    }

    @Override
    public Optional<MyBasicEntity> find(Map<String, String> keys) {

        MyBasicEntity entity = repository.get(keys.get("Id"));
        return Optional.ofNullable(entity);
    }

    @Override
    public MyBasicEntity create(Object entity) throws ODataApplicationException {

        MyBasicEntity myBasicEntity = (MyBasicEntity) entity;
        repository.put(myBasicEntity.getId(), myBasicEntity);
        return myBasicEntity;
    }

    @Override
    public void update(Map<String, String> keys, Object entity) {

    }

    @Override
    public void delete(Map<String, String> keys) {

    }
}
