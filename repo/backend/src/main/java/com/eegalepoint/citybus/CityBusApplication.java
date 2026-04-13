package com.eegalepoint.citybus;

import com.eegalepoint.citybus.config.AppJwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties(AppJwtProperties.class)
@EnableScheduling
public class CityBusApplication {

  public static void main(String[] args) {
    SpringApplication.run(CityBusApplication.class, args);
  }
}
