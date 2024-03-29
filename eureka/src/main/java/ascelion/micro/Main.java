package ascelion.micro;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@SpringBootApplication
@EnableEurekaServer
public class Main {
	static {
		System.setProperty("server.servlet.context-path", "/services");
	}

	public static void main(String[] args) {
		SpringApplication.run(Main.class, args);
	}
}
