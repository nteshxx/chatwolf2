package com.chatwolf.auth;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(OrderAnnotation.class)
class AuthServiceApplicationTests {

    @Autowired
    MockMvc mockMvc;

    @Test
    @Order(1)
    public void shouldSignup() {
        try {
            mockMvc.perform(post("/api/auth/register")
                            .content(
                                    "{\n\"firstName\": \"Nitesh\",\n\"lastName\": \"Yadav\",\n\"email\": \"nteshxx@gmail.com\",\n\"password\": \"password123\"\n}")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.message").value("Successfully Signed Up"))
                    .andReturn();
        } catch (Exception e) {
            fail("Should not have thrown any exception");
        }
    }

    @Test
    @Order(2)
    public void shouldNotSignup() {
        try {
            mockMvc.perform(post("/api/auth/register")
                            .content(
                                    "{\n\"firstName\": \"Nitesh\",\n\"lastName\": \"Yadav\",\n\"email\": \"nteshxx@gmail.com\",\n\"password\": \"password\"\n}")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Email Id Is Already Taken"))
                    .andReturn();
        } catch (Exception e) {
            fail("Should not have thrown any exception");
        }
    }

    @Test
    @Order(3)
    public void shouldNotLogin() {
        try {
            mockMvc.perform(post("/api/auth/login")
                            .content("{\n\"email\": \"nteshxx@gmail.com\",\n\"password\": \"wrongpassword\"\n}")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Incorrect Username or Password"))
                    .andReturn();
        } catch (Exception e) {
            fail("Should not have thrown any exception");
        }
    }

    @Test
    @Order(4)
    public void shouldLogin() {
        try {
            mockMvc.perform(post("/api/auth/login")
                            .content("{\n\"email\": \"nteshxx@gmail.com\",\n\"password\": \"password123\"\n}")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Successfully Logged In"))
                    .andReturn();
        } catch (Exception e) {
            fail("Should not have thrown any exception");
        }
    }

    @Test
    @Order(5)
    public void shouldGetRefreshToken() {
        try {
            MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                            .content("{\n\"email\": \"nteshxx@gmail.com\",\n\"password\": \"password123\"\n}")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Successfully Logged In"))
                    .andReturn();

            Cookie cookie = loginResult.getResponse().getCookie("refreshToken");
            assertNotNull(cookie);

            mockMvc.perform(get("/api/auth/refresh-token").cookie(cookie))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Successfully Generated New Refresh Token"))
                    .andReturn();

        } catch (Exception e) {
            fail("Should not have thrown any exception");
        }
    }

    @Test
    @Order(6)
    public void shouldGetAccessToken() {
        try {
            MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                            .content("{\n\"email\": \"nteshxx@gmail.com\",\n\"password\": \"password123\"\n}")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Successfully Logged In"))
                    .andReturn();

            Cookie refreshTokenCookie = loginResult.getResponse().getCookie("refreshToken");
            assertNotNull(refreshTokenCookie);

            mockMvc.perform(get("/api/auth/access-token").cookie(refreshTokenCookie))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Successfully Generated New Access Token"))
                    .andReturn();
        } catch (Exception e) {
            fail("Should not have thrown any exception");
        }
    }

    @Test
    @Order(7)
    public void shouldChangePassword() {
        try {
            MvcResult result = mockMvc.perform(post("/api/auth/login")
                            .content("{\n\"email\": \"nteshxx@gmail.com\",\n\"password\": \"password123\"\n}")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Successfully Logged In"))
                    .andReturn();

            String jsonString = result.getResponse().getContentAsString();
            JsonObject data = new Gson().fromJson(jsonString, JsonObject.class);
            String validAccessToken = data.getAsJsonObject("data")
                    .getAsJsonObject("token")
                    .get("accessToken")
                    .getAsString();

            mockMvc.perform(patch("/api/user/change-password")
                            .header("Authorization", "Bearer " + validAccessToken)
                            .content(
                                    "{\n\"oldPassword\": \"wrongpassword\",\n\"newPassword\": \"password456\",\n\"confirmNewPassword\": \"password456\"\n}")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Invalid Old Password"))
                    .andReturn();

            mockMvc.perform(patch("/api/user/change-password")
                            .header("Authorization", "Bearer " + validAccessToken)
                            .content(
                                    "{\n\"oldPassword\": \"password123\",\n\"newPassword\": \"password456\",\n\"confirmNewPassword\": \"password789\"\n}")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("New Password and Confirm New Password do not match"))
                    .andReturn();

            mockMvc.perform(patch("/api/user/change-password")
                            .header("Authorization", "Bearer " + validAccessToken)
                            .content(
                                    "{\n\"oldPassword\": \"password123\",\n\"newPassword\": \"password\",\n\"confirmNewPassword\": \"password\"\n}")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Password Changed Successfully"))
                    .andReturn();

        } catch (Exception e) {
            fail("Should not have thrown any exception");
        }
    }

    @Test
    @Order(8)
    public void shouldGetProfile() {
        try {
            MvcResult result = mockMvc.perform(post("/api/auth/login")
                            .content("{\n\"email\": \"nteshxx@gmail.com\",\n\"password\": \"password\"\n}")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Successfully Logged In"))
                    .andReturn();

            String jsonString = result.getResponse().getContentAsString();
            JsonObject data = new Gson().fromJson(jsonString, JsonObject.class);
            String validAccessToken = data.getAsJsonObject("data")
                    .getAsJsonObject("token")
                    .get("accessToken")
                    .getAsString();

            mockMvc.perform(get("/api/user/profile").header("Authorization", "Bearer " + validAccessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Success"))
                    .andReturn();
        } catch (Exception e) {
            fail("Should not have thrown any exception");
        }
    }

    @Test
    @Order(9)
    public void shouldLogout() {
        try {
            MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                            .content("{\n\"email\": \"nteshxx@gmail.com\",\n\"password\": \"password\"\n}")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Successfully Logged In"))
                    .andReturn();

            Cookie refreshTokenCookie = loginResult.getResponse().getCookie("refreshToken");
            assertNotNull(refreshTokenCookie);

            mockMvc.perform(get("/api/auth/logout").cookie(refreshTokenCookie))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Successfully Logged Out"))
                    .andReturn();
        } catch (Exception e) {
            fail("Should not have thrown any exception");
        }
    }

    @Test
    @Order(10)
    public void shouldLogoutAll() {
        try {
            MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                            .content("{\n\"email\": \"nteshxx@gmail.com\",\n\"password\": \"password\"\n}")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Successfully Logged In"))
                    .andReturn();

            Cookie refreshTokenCookie = loginResult.getResponse().getCookie("refreshToken");
            assertNotNull(refreshTokenCookie);

            mockMvc.perform(get("/api/auth/logout-all").cookie(refreshTokenCookie))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Successfully Logged Out Of All Devices"))
                    .andReturn();
        } catch (Exception e) {
            fail("Should not have thrown any exception");
        }
    }

    @Test
    @Order(11)
    public void shouldCreateUser() {
        try {
            MvcResult result = mockMvc.perform(post("/api/auth/login")
                            .content("{\n\"email\": \"nteshxx@gmail.com\",\n\"password\": \"password\"\n}")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Successfully Logged In"))
                    .andReturn();

            String jsonString = result.getResponse().getContentAsString();
            JsonObject data = new Gson().fromJson(jsonString, JsonObject.class);
            String validAccessToken = data.getAsJsonObject("data")
                    .getAsJsonObject("token")
                    .get("accessToken")
                    .getAsString();

            mockMvc.perform(post("/api/user/create")
                            .header("Authorization", "Bearer " + validAccessToken)
                            .content(
                                    "{\n\"firstName\": \"Test\",\n\"lastName\": \"Society\",\n\"email\": \"testsociety@gmail.com\",\n\"password\": \"passwordtest45\",\n\"role\": \"MANAGER\"\n}")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.message").value("User Created Successfully"))
                    .andReturn();
        } catch (Exception e) {
            fail("Should not have thrown any exception");
        }
    }

    @Test
    @Order(11)
    public void shouldGetExistingUser() {
        try {
            MvcResult result = mockMvc.perform(post("/api/auth/login")
                            .content("{\n\"email\": \"nteshxx@gmail.com\",\n\"password\": \"password\"\n}")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Successfully Logged In"))
                    .andReturn();

            String jsonString = result.getResponse().getContentAsString();
            JsonObject data = new Gson().fromJson(jsonString, JsonObject.class);
            String validAccessToken = data.getAsJsonObject("data")
                    .getAsJsonObject("token")
                    .get("accessToken")
                    .getAsString();

            mockMvc.perform(post("/api/user/create")
                            .header("Authorization", "Bearer " + validAccessToken)
                            .content(
                                    "{\n\"firstName\": \"Test\",\n\"lastName\": \"Society\",\n\"email\": \"testsociety@gmail.com\",\n\"password\": \"passwordtest45\",\n\"role\": \"MANAGER\"\n}")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Account Already Exists"))
                    .andReturn();
        } catch (Exception e) {
            fail("Should not have thrown any exception");
        }
    }

    @Test
    public void acccountDoesNottExist() {
        try {
            mockMvc.perform(post("/api/auth/login")
                            .content("{\n\"email\": \"nteshxx123@gmail.com\",\n\"password\": \"wrongpassword\"\n}")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("Not Found"))
                    .andExpect(jsonPath("$.message").value("Account Doesn't Exist"))
                    .andReturn();
        } catch (Exception e) {
            fail("Should not have thrown any exception");
        }
    }

    @Test
    public void invalidEmail() {
        try {
            mockMvc.perform(post("/api/auth/login")
                            .content("{\n\"email\": \"nteshxx12\",\n\"password\": \"wrongpassword\"\n}")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Invalid email format"))
                    .andReturn();
        } catch (Exception e) {
            fail("Should not have thrown any exception");
        }
    }

    @Test
    public void shouldNotGetRefreshToken() {
        try {
            String invalidRefreshToken =
                    "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJzdWIiOiI2MzU0ZTFhYjQwZDE3NjMwYjRmNWM1MzMiLCJ0b2tlbklkIjoiNjM1NGU0YTA0MGQxNzYzMGI0ZjVjNTM5IiwiaXNzIjoiTXlBcHAiLCJleHAiOjE2NjkwOTk5MzYsImlhdCI6MTY2NjUwNzkzNn0.dKeX0hn61gAYgC1iv29EACspdDfCCBzmPo3F8SMtHyUARNbkTKCC15WQ4BDoB_23R5QizZuNBsSb80AWgASF9w";

            mockMvc.perform(get("/api/auth/refresh-token").cookie(new Cookie("refreshToken", invalidRefreshToken)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Invalid Refresh Token"))
                    .andReturn();
        } catch (Exception e) {
            fail("Should not have thrown any exception");
        }
    }

    @Test
    public void shouldNotGetAccessToken() {
        try {
            mockMvc.perform(get("/api/auth/access-token"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Refresh Token Cookie Not Found"))
                    .andReturn();
        } catch (Exception e) {
            fail("Should not have thrown any exception");
        }
    }

    @Test
    public void shouldNotLogout() {
        try {
            String invalidRefreshToken =
                    "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJzdWIiOiI2MzU0ZTFhYjQwZDE3NjMwYjRmNWM1MzMiLCJ0b2tlbklkIjoiNjM1NGU0YTA0MGQxNzYzMGI0ZjVjNTM5IiwiaXNzIjoiTXlBcHAiLCJleHAiOjE2NjkwOTk5MzYsImlhdCI6MTY2NjUwNzkzNn0.dKeX0hn61gAYgC1iv29EACspdDfCCBzmPo3F8SMtHyUARNbkTKCC15WQ4BDoB_23R5QizZuNBsSb80AWgASF9w";

            Cookie invalidRefreshTokenCookie = new Cookie("refreshToken", invalidRefreshToken);
            invalidRefreshTokenCookie.setHttpOnly(true);
            invalidRefreshTokenCookie.setSecure(false);
            invalidRefreshTokenCookie.setPath("/");
            invalidRefreshTokenCookie.setMaxAge(7 * 24 * 60 * 60);

            mockMvc.perform(get("/api/auth/logout").cookie(invalidRefreshTokenCookie))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Invalid Refresh Token"))
                    .andReturn();
        } catch (Exception e) {
            fail("Should not have thrown any exception");
        }
    }

    @Test
    public void shouldNotLogoutAll() {
        try {
            mockMvc.perform(get("/api/auth/logout-all"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Refresh Token Cookie Not Found"))
                    .andReturn();
        } catch (Exception e) {
            fail("Should not have thrown any exception");
        }
    }

    @Test
    public void shouldNotGetProfile() {
        try {
            String invalidAccessToken =
                    "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJzdWIiOiI2Mzg0YmQ4ZGM4ZjkxNTI5ZWVkOTY1MTEiLCJpc3MiOiJNeUFwcCIsImV4cCI6MTY2OTc4Nzc3MiwiaWF0IjoxNjY5Nzg3NDcyfQ.JeSavorxw46X3bzhY8pmIeIhd8il-lSEb0NAnUb-m07LZDKNhPw5p2QogVLHWSVDbkzNpneoN7KSlQcq5869bQ";

            mockMvc.perform(get("/api/user/profile").header("Authorization", "Bearer " + invalidAccessToken))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("Authentication Required"))
                    .andReturn();
        } catch (Exception e) {
            fail("Should not have thrown any exception");
        }
    }

    @Test
    public void shouldNotCreateUser() {
        try {
            String invalidAccessToken =
                    "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJzdWIiOiI2Mzg0YmQ4ZGM4ZjkxNTI5ZWVkOTY1MTEiLCJpc3MiOiJNeUFwcCIsImV4cCI6MTY2OTc4Nzc3MiwiaWF0IjoxNjY5Nzg3NDcyfQ.JeSavorxw46X3bzhY8pmIeIhd8il-lSEb0NAnUb-m07LZDKNhPw5p2QogVLHWSVDbkzNpneoN7KSlQcq5869bQ";

            mockMvc.perform(post("/api/user/create")
                            .header("Authorization", "Bearer " + invalidAccessToken)
                            .content(
                                    "{\n\"firstName\": \"Test\",\n\"lastName\": \"Society2\",\n\"email\": \"testsociety2@gmail.com\",\n\"password\": \"passwordtest4\",\n\"role\": \"ADMIN\"\n}")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("Authentication Required"))
                    .andReturn();
        } catch (Exception e) {
            fail("Should not have thrown any exception");
        }
    }
}
