package net.moewes.odata.it;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class AzureTableStorageResourceTest {

    @Test
    public void testMetadata() {
        given()
                .when().get("/odata/$metadata")
                .then()
                .statusCode(200)
                .body(stringContainsInOrder("<EntityType Name=","MyEntity",">","<EntitySet " +
                        "Name=", "MyService",">"));
    }
}
