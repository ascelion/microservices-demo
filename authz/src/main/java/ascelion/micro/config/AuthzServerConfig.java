package ascelion.micro.config;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ascelion.micro.config.SecurityProperties.Details;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.config.annotation.builders.ClientDetailsServiceBuilder;
import org.springframework.security.oauth2.config.annotation.builders.InMemoryClientDetailsServiceBuilder;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.token.TokenEnhancer;
import org.springframework.security.oauth2.provider.token.TokenEnhancerChain;
import org.springframework.security.oauth2.provider.token.TokenStore;

@Configuration
@EnableAuthorizationServer
@EnableConfigurationProperties(SecurityProperties.class)
@RequiredArgsConstructor
public class AuthzServerConfig extends AuthorizationServerConfigurerAdapter {

	private final SecurityProperties securityProperties;
	private final List<TokenEnhancer> tokenEnhancers;
	private final TokenStore tokenStore;
	private final AuthenticationManager authenticationManager;

	@Override
	public void configure(final AuthorizationServerSecurityConfigurer security) throws Exception {
		security
				.accessDeniedHandler(this::forbidden)
				.authenticationEntryPoint(this::unauthorized)
				.tokenKeyAccess("permitAll()")
				.checkTokenAccess("isAuthenticated()");
	}

	@Override
	public void configure(final ClientDetailsServiceConfigurer clients) throws Exception {
		final InMemoryClientDetailsServiceBuilder cdsb = clients.inMemory();

		this.securityProperties.getClients()
				.entrySet()
				.forEach(d -> addClient(cdsb, d));
	}

	@Override
	public void configure(final AuthorizationServerEndpointsConfigurer endpoints) throws Exception {
		final TokenEnhancerChain chain = new TokenEnhancerChain();

		chain.setTokenEnhancers(this.tokenEnhancers);

		endpoints.tokenStore(this.tokenStore)
				.tokenEnhancer(chain)
				.authenticationManager(this.authenticationManager);
	}

	private void addClient(ClientDetailsServiceBuilder<?> service, Map.Entry<String, Details> ent) {
		final Details props = ent.getValue();

		service
				.withClient(ent.getKey())
				.accessTokenValiditySeconds(props.getAccessTokenValidity())
				.additionalInformation(props.getInfo())
				.authorities(props.getAuthorities())
				.authorizedGrantTypes(props.getGrantTypes())
				.autoApprove(props.isAutoApprove())
				.autoApprove(props.getAutoApproveScopes())
				.redirectUris(props.getRedirectUris())
				.refreshTokenValiditySeconds(props.getRefreshTokenValidity())
				.resourceIds(props.getResourceIds())
				.scopes(props.getScopes())
				.secret(props.getSecret());
	}

	private void unauthorized(HttpServletRequest req, HttpServletResponse rsp, AuthenticationException ex) throws IOException, ServletException {
		rsp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
	}

	private void forbidden(HttpServletRequest req, HttpServletResponse rsp, AccessDeniedException ex) throws IOException, ServletException {
		rsp.sendError(HttpServletResponse.SC_FORBIDDEN);
	}
}