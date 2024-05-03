package org.sitenetsoft.sunseterp;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import jakarta.ws.rs.core.Response;

public class SunsetErpResourceUnitTest {

    @Test
    public void testProfileMethod() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        SunsetErpResource resource = new SunsetErpResource();
        Method profileMethod = SunsetErpResource.class.getDeclaredMethod("profile");
        profileMethod.setAccessible(true); // Make the private method accessible
        String profile = (String) profileMethod.invoke(resource);

        // Assert that the default profile is "prod"
        assertEquals("prod", profile);
    }

    @Test
    public void testInfo() {
        // Create an instance of SunsetErpResource
        SunsetErpResource resource = new SunsetErpResource();

        // Call the info() method
        try (Response response = resource.info()) {
            // Assert that the response code is 200 OK
            assertEquals(200, response.getStatus());

            // Add more assertions if needed, such as checking the response body
        }
    }

    @Test
    public void testHealth() {
        // Create an instance of SunsetErpResource
        SunsetErpResource resource = new SunsetErpResource();

        // Call the health() method
        try (Response response = resource.health()) {
            // Assert that the response code is 200 OK
            assertEquals(204, response.getStatus());

            // Add more assertions if needed, such as checking the response headers
        }
    }

    @Test
    public void testVersion() {
        // Create an instance of SunsetErpResource
        SunsetErpResource resource = new SunsetErpResource();

        // Call the version() method
        try (Response response = resource.version()) {
            // Assert that the response code is 200 OK
            assertEquals(200, response.getStatus());

            // Assert that the response body contains the expected version string
            String expectedVersion = "SunsetERP 1.0.0";
            assertEquals(expectedVersion, response.getEntity());

            // Add more assertions if needed
        }
    }
}
