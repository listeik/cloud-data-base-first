package dev.ratmir.cloudstorage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
public class CloudDataBaseApplication {

	public static void main(String[] args) {
		SpringApplication.run(CloudDataBaseApplication.class, args);
	}

}
