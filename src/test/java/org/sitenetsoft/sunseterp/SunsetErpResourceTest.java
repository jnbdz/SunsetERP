package org.sitenetsoft.sunseterp;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

@QuarkusTest
public class SunsetErpResourceTest {

    @Test
    public void testInfoEndpoint() {
        given()
                .when()
                .get("/sunseterp/info") // Path should include the base path '/sunseterp'
                .then()
                .statusCode(200)
                .body(notNullValue()); // Assert that response body is not null
    }

    @Test
    public void testHealthEndpoint() {
        given()
                .when()
                .head("/sunseterp/health") // Using head method for the health endpoint
                .then()
                .statusCode(204)
                .header("Health-Status", "OK"); // Assert the custom header is present and has value "OK"
    }

    @Test
    public void testVersionEndpoint() {
        given()
                .when()
                .get("/sunseterp/version")
                .then()
                .statusCode(200)
                .body(is("SunsetERP 1.0.0")); // Assert the response body is "SunsetERP 1.0"
    }

}
