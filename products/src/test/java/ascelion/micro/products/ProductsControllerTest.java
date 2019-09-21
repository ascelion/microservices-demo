package ascelion.micro.products;

import java.math.BigDecimal;
import java.util.List;

import javax.sql.DataSource;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@RunWith(SpringRunner.class)
@WebMvcTest(ProductsController.class)
public class ProductsControllerTest {
	@Autowired
	private MockMvc mvc;
	@Autowired
	private ObjectMapper om;
	@MockBean
	private ProductsRepository repo;
	@MockBean
	private DataSource ds;

	private final List<Product> products = Products.generate(7);

	@Before
	public void setUp() {
		when(this.repo.findAll())
				.then(ivc -> {
					return this.products;
				});
		when(this.repo.findById(any()))
				.then(ivc -> {
					return this.products.stream()
							.filter(p -> p.getId().equals(ivc.getArgument(0)))
							.findFirst();
				});
		when(this.repo.save(any()))
				.then(ivc -> {
					final Product p = ivc.getArgument(0);

					if (p.getId() == null) {
						final Product newP = Products.generateOne(this.products.size() + 2L);
						newP.setName(p.getName());
						newP.setDescription(p.getDescription());
						newP.setPrice(p.getPrice());

						this.products.add(newP);

						return newP;
					} else {
						this.products.set(p.getId().intValue() - 1, p);

						return p;
					}
				});
	}

	@Test
	@WithUser
	public void getAll() throws Exception {
		final MockHttpServletRequestBuilder req = get("/products")
				.accept(APPLICATION_JSON);

		this.mvc.perform(req)
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
				.andExpect(jsonPath("$", hasSize(this.products.size())))
				.andExpect(jsonPath("$[0].id", equalTo(1)));
	}

	@Test
	public void getAllAnonymous() throws Exception {
		final MockHttpServletRequestBuilder req = get("/products")
				.accept(APPLICATION_JSON);

		this.mvc.perform(req)
				.andExpect(status().isUnauthorized());
	}

	@Test
	@WithUser
	public void getAllInvalid() throws Exception {
		final MockHttpServletRequestBuilder req = get("/products")
				.param("page", "0")
				.param("size", "5")
				.accept(APPLICATION_JSON);

		this.mvc.perform(req)
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
				.andExpect(jsonPath("$.messages", hasSize(1)));
	}

	@Test
	@WithUser
	public void getOne() throws Exception {
		final int id = this.products.get(this.products.size() / 2).getId().intValue();
		final MockHttpServletRequestBuilder req = get("/products/" + id)
				.accept(APPLICATION_JSON);

		this.mvc.perform(req)
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
				.andExpect(jsonPath("$.id", equalTo(id)));
	}

	@Test
	@WithUser
	public void getNotFound() throws Exception {
		final int id = this.products.size() * 2;
		final MockHttpServletRequestBuilder req = get("/products/" + id)
				.accept(APPLICATION_JSON);

		this.mvc.perform(req)
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
				.andExpect(jsonPath("$.messages", hasSize(1)));
	}

	@Test
	@WithAdmin
	public void create() throws Exception {
		final ProductRequest dto = new ProductRequest("add name", "add description", BigDecimal.ONE);
		final MockHttpServletRequestBuilder req = post("/products")
				.contentType(APPLICATION_JSON)
				.accept(APPLICATION_JSON)
				.content(this.om.writeValueAsString(dto));

		this.mvc.perform(req)
				.andExpect(status().isCreated())
				.andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
				.andExpect(jsonPath("$.id", equalTo(this.products.size() + 1)))
				.andExpect(jsonPath("$.name", equalTo(dto.getName())))
				.andExpect(jsonPath("$.description", equalTo(dto.getDescription())))
				.andExpect(jsonPath("$.price", equalTo(dto.getPrice().intValue())));
	}

	@Test
	@WithAdmin
	public void createInvalid() throws Exception {
		final ProductRequest dto = new ProductRequest(null, null, null);
		final MockHttpServletRequestBuilder req = post("/products")
				.contentType(APPLICATION_JSON)
				.accept(APPLICATION_JSON)
				.content(this.om.writeValueAsString(dto));

		this.mvc.perform(req)
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
				.andExpect(jsonPath("$.messages", hasSize(3)));
	}

	@Test
	public void createAnonymous() throws Exception {
		final MockHttpServletRequestBuilder req = post("/products")
				.contentType(APPLICATION_JSON);

		this.mvc.perform(req)
				.andExpect(status().isUnauthorized());
	}

	@Test
	@WithUser
	public void createAsUser() throws Exception {
		final ProductRequest dto = new ProductRequest("add name", "add description", BigDecimal.ONE);
		final MockHttpServletRequestBuilder req = post("/products")
				.contentType(APPLICATION_JSON)
				.accept(APPLICATION_JSON)
				.content(this.om.writeValueAsString(dto));

		this.mvc.perform(req)
				.andExpect(status().isForbidden());
	}

	@Test
	@WithAdmin
	public void updateProduct() throws Exception {
		final int id = this.products.get(this.products.size() / 2).getId().intValue();
		final ProductRequest dto = new ProductRequest("new name", "new description", BigDecimal.ONE);

		final MockHttpServletRequestBuilder req = put("/products/" + id)
				.contentType(APPLICATION_JSON)
				.accept(APPLICATION_JSON)
				.content(this.om.writeValueAsString(dto));

		this.mvc.perform(req)
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
				.andExpect(jsonPath("$.id", equalTo(id)))
				.andExpect(jsonPath("$.name", equalTo(dto.getName())))
				.andExpect(jsonPath("$.description", equalTo(dto.getDescription())))
				.andExpect(jsonPath("$.price", equalTo(dto.getPrice().intValue())));
	}

	@Test
	@WithAdmin
	public void updateInvalid() throws Exception {
		final int id = this.products.get(this.products.size() / 2).getId().intValue();
		final ProductRequest dto = new ProductRequest(null, null, null);

		final MockHttpServletRequestBuilder req = put("/products/" + id)
				.contentType(APPLICATION_JSON)
				.accept(APPLICATION_JSON)
				.content(this.om.writeValueAsString(dto));

		this.mvc.perform(req)
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
				.andExpect(jsonPath("$.messages", hasSize(3)));
	}

	@Test
	@WithAdmin
	public void updateNotFound() throws Exception {
		final int id = this.products.size() * 2;
		final ProductRequest dto = new ProductRequest("new name", "new description", BigDecimal.ONE);
		final MockHttpServletRequestBuilder req = put("/products/" + id)
				.contentType(APPLICATION_JSON)
				.accept(APPLICATION_JSON)
				.content(this.om.writeValueAsString(dto));

		this.mvc.perform(req)
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
				.andExpect(jsonPath("$.messages", hasSize(1)));
	}

	@Test
	public void updateAnonymous() throws Exception {
		final MockHttpServletRequestBuilder req = put("/products/314")
				.contentType(APPLICATION_JSON);

		this.mvc.perform(req)
				.andExpect(status().isUnauthorized());
	}

	@Test
	@WithUser
	public void updateAsUser() throws Exception {
		final int id = this.products.get(this.products.size() / 2).getId().intValue();
		final ProductRequest dto = new ProductRequest("new name", "new description", BigDecimal.ONE);
		final MockHttpServletRequestBuilder req = put("/products/" + id)
				.contentType(APPLICATION_JSON)
				.accept(APPLICATION_JSON)
				.content(this.om.writeValueAsString(dto));

		this.mvc.perform(req)
				.andExpect(status().isForbidden());
	}

	@Test
	@WithAdmin
	public void patchProduct() throws Exception {
		final int id = this.products.get(this.products.size() / 2).getId().intValue();
		final ProductRequest dto = new ProductRequest("new name", "new description", BigDecimal.ONE);
		final MockHttpServletRequestBuilder req = patch("/products/" + id)
				.contentType(APPLICATION_JSON)
				.accept(APPLICATION_JSON)
				.content(this.om.writeValueAsString(dto));

		this.mvc.perform(req)
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
				.andExpect(jsonPath("$.id", equalTo(id)))
				.andExpect(jsonPath("$.name", equalTo(dto.getName())))
				.andExpect(jsonPath("$.description", equalTo(dto.getDescription())))
				.andExpect(jsonPath("$.price", equalTo(dto.getPrice().intValue())));
	}

	@Test
	@WithAdmin
	public void patchProductInvalid() throws Exception {
		final int id = this.products.get(this.products.size() / 2).getId().intValue();
		final ProductRequest dto = new ProductRequest(null, null, null);
		final MockHttpServletRequestBuilder req = patch("/products/" + id)
				.contentType(APPLICATION_JSON)
				.accept(APPLICATION_JSON)
				.content(this.om.writeValueAsString(dto));

		this.mvc.perform(req)
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
				.andExpect(jsonPath("$.messages", hasSize(1)));
	}

	@Test
	@WithAdmin
	public void patchProductNotFound() throws Exception {
		final int id = this.products.size() * 2;
		final ProductRequest dto = new ProductRequest("new name", "new description", BigDecimal.ONE);
		final MockHttpServletRequestBuilder req = patch("/products/" + id)
				.contentType(APPLICATION_JSON)
				.accept(APPLICATION_JSON)
				.content(this.om.writeValueAsString(dto));

		this.mvc.perform(req)
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
				.andExpect(jsonPath("$.messages", hasSize(1)));
	}

	@Test
	public void patchProductAnonymous() throws Exception {
		final MockHttpServletRequestBuilder req = patch("/products/314")
				.contentType(APPLICATION_JSON);

		this.mvc.perform(req)
				.andExpect(status().isUnauthorized());
	}

	@Test
	@WithUser
	public void patchProductAsUser() throws Exception {
		final int id = this.products.get(this.products.size() / 2).getId().intValue();
		final ProductRequest dto = new ProductRequest("new name", "new description", BigDecimal.ONE);
		final MockHttpServletRequestBuilder req = patch("/products/" + id)
				.contentType(APPLICATION_JSON)
				.accept(APPLICATION_JSON)
				.content(this.om.writeValueAsString(dto));

		this.mvc.perform(req)
				.andExpect(status().isForbidden());
	}
}
