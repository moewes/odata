package net.moewes.odata.it.draftenabled;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import net.moewes.quarkus.odata.annotations.EntityKey;
import net.moewes.quarkus.odata.annotations.ODataEntity;

@ODataEntity("MyEntity")
@Data
public class MyEntity {

    @EntityKey
    private String id;
    @EntityKey
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private boolean isActiveEntity;

    private String name;

    private boolean hasActiveEntity;
    private boolean hasDraftEntity;

    public boolean getIsActiveEntity() {
        return isActiveEntity;
    }

    public void setIsActiveEntity(boolean activeEntity) {
        isActiveEntity = activeEntity;
    }
}
