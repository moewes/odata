package net.moewes.odata.it.draftenabled;

import net.moewes.odata.it.MySecondEntity;
import net.moewes.quarkus.odata.EntityCollectionProvider;
import net.moewes.quarkus.odata.EntityProvider;
import net.moewes.quarkus.odata.annotations.ODataAction;
import net.moewes.quarkus.odata.annotations.ODataEntitySet;
import net.moewes.quarkus.odata.annotations.ODataFunction;
import net.moewes.quarkus.odata.annotations.ODataNavigationBinding;
import org.apache.olingo.server.api.ODataApplicationException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

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

    @ODataNavigationBinding("SiblingEntity") // FIXM
    public MyEntity getSiblingEntity(MyEntity entity) {

        entity.setId("S");
        entity.setName("Active Sibling");
        return entity;
    }

    @ODataNavigationBinding("DraftAdministrativeData") // FIXME
    public DraftAdministrativeData getDraftAdministrativeData(MyEntity entity) {
        return new DraftAdministrativeData();
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

    @ODataAction()
    public MyEntity draftPrepare(MyEntity in, String sideEffectQualifier) {

        Logger.getLogger("draftPrepare " + sideEffectQualifier);
        return in;
    }

    @ODataAction
    MyEntity draftActivate(MyEntity in) {
        Logger.getLogger("draftActivate ");
        in.setId("AC");
        in.setName("Draft");
        in.setHasActiveEntity(true);
        in.setIsActiveEntity(false);
        return in;
    }

    @ODataAction
    MyEntity draftEdit(MyEntity in, boolean preserveChanges) {
        Logger.getLogger("draftEdit " + preserveChanges);
        return in;
    }
}
