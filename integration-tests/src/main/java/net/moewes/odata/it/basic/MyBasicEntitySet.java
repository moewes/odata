package net.moewes.odata.it.basic;

import jakarta.annotation.PostConstruct;
import net.moewes.quarkus.odata.EntityCollectionProvider;
import net.moewes.quarkus.odata.EntityProvider;
import net.moewes.quarkus.odata.annotations.ODataAction;
import net.moewes.quarkus.odata.annotations.ODataEntitySet;
import org.apache.olingo.server.api.ODataApplicationException;

import java.util.*;

@ODataEntitySet(value = "BasicSet", entityType = "BasicEntity")
public class MyBasicEntitySet
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

    @ODataAction
    public String actionReturningStringValue(MyBasicEntity entity, String parameter) {
        return "action on entity with id " + entity.getId() + " with parameter " + parameter;
    }

    @ODataAction
    public MyBasicEntity actionReturningEntityType(MyBasicEntity entity, String parameter) {
        entity.setText(parameter);
        return entity;
    }

    @ODataAction
    public List<MyBasicEntity> actionReturningList(MyBasicEntity entity, int parameter) {
        List<MyBasicEntity> result = new ArrayList<>();

        for (int i = 0; i < parameter; i++) {
            result.add(entity);
        }
        return result;
    }

    @ODataAction
    public List<String> actionReturningListOfString(MyBasicEntity entity, int parameter) {
        List<String> result = new ArrayList<>();

        for (int i = 0; i < parameter; i++) {
            result.add(entity.getText());
        }
        return result;
    }
}
