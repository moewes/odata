package net.moewes.odata.it.navigation;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.path.json.JsonPath;
import io.restassured.path.json.config.JsonPathConfig;
import io.restassured.response.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.stringContainsInOrder;

@QuarkusTest
public class IntegrationTest {

    @Test
    public void testMetadata() { // FIXME
        given()
                .when().get("/odata/$metadata")
                .then()
                .statusCode(200)
                // Content for basic entity with supported types
                .body(stringContainsInOrder("EntityType Name=", "BasicEntity", ">"))
                .body(stringContainsInOrder("<Property Name=", "Number", "Type=", "Edm.Int32"))
                .body(stringContainsInOrder("<Property Name=", "Flag", "Type=", "Edm.Boolean"))
                .body(stringContainsInOrder("<Property Name=", "Text", "Type=", "Edm.String"))
                // Content for EntitySet for BasicEntity;
                .body(stringContainsInOrder("<EntitySet Name=", "BasicSet", "EntityType=",
                        "Quarkus.OData.BasicEntity"));
    }

    @Test
    public void getAllCustomers() {
        Response response = given()
                .when()
                .get("/odata/Customers");

        response
                .then()
                .statusCode(200);

        JsonPath jsonPath = response.jsonPath().using(new JsonPathConfig());

        List<Object> list = jsonPath.getList("value");
        Assertions.assertEquals(2, list.size());
    }

    @Test
    public void getCustomer() { // FIXME

    }

    @Test
    public void getAllProducts() {
        Response response = given()
                .when()
                .get("/odata/Products");

        response
                .then()
                .statusCode(200);

        JsonPath jsonPath = response.jsonPath().using(new JsonPathConfig());

        List<Object> list = jsonPath.getList("value");
        Assertions.assertEquals(2, list.size());
    }

    @Test
    public void getProduct() { // FIXME

    }

    @Test
    public void getAllOrders() {
        Response response = given()
                .when()
                .get("/odata/Orders");

        response
                .then()
                .statusCode(200);

        JsonPath jsonPath = response.jsonPath().using(new JsonPathConfig());

        List<Object> list = jsonPath.getList("value");
        Assertions.assertEquals(3, list.size());
    }

    @Test
    public void getOrder() {

        Response response = given()
                .when()
                .get("/odata/Orders('O1')");

        response
                .then()
                .statusCode(200);
    }

    @Test
    public void getAllOrderItems() {
        Response response = given()
                .when()
                .get("/odata/OrderItems");

        response
                .then()
                .statusCode(200);

        JsonPath jsonPath = response.jsonPath().using(new JsonPathConfig());

        List<Object> list = jsonPath.getList("value");
        Assertions.assertEquals(4, list.size());
    }

    @Test
    public void getAllOrderOrderItems() {
        Response response = given()
                .when()
                .get("/odata/Orders('O1')/OrderItems");

        response
                .then()
                .statusCode(200);

        JsonPath jsonPath = response.jsonPath().using(new JsonPathConfig());

        List<Object> list = jsonPath.getList("value");
        Assertions.assertEquals(2, list.size());
    }

}
