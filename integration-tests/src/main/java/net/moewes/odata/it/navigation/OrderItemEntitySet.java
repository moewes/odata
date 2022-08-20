package net.moewes.odata.it.navigation;

import net.moewes.quarkus.odata.EntityCollectionProvider;
import net.moewes.quarkus.odata.annotations.ODataEntitySet;

import javax.inject.Inject;
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
        return Optional.empty();
    }
}
