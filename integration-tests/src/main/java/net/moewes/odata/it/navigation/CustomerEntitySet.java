package net.moewes.odata.it.navigation;

import net.moewes.quarkus.odata.EntityCollectionProvider;
import net.moewes.quarkus.odata.annotations.ODataEntitySet;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@ODataEntitySet(value = "Customers", entityType = "Customer")
public class CustomerEntitySet implements EntityCollectionProvider<Customer> {

    @Inject
    DataBase dataBase;

    @Override
    public List<Customer> getCollection() {
        return dataBase.getAllCustomers();
    }

    @Override
    public Optional<Customer> find(Map<String, String> keys) {
        return Optional.empty();
    }
}
