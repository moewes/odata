package net.moewes.odata.it;

import lombok.Data;
import net.moewes.quarkus.odata.annotations.EntityKey;
import net.moewes.quarkus.odata.annotations.ODataEntity;

@ODataEntity("MyEntity")
@Data
public class MySecondEntity {

    @EntityKey
    private String id;

    private String name;
}
