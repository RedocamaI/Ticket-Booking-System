package com.redocmi.train_service;

import org.springframework.boot.SpringApplication;

public class TestTrainServiceApplication {

	public static void main(String[] args) {
		SpringApplication.from(TrainServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
