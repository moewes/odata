package net.moewes.odata.it;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import org.json.simple.JSONObject;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class IntegrationTest {

    @Test
    public void testMetadata() {
        given()
                .when().get("/odata/$metadata")
                .then()
                .statusCode(200)
                .body(stringContainsInOrder("<EntityType Name=","MyEntity",">","<EntitySet " +
                        "Name=", "MyService",">"));
    }



    @Test
    public void testAction() {

        JSONObject requestParams = new JSONObject();
        requestParams.put("parameter","abc");

        given()
                .when()
                .header("Content-Type", "application/json")
                .body(requestParams.toJSONString())
                .post("/odata/MyService('123')/Quarkus.OData.myAction")
                .then()
                .statusCode(200)
                .body(stringContainsInOrder("value","myAction on MyEntity with id 123 and " +
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
                .get("/odata/MyService('123')")
                .then()
                .statusCode(200);
        // TODO make it better
    }

    @Test
    public void testFindNavigation() {
        given()
                .when()
                .get("/odata/MyService('123')/MyService")// FIXME
                .then()
                .statusCode(200);
        // TODO make it better
    }

}
