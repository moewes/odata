package net.moewes.odata.it.navigation;

import net.moewes.quarkus.odata.EntityCollectionProvider;
import net.moewes.quarkus.odata.annotations.ODataEntitySet;
import net.moewes.quarkus.odata.annotations.ODataNavigationBinding;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@ODataEntitySet(value = "Orders", entityType = "Order")
public class OrderEntitySet implements EntityCollectionProvider<Order> {

    @Inject
    DataBase dataBase;

    @Override
    public List<Order> getCollection() {
        return dataBase.getAllOrders();
    }

    @Override
    public Optional<Order> find(Map<String, String> keys) {
        return dataBase.getOrder(keys.get("Id"));
    }

    @ODataNavigationBinding("OrderItems")
    public List<OrderItem> getOrderItems(Order order) {
        return dataBase.getOrderItems(order);
    }

}
