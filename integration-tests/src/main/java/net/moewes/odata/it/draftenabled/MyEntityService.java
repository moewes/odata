package net.moewes.odata.it.draftenabled;

import net.moewes.odata.it.MySecondEntity;
import net.moewes.quarkus.odata.EntityCollectionProvider;
import net.moewes.quarkus.odata.EntityProvider;
import net.moewes.quarkus.odata.annotations.ODataAction;
import net.moewes.quarkus.odata.annotations.ODataEntitySet;
import net.moewes.quarkus.odata.annotations.ODataFunction;
import net.moewes.quarkus.odata.annotations.ODataNavigationBinding;
import org.apache.olingo.server.api.ODataApplicationException;

import java.util.*;
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
        entity2.setHasActiveEntity(false);
        result.add(entity2);
        return result;
    }

    @Override
    public Optional<MyEntity> find(Map<String, String> keys) {

        MyEntity entity = new MyEntity();
        entity.setId(keys.get("Id"));
        entity.setName("Entity with id " + entity.getId());
        entity.setIsActiveEntity(true);

        return Optional.of(entity);
    }

    @Override
    public MyEntity create(Object entity) throws ODataApplicationException {
        MyEntity result = new MyEntity();
        result.setId("N");
        result.setName("New");
        result.setHasActiveEntity(true);
        result.setHasDraftEntity(true);
        return result;
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

        DraftAdministrativeData draftAdministrativeData = new DraftAdministrativeData();
        draftAdministrativeData.setDraftUUID(UUID.randomUUID());
        draftAdministrativeData.setCreatedByUser("C-User");
        draftAdministrativeData.setLastChangeByUser("l_user");
        draftAdministrativeData.setInProcessByUser("P-User");
        return draftAdministrativeData;
        //return null;
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

        Logger.getLogger("X").info("draftPrepare " + sideEffectQualifier);
        //in.setId("AC");
        in.setName("Draft prepare");
        in.setHasActiveEntity(true);
        in.setIsActiveEntity(false);

        return in;
    }

    @ODataAction
    MyEntity draftActivate(MyEntity in) {
        Logger.getLogger("X").info("draftActivate ");
        //in.setId("AC");
        in.setName("Draft activted");
        in.setHasActiveEntity(false);
        in.setIsActiveEntity(true);
        return in;
    }

    @ODataAction
    MyEntity draftEdit(MyEntity in) {//}, boolean PreserveChanges) {
        Logger.getLogger("X").info("draftEdit ");
        // in.setId("AC");
        in.setName("Draft edit");
        in.setHasActiveEntity(true);
        in.setIsActiveEntity(false);
        return in;
    }
}
