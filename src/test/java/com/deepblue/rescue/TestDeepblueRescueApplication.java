package com.deepblue.rescue;

import org.springframework.boot.SpringApplication;

public class TestDeepblueRescueApplication {

	public static void main(String[] args) {
		SpringApplication.from(DeepblueRescueApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
