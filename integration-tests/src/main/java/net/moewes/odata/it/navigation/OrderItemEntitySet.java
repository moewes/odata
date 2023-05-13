package net.moewes.odata.it.navigation;

import jakarta.inject.Inject;
import net.moewes.quarkus.odata.EntityCollectionProvider;
import net.moewes.quarkus.odata.annotations.ODataEntitySet;
import net.moewes.quarkus.odata.annotations.ODataNavigationBinding;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@ODataEntitySet(value = "OrderItems", entityType = "OrderItem")
public class OrderItemEntitySet implements EntityCollectionProvider<OrderItem> {

    @Inject
    DataBase dataBase;

    @Override
    public List<OrderItem> getCollection() {
        return dataBase.getAllOrderItems();
    }

    @Override
    public Optional<OrderItem> find(Map<String, String> keys) {
        String orderId = keys.get("OrderId");
        int number = Integer.parseInt(keys.get("Number"));
        return dataBase.getOrderItem(orderId, number);
    }

    @ODataNavigationBinding("Product")
    public Product getProduct(OrderItem orderItem) {
        return dataBase.getPorductOfOrderItem(orderItem);
    }
}
