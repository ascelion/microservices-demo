package ascelion.micro.customers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.sql.DataSource;

import ascelion.micro.shared.utils.BeanToBeanMapper;
import ascelion.micro.tests.TestsResourceServerConfig;
import ascelion.micro.tests.WithRoleAdmin;

import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.util.FieldUtils.setProtectedFieldValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@RunWith(SpringRunner.class)
@WebMvcTest({
		CustomersController.class,
})
@Import({
		TestsResourceServerConfig.class,
})
@ActiveProfiles("test")
public class CustomersControllerTest {

	@Autowired
	private MockMvc mvc;
	@Autowired
	private ObjectMapper om;
	@MockBean
	private CustomersRepository repo;
	@MockBean
	private DataSource ds;

	private final Map<UUID, Customer> customers = new HashMap<>();
	private final BeanToBeanMapper bbm = new BeanToBeanMapper();

	@Before
	public void setUp() {
		when(this.repo.findAll())
				.then(ivc -> {
					return new ArrayList<>(this.customers.values());
				});
		when(this.repo.findById(any()))
				.then(ivc -> {
					return ofNullable(this.customers.get(ivc.getArgument(0)));
				});
		when(this.repo.save(any()))
				.then(ivc -> {
					final Customer o = ivc.getArgument(0);

					if (o.getId() == null) {
						final Customer newO = this.bbm.copy(o, Customer.class, false);

						setProtectedFieldValue("id", newO, randomUUID());

						this.customers.put(newO.getId(), newO);

						return newO;
					} else {
						this.customers.put(o.getId(), o);

						return o;
					}
				});
	}

	@Test
	@WithRoleAdmin
	public void createEntity() throws Exception {
		final CustomerRequest dto = new CustomerRequest(randomUUID().toString(), randomUUID().toString());
		final MockHttpServletRequestBuilder req = post("/customers")
				.contentType(APPLICATION_JSON)
				.accept(APPLICATION_JSON)
				.content(this.om.writeValueAsString(dto));

		this.mvc.perform(req)
				.andExpect(status().isCreated())
				.andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
				.andExpect(jsonPath("$.id", notNullValue()))
				.andExpect(jsonPath("$.firstName", equalTo(dto.getFirstName())))
				.andExpect(jsonPath("$.lastName", equalTo(dto.getLastName())));
	}
}