package net.moewes.odata.it.navigation;

import jakarta.inject.Inject;
import net.moewes.quarkus.odata.EntityCollectionProvider;
import net.moewes.quarkus.odata.annotations.ODataEntitySet;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@ODataEntitySet(value = "Products", entityType = "Product")
public class ProductEntitySet implements EntityCollectionProvider<Product> {

    @Inject
    DataBase dataBase;

    @Override
    public List<Product> getCollection() {
        return dataBase.getAllProducts();
    }

    @Override
    public Optional<Product> find(Map<String, String> keys) {
        return Optional.empty();
    }
}
