package com.eegalepoint.citybus;

import com.eegalepoint.citybus.config.AppJwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AppJwtProperties.class)
public class CityBusApplication {

  public static void main(String[] args) {
    SpringApplication.run(CityBusApplication.class, args);
  }
}
