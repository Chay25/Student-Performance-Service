package com.example.studentapi.controller;
import com.example.studentapi.config.SecurityConfig;
import com.example.studentapi.dto.*;
import com.example.studentapi.service.PredictionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import java.time.Instant;
import java.util.List;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@WebMvcTest(PredictionController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties="app.api-key=test-secret")
class PredictionControllerTest {
  @Autowired MockMvc mvc;
  @MockBean PredictionService service;
  private static final String BODY="""
    {"studentId":"s-1","attendanceRate":90,"studyHoursPerWeek":12,"previousScore":76,"assignmentsCompleted":20}
    """;
  @Test void rejectsMissingKey() throws Exception {
    mvc.perform(post("/api/predictions").contentType("application/json").content(BODY))
      .andExpect(status().isUnauthorized());
  }
  @Test void rejectsInvalidInput() throws Exception {
    mvc.perform(post("/api/predictions").header("X-API-Key","test-secret")
      .contentType("application/json").content(BODY.replace("90","190")))
      .andExpect(status().isBadRequest());
  }
  @Test void createsPrediction() throws Exception {
    when(service.create(any())).thenReturn(new PredictionResponse(1L,"s-1",78.1,
      List.of(new Factor("previousScore",3.4)),"synthetic-demo-v1", Instant.parse("2026-01-01T00:00:00Z")));
    mvc.perform(post("/api/predictions").header("X-API-Key","test-secret")
      .contentType("application/json").content(BODY))
      .andExpect(status().isCreated()).andExpect(jsonPath("$.studentId").value("s-1"));
  }
}
