package dev.ratmir.cloudstorage;

import org.springframework.boot.SpringApplication;

public class TestCloudDataBaseApplication {

	public static void main(String[] args) {
		SpringApplication.from(CloudDataBaseApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
