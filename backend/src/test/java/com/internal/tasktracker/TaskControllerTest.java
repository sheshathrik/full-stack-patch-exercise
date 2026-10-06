package com.internal.tasktracker;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Status filter should only return tasks with that exact status")
    void testStatusFilterExcludesOtherStatuses() throws Exception {
        mockMvc.perform(get("/api/tasks")
                .param("status", "DONE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.items[*].status", everyItem(is("DONE"))));
    }

    @Test
    @DisplayName("Archived tasks should never be returned even when matching description")
    void testArchivedTasksAreExcluded() throws Exception {
        // 'deprecated' is in the description of an archived task (Legacy API cleanup)
        mockMvc.perform(get("/api/tasks")
                .param("q", "deprecated"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].archived", everyItem(is(false))))
                .andExpect(jsonPath("$.total", is(0)));
    }

    @Test
    @DisplayName("Invalid status returns HTTP 400 Bad Request with descriptive error")
    void testInvalidStatusReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/tasks")
                .param("status", "INVALID_STATUS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("INVALID_STATUS")))
                .andExpect(jsonPath("$.message", containsString("Allowed values")));
    }

    @Test
    @DisplayName("Page bounds are sanitized so page=0 does not cause 500 error")
    void testSanitizePageBounds() throws Exception {
        mockMvc.perform(get("/api/tasks")
                .param("page", "0")
                .param("pageSize", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page", is(1)))
                .andExpect(jsonPath("$.pageSize", is(5)))
                .andExpect(jsonPath("$.items", hasSize(5)));
    }

    @Test
    @DisplayName("Search executes promptly without artificial delay")
    void testSearchLatencyWithoutArtificialDelay() throws Exception {
        long startTime = System.currentTimeMillis();
        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total", greaterThan(0)));
        long duration = System.currentTimeMillis() - startTime;
        // Verify it runs in well under the previously hardcoded 1000ms delay
        assert(duration < 500);
    }
}

