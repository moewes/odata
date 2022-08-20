package net.moewes.odata.it.navigation;

import lombok.Data;
import net.moewes.quarkus.odata.annotations.EntityKey;
import net.moewes.quarkus.odata.annotations.ODataEntity;

import java.time.LocalDate;

@ODataEntity("Order")
@Data
public class Order {

    @EntityKey
    private String id;
    private LocalDate orderDate;
    private boolean delivered;
}
