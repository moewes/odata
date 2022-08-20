package net.moewes.odata.it.navigation;

import lombok.Data;
import net.moewes.quarkus.odata.annotations.EntityKey;
import net.moewes.quarkus.odata.annotations.ODataEntity;

@ODataEntity("Product")
@Data
public class Product {

    @EntityKey
    private String id;
    private String name;
}
