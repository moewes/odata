package net.moewes.odata.it.navigation;

import lombok.Data;
import net.moewes.quarkus.odata.annotations.EntityKey;
import net.moewes.quarkus.odata.annotations.ODataEntity;

@ODataEntity("OrderItem")
@Data
public class OrderItem {

    @EntityKey
    private String orderId;
    @EntityKey
    private int number;
    private int quantity;
}
