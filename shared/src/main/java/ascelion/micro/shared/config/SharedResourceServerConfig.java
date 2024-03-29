package ascelion.micro.shared.config;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static ascelion.micro.shared.SecurityConstants.ROLE_ADMIN;
import static ascelion.micro.shared.SecurityConstants.ROLE_USER;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;

@EnableResourceServer
@Configuration
@RequiredArgsConstructor
public class SharedResourceServerConfig extends ResourceServerConfigurerAdapter {

	private final DefaultTokenServices tokenServices;

	@Override
	public void configure(final HttpSecurity http) throws Exception {
		//@formatter:off
		http
			.exceptionHandling()
				.authenticationEntryPoint(this::unauthorized)
				.accessDeniedHandler(this::forbidden)
			.and()
				.authorizeRequests()
				.antMatchers("/error/**").permitAll()
				.antMatchers("/actuator/**").permitAll()
				.antMatchers(HttpMethod.GET, "/**").hasRole(ROLE_USER)
				.antMatchers(HttpMethod.HEAD, "/**").hasRole(ROLE_USER)
				.antMatchers(HttpMethod.OPTIONS, "/**").hasRole(ROLE_USER)
				.antMatchers(HttpMethod.DELETE, "/**").hasRole(ROLE_ADMIN)
				.antMatchers(HttpMethod.PATCH, "/**").hasRole(ROLE_ADMIN)
				.antMatchers(HttpMethod.POST, "/**").hasRole(ROLE_ADMIN)
				.antMatchers(HttpMethod.PUT, "/**").hasRole(ROLE_ADMIN)
				.anyRequest().authenticated()
			.and()
				.csrf().disable()
			;
		//@formatter:on
	}

	@Override
	public void configure(final ResourceServerSecurityConfigurer resources) {
		resources.tokenServices(this.tokenServices);
	}

	private void unauthorized(HttpServletRequest req, HttpServletResponse rsp, AuthenticationException ex) throws IOException {
		rsp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
	}

	private void forbidden(HttpServletRequest req, HttpServletResponse rsp, AccessDeniedException ex) throws IOException {
		rsp.sendError(HttpServletResponse.SC_FORBIDDEN);
	}

}
