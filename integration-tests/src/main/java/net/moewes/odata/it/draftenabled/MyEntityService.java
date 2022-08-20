package net.moewes.odata.it.draftenabled;

import net.moewes.odata.it.MySecondEntity;
import net.moewes.quarkus.odata.EntityCollectionProvider;
import net.moewes.quarkus.odata.EntityProvider;
import net.moewes.quarkus.odata.annotations.ODataAction;
import net.moewes.quarkus.odata.annotations.ODataEntitySet;
import net.moewes.quarkus.odata.annotations.ODataFunction;
import org.apache.olingo.server.api.ODataApplicationException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@ODataEntitySet(value = "MyService", entityType = "MyEntity")
public class MyEntityService
        implements EntityProvider<MyEntity>, EntityCollectionProvider<MyEntity> {
    @Override
    public List<MyEntity> getCollection() {

        List<MyEntity> result = new ArrayList<>();

        MyEntity entity = new MyEntity();
        entity.setId("Id");
        entity.setName("Entity with id " + entity.getId());
        result.add(entity);

        MyEntity entity2 = new MyEntity();
        entity2.setId("E2");
        entity2.setName("Entity with id " + entity.getId());
        entity2.setIsActiveEntity(true);
        entity2.setHasActiveEntity(true);
        result.add(entity2);
        return result;
    }

    @Override
    public Optional<MyEntity> find(Map<String, String> keys) {

        MyEntity entity = new MyEntity();
        entity.setId(keys.get("Id"));
        entity.setName("Entity with id " + entity.getId());

        return Optional.of(entity);
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

    //@ODataNavigationBinding("SiblingEntity") // FIXME
    public MyEntity getSiblingEntity(MyEntity entity) {
        return null;
    }

    //@ODataNavigationBinding("DraftAdministrativData") // FIXME
    public DraftAdministrativeData getDraftAdministrativeData(MyEntity entity) {
        return null;
    }

    //@ODataNavigationBinding("SecondEntity") // FIXME
    public List<MySecondEntity> getNavigation(MyEntity entity) {
        return null;
    }

    @ODataAction
    public String myAction(MyEntity entity, String parameter) {
        return "myAction on MyEntity with id " + entity.getId() + " and parameter " + parameter;
    }

    @ODataFunction
    public String myFunction(MyEntity entity) {
        return "myFunction";
    }
}
