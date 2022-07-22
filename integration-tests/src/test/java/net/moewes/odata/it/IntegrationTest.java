package net.moewes.odata.it;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.path.json.JsonPath;
import io.restassured.path.json.config.JsonPathConfig;
import io.restassured.response.Response;
import org.json.simple.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

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
                        "Name=", "MyService", ">"))
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
    public void getAllBasicEntities() {
        Response response = given()
                .when()
                .get("/odata/BasicSet");

        response
                .then()
                .statusCode(200);

        JsonPath jsonPath = response.jsonPath().using(new JsonPathConfig());

        List<Object> list = jsonPath.getList("value");
        Assertions.assertEquals(list.size(), 2);
    }

    @Test
    public void getBasicEntityDefaultValues() {
        Response response = given()
                .when()
                .get("/odata/BasicSet('EwDV')");

        response
                .then()
                .statusCode(200);

        JsonPath jsonPath = response.jsonPath();

        Assertions.assertEquals("EwDV", jsonPath.get("Id"));
        Assertions.assertEquals(Boolean.FALSE, jsonPath.get("Flag"));
        Assertions.assertEquals(Integer.valueOf(0), jsonPath.get("Number"));
        Assertions.assertEquals(null, (String) jsonPath.get("Text"));
    }

    @Test
    public void getBasicEntity() {
        Response response = given()
                .when()
                .get("/odata/BasicSet('E1')");

        response
                .then()
                .statusCode(200);

        JsonPath jsonPath = response.jsonPath();

        Assertions.assertEquals("E1", jsonPath.get("Id"));
        Assertions.assertEquals(Boolean.TRUE, jsonPath.get("Flag"));
        Assertions.assertEquals(Integer.valueOf(10), jsonPath.get("Number"));
        Assertions.assertEquals("FooBar", jsonPath.get("Text"));
    }

    // @Test // FIXME not found behavior
    public void getBasicNotFound() {
        Response response = given()
                .when()
                .get("/odata/BasicSet('notFound')");

        response
                .then()
                .statusCode(404);
    }

    @Test
    public void addBasicEntity() {

        JSONObject requestParams = new JSONObject();
        requestParams.put("Id", "N1");
        requestParams.put("Flag", true);
        requestParams.put("Number", 5);
        requestParams.put("Text", "New Element");

        given()
                .when()
                .header("Content-Type", "application/json")
                .body(requestParams.toJSONString())
                .post("/odata/BasicSet")
                .then()
                .statusCode(201);

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
