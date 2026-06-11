package com.odonta.polity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class PolityApplication {
  public static void main(String[] args) {
    SpringApplication.run(PolityApplication.class, args);
  }
}
