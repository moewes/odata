package net.moewes.odata.it.navigation;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class DataBase {

    private final Set<OrderDatabaseItem> store = new HashSet<>();

    @PostConstruct
    public void init() {

        store.add(OrderDatabaseItem.builder()
                .OrderId("O1")
                .orderDate(LocalDate.of(2022, 1, 1))
                .OrderItemLine(1)
                .productId("P1")
                .productName("Product one")
                .customerId("C1")
                .customerName("Customer One")
                .quantity(5)
                .build());

        store.add(OrderDatabaseItem.builder()
                .OrderId("O1")
                .orderDate(LocalDate.of(2022, 1, 1))
                .OrderItemLine(2)
                .productId("P2")
                .productName("Product two")
                .customerId("C1")
                .customerName("Customer One")
                .quantity(10)
                .build());

        store.add(OrderDatabaseItem.builder()
                .OrderId("O2")
                .orderDate(LocalDate.of(2022, 2, 1))
                .OrderItemLine(1)
                .productId("P1")
                .productName("Product one")
                .customerId("C1")
                .customerName("Customer One")
                .quantity(20)
                .build());

        store.add(OrderDatabaseItem.builder()
                .OrderId("O3")
                .orderDate(LocalDate.of(2022, 3, 1))
                .OrderItemLine(1)
                .productId("P1")
                .productName("Product one")
                .customerId("C2")
                .customerName("Customer Two")
                .quantity(5)
                .build());
    }

    public List<Customer> getAllCustomers() {

        return store.stream().map(item -> {
            Customer result = new Customer();
            result.setId(item.getCustomerId());
            result.setName(item.getCustomerName());
            return result;
        }).distinct().collect(Collectors.toList());
    }

    public List<Product> getAllProducts() {

        return store.stream().map(item -> {
            Product result = new Product();
            result.setId(item.getProductId());
            result.setName(item.getProductName());
            return result;
        }).distinct().collect(Collectors.toList());
    }

    public List<Order> getAllOrders() {

        return store.stream().map(item -> {
            Order result = new Order();
            result.setId(item.getOrderId());
            result.setOrderDate(item.getOrderDate());
            result.setDelivered(item.isDelivered());
            return result;
        }).distinct().collect(Collectors.toList());
    }

    public List<OrderItem> getAllOrderItems() {

        return store.stream().map(item -> {
            OrderItem result = new OrderItem();
            result.setNumber(item.getOrderItemLine());
            result.setOrderId(item.getOrderId());
            result.setQuantity(item.getQuantity());
            return result;
        }).distinct().collect(Collectors.toList());
    }

    public Optional<Order> getOrder(String id) {

        return store.stream()
                .filter(item -> item.getOrderId().equals(id))
                .findFirst()
                .map(orderDatabaseItem -> {
                    Order result = new Order();
                    result.setId(orderDatabaseItem.getOrderId());
                    result.setOrderDate(orderDatabaseItem.getOrderDate());
                    result.setDelivered(orderDatabaseItem.isDelivered());
                    return result;
                });
    }

    public List<OrderItem> getOrderItems(Order order) {

        return store.stream()
                .filter(item -> item.getOrderId().equals(order.getId()))
                .map(orderDatabaseItem -> {
                    OrderItem result = new OrderItem();
                    result.setOrderId(orderDatabaseItem.getOrderId());
                    result.setNumber(orderDatabaseItem.getOrderItemLine());
                    result.setQuantity(orderDatabaseItem.getQuantity());
                    return result;
                }).collect(Collectors.toList());
    }

    public Optional<OrderItem> getOrderItem(String orderId, int number) {
        return store.stream()
                .filter(item -> item.getOrderId().equals(orderId) && item.getOrderItemLine() ==
                        number)
                .findFirst()
                .map(orderDatabaseItem -> {
                    OrderItem result = new OrderItem();
                    result.setOrderId(orderDatabaseItem.getOrderId());
                    result.setNumber(orderDatabaseItem.getOrderItemLine());
                    result.setQuantity(orderDatabaseItem.getQuantity());
                    return result;
                });
    }

    public Product getPorductOfOrderItem(OrderItem orderItem) {
        return store.stream()
                .filter(item -> item.getOrderId()
                        .equals(orderItem.getOrderId()) && item.getOrderItemLine() ==
                        orderItem.getNumber())
                .findFirst()
                .map(orderDatabaseItem -> {
                    Product result = new Product();
                    result.setId(orderDatabaseItem.getProductId());
                    result.setName(orderDatabaseItem.getProductName());
                    return result;
                }).orElseThrow();
    }
}
