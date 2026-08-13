package com.azasyu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.azasyu.global.config.AppProperties;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class AzasYuApplication {

	public static void main(String[] args) {
		SpringApplication.run(AzasYuApplication.class, args);
	}

}
