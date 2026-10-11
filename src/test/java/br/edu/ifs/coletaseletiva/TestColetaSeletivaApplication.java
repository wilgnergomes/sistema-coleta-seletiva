package br.edu.ifs.coletaseletiva;

import org.springframework.boot.SpringApplication;

public class TestColetaSeletivaApplication {

	public static void main(String[] args) {
		SpringApplication.from(ColetaSeletivaApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
