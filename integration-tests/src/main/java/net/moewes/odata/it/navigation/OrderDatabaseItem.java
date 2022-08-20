package net.moewes.odata.it.navigation;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;

@Value
@Builder
public class OrderDatabaseItem {

    private String OrderId;
    private int OrderItemLine;
    private int quantity;
    private String productId;
    private String customerId;
    private String customerName;
    private String productName;
    private boolean delivered;
    private LocalDate orderDate;

}
