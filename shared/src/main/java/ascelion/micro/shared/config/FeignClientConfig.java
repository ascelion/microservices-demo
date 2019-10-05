package ascelion.micro.shared.config;

import javax.servlet.http.HttpServletRequest;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

import feign.Client;
import feign.Logger;
import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(Client.class)
public class FeignClientConfig {

	@Autowired
	private HttpServletRequest request;

	@Bean
	Logger.Level feignLoggerLevel() {
		return Logger.Level.FULL;
	}

	@Bean
	RequestInterceptor feignAuthzHeader() {
		return template -> {
			template.header(AUTHORIZATION, this.request.getHeader(AUTHORIZATION));
		};
	}
}