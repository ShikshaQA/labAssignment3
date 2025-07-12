package com.rest.test;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import org.testng.annotations.Test;
import static org.hamcrest.Matchers.*;
import java.util.List;

import static io.restassured.RestAssured.given;

@Test
public class MainTest {

    RequestSpecification requestSpecification;
    Response response;
    ValidatableResponse validatableResponse;
    @Test
    public void verifyStatusCode() {

        // Base URL of the API
        RestAssured.baseURI = "http://localhost:8085/books";

        // Username and password for Basic Authentication
        String username = "user"; // Replace with the correct username
        String password = "password"; // Replace with the correct password

        // Create the request specification
        RequestSpecification requestSpecification = given()
                .auth().preemptive().basic(username, password) // Use preemptive basic auth
                .log().all(); // Log all request details (headers, body, etc.)

        // Send GET request and get the response
        Response response = requestSpecification.get();

        // Print the response details for debugging
        System.out.println("Response Status Code: " + response.getStatusCode());
        System.out.println("Response Body: " + response.prettyPrint());
        System.out.println("Response Headers: " + response.getHeaders());

        // Perform validation on the response
        ValidatableResponse validatableResponse = response.then();

        /* Validate status code */
        validatableResponse.statusCode(200);

        // Validate status line
        validatableResponse.statusLine("HTTP/1.1 200 ");
    }
    @Test
    public void testGetBooks() {
        Response response = given()
                                .auth().basic("user", "password")
                                .contentType("application/json")
                            .when()
                                .get("http://localhost:8085/books")
                            .then()
                                .statusCode(200)
                                .extract().response();

       
        // Optionally, validate the first book's details
        response.then().body("[0].name", equalTo("A Guide to the Bodhisattva Way of Life"))
                .body("[0].author", equalTo("Santideva"));
    }

    @Test
    public void testCreateBook() {
        String requestBody = "{\n" +
                "    \"name\": \"A to the Bodhisattva Way of Life\",\n" +
                "    \"author\": \"Santideva\",\n" +
                "    \"price\": 15.41\n" +
                "}";

        Response response = given()
                                .auth().basic("admin", "password")
                                .contentType("application/json")
                                .body(requestBody)
                            .when()
                                .post("http://localhost:8085/books")
                            .then()
                                .statusCode(201)
                                .extract().response();

        // Validate the response body
        response.then().body("name", equalTo("A to the Bodhisattva Way of Life"))
                .body("author", equalTo("Santideva"))
                .body("price", equalTo(15.41f));
    }
    @Test
    public void testGetBookById() {
        int bookId =2;

        Response response = given()
                                .auth().basic("admin", "password")
                                .contentType("application/json")
                            .when()
                                .get("http://localhost:8085/books/" + bookId)
                            .then()
                                .statusCode(200)
                                .extract().response();

        // Print for debugging
        System.out.println("Response: " + response.getBody().asString());
        // Validate the book details
        response.then().body("id", equalTo(bookId))
                .body("name", equalTo("The Life-Changing Magic of Tidying Up"))
                .body("author", equalTo("Marie Kondo"))
                .body("price", equalTo(9.69f));
    }
    
    @Test
    public void testUpdateBook() {
        int bookId = 3;

        String updatedRequestBody = "{\n" +
                "    \"id\": 2,\n" +
                "    \"name\": \"The Life-Changing Magic of Tidying Up\",\n" + // Also fixed typo: "he Life-Changing..."
                "    \"author\": \"Marie Kondo\",\n" +
                "    \"price\": 9.69\n" +
                "}";

        Response response = given()
                                .auth().basic("admin", "password")
                                .contentType("application/json")
                                .body(updatedRequestBody)
                            .when()
                                .put("http://localhost:8085/books/" + bookId)
                            .then()
                                .statusCode(200)
                                .extract().response();

        // Validate the updated book details
        response.then().body("price", equalTo(9.69f));
        response.then().body("name", equalTo("The Life-Changing Magic of Tidying Up"));
    }

    @Test
    public void testDeleteBookById() {
        int bookIdToDelete = 5;
       

        // Optional: Create the book first to ensure it exists
        String requestBody = "{\n" +
                "    \"id\": 5,\n" +
                "    \"name\": \"A to the Bodhisattva Way of Life\",\n" +
                "    \"author\": \"Santideva\",\n" +
                "    \"price\": 15.41\n" +
                "}";
       
        given()
            .auth().basic("admin", "password")
            .contentType("application/json")
            .body(requestBody)
        .when()
            .post("http://localhost:8085/books")
        .then()
            .statusCode(anyOf(equalTo(201), equalTo(200))); // Accept either

        // Now delete
        given()
            .auth().basic("admin", "password")
        .when()
            .delete("http://localhost:8085/books/" + bookIdToDelete)
        .then()
            .statusCode(anyOf(equalTo(200), equalTo(204))); // Accept either 200 or 204

        // Confirm it's deleted
        given()
            .auth().basic("admin", "password")
        .when()
            .get("http://localhost:8085/books/" + bookIdToDelete)
        .then()
            .statusCode(404);
    }


    @Test
    public void testCreateBookWithMissingFields() {
        String invalidRequest = "{ \"name\": \"Test Book\" }"; // Missing author & price

        given()
            .auth().basic("admin", "password")
            .contentType("application/json")
            .body(invalidRequest)
        .when()
            .post("http://localhost:8085/books")
        .then()
            .statusCode(400); // Expecting Bad Request or custom error
    }

    @Test
    public void testUnauthorizedAccess() {
        given()
            .auth().basic("wrongUser", "wrongPassword")
            .when()
            .get("http://localhost:8085/books")
            .then()
            .statusCode(401); // Unauthorized
    }
    @Test
    public void testBookPricesGreaterThanZero() {
        Response response = given()
            .auth().basic("user", "password")
            .when()
            .get("http://localhost:8085/books")
            .then()
            .statusCode(200)
            .extract().response();

        // Loop through all returned prices and assert each is > 0
        List<Float> prices = response.jsonPath().getList("price", Float.class);
        for (Float price : prices) {
            assert price > 0 : "Price is not greater than zero: " + price;
        }
    }
    
    @Test
    public void testBookListIsNotEmpty() {
        given()
            .auth().basic("user", "password")
        .when()
            .get("http://localhost:8085/books")
        .then()
            .statusCode(200)
            .body("size()", greaterThan(0));
    }
}