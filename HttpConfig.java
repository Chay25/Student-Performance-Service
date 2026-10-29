package com.example.studentapi.config;
import org.springframework.context.annotation.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
@Configuration
public class HttpConfig {
  @Bean RestClient.Builder restClientBuilder() {
    SimpleClientHttpRequestFactory factory=new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(3000); factory.setReadTimeout(10000);
    return RestClient.builder().requestFactory(factory);
  }
}
