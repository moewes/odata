package net.moewes.azure.table.storage.it;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class AzureTableStorageResourceTest {

    @Test
    public void testHelloEndpoint() {
        given()
                .when().get("/azure-table-storage")
                .then()
                .statusCode(200)
                .body(is("Hello azure-table-storage"));
    }
}
