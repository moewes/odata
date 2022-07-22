package net.moewes.odata.it.basic;

import lombok.Data;
import net.moewes.quarkus.odata.annotations.EntityKey;
import net.moewes.quarkus.odata.annotations.ODataEntity;

@ODataEntity("BasicEntity")
@Data
public class MyBasicEntity {

    @EntityKey
    private String id;

    private int number;
    private boolean flag;
    private String text;
}
