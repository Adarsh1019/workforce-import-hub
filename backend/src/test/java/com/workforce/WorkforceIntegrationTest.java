package com.workforce;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** End-to-end against a real PostgreSQL (Docker required): auth, roles, and a CSV import. */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@TestPropertySource(properties = "app.import.upload-dir=target/test-uploads")
class WorkforceIntegrationTest {
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    private String token(String user, String pass) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType("application/json")
                        .content("{\"username\":\"" + user + "\",\"password\":\"" + pass + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return "Bearer " + mapper.readTree(body).get("token").asText();
    }

    @Test
    void rejectsWrongPassword() throws Exception {
        mvc.perform(post("/api/auth/login").contentType("application/json")
                .content("{\"username\":\"admin\",\"password\":\"nope\"}")).andExpect(status().isUnauthorized());
    }

    @Test
    void requiresAuthentication() throws Exception {
        mvc.perform(get("/api/employees")).andExpect(status().isUnauthorized());
    }

    @Test
    void hrCannotDeleteDepartments() throws Exception {
        mvc.perform(delete("/api/departments/1").header("Authorization", token("hr", "12345")))
                .andExpect(status().isForbidden());
    }

    @Test
    void importsValidRowsAndRejectsBadOnes() throws Exception {
        String auth = token("admin", "12345");
        String csv = """
                name,email,department,role,salary,joiningDate
                Test One,test.one@example.com,Sales,Area Sales Manager,70000,2022-01-10
                Test Two,test.two@example.com,Sales,Clerk,40000,2021-05-02
                Dup Two,TEST.TWO@example.com,Sales,Clerk,40000,2021-05-02
                Bad Email,not-an-email,Sales,Clerk,40000,2021-05-02
                Ghost,ghost@example.com,Space Program,Astronaut,90000,2023-07-01
                """;
        var file = new MockMultipartFile("file", "test.csv", "text/csv", csv.getBytes());
        String res = mvc.perform(multipart("/api/imports").file(file).header("Authorization", auth))
                .andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString();
        long id = mapper.readTree(res).get("id").asLong();

        JsonNode job = null;
        for (int i = 0; i < 60; i++) {
            job = mapper.readTree(mvc.perform(get("/api/imports/" + id).header("Authorization", auth))
                    .andReturn().getResponse().getContentAsString());
            if ("COMPLETED".equals(job.get("status").asText()) || "FAILED".equals(job.get("status").asText())) break;
            Thread.sleep(250);
        }
        assertThat(job.get("status").asText()).isEqualTo("COMPLETED");
        assertThat(job.get("totalRecords").asInt()).isEqualTo(5);
        assertThat(job.get("successCount").asInt()).isEqualTo(2);
        assertThat(job.get("failureCount").asInt()).isEqualTo(3);

        String errors = mvc.perform(get("/api/imports/" + id + "/errors.csv").header("Authorization", auth))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(errors).contains("duplicate email in file").contains("does not exist").contains("is invalid");
    }
}
