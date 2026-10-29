package com.example.studentapi.service;
import com.example.studentapi.repository.PredictionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class PredictionServiceTest {
  @Test void missingRecordReturns404() {
    PredictionRepository repo=mock(PredictionRepository.class);
    when(repo.findById(10L)).thenReturn(java.util.Optional.empty());
    PredictionService service=new PredictionService(repo, RestClient.builder(), new ObjectMapper(),"http://localhost:8000");
    ResponseStatusException ex=assertThrows(ResponseStatusException.class,()->service.get(10L));
    assertEquals(404,ex.getStatusCode().value());
  }
}
