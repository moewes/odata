package net.moewes.odata.it.navigation;

import lombok.Data;
import net.moewes.quarkus.odata.annotations.EntityKey;
import net.moewes.quarkus.odata.annotations.ODataEntity;

@ODataEntity("Customer")
@Data
public class Customer {

    @EntityKey
    private String id;
    private String Name;
}
