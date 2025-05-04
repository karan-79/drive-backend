package com.project.mydrive;

import org.springframework.boot.SpringApplication;

public class TestMyDriveApplication {

	public static void main(String[] args) {
		SpringApplication.from(MyDriveApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
