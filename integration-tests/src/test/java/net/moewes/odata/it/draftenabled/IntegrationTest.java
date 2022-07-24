package net.moewes.odata.it.draftenabled;

import io.quarkus.test.junit.QuarkusTest;
import org.json.simple.JSONObject;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.stringContainsInOrder;

@QuarkusTest
public class IntegrationTest {

    @Test
    public void testMetadata() {
        given()
                .when().get("/odata/$metadata")
                .then()
                .statusCode(200)
                .body(stringContainsInOrder("<EntityType Name=", "MyEntity", ">", "<EntitySet " +
                        "Name=", "MyService", ">"));
        // Content for basic entity with supported types
        // .body(stringContainsInOrder("<Property Name=", "Number", "Type=", "Edm.Int32"))
        // .body(stringContainsInOrder("<Property Name=", "Flag", "Type=", "Edm.Boolean"))
        // .body(stringContainsInOrder("<Property Name=", "Text", "Type=", "Edm.String"))
        // Content for EntitySet for BasicEntity;

    }

    //@Test() // FIXME
    public void testAction() {

        JSONObject requestParams = new JSONObject();
        requestParams.put("parameter", "abc");

        given()
                .when()
                .header("Content-Type", "application/json")
                .body(requestParams.toJSONString())
                .post("/odata/MyService(Id='123',IsActiveEntity=true)/Quarkus.OData.myAction")
                .then()
                .statusCode(200)
                .body(stringContainsInOrder("value", "myAction on MyEntity with id 123 and " +
                        "parameter abc"));
    }

    @Test
    public void testGetAll() {
        given()
                .when()
                .get("/odata/MyService")
                .then()
                .statusCode(200);
        // TODO make it better
    }

    @Test
    public void testFind() {
        given()
                .when()
                .get("/odata/MyService(Id='123',IsActiveEntity=true)")
                .then()
                .statusCode(200);
        // TODO make it better
    }

    //@Test // FIXME
    public void testFindNavigation() {
        given()
                .when()
                .get("/odata/MyService(Id='123',IsActiveEntity=true)/MyService")// FIXME
                .then()
                .statusCode(200);
        // TODO make it better
    }

}
