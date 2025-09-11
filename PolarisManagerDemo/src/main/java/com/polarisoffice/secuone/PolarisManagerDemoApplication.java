package com.polarisoffice.secuone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EnableJpaAuditing   // ✅ 추가
@SpringBootApplication
@EntityScan(basePackages = "com.polarisoffice.secuone.domain")
@EnableJpaRepositories(basePackages = "com.polarisoffice.secuone.repository")
public class PolarisManagerDemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(PolarisManagerDemoApplication.class, args);
	}

}
