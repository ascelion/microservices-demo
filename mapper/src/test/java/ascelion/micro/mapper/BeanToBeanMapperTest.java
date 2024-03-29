package ascelion.micro.mapper;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {
		BeanToBeanMapper.class,
})
@ActiveProfiles("test")
public class BeanToBeanMapperTest {
	@BBMap(to = Bean2.class)
	@AllArgsConstructor
	@NoArgsConstructor
	@Getter
	static public class Bean1 {
		private String field1;
		private String field2;
	}

	@AllArgsConstructor
	@NoArgsConstructor
	@Getter
	static public class Bean2 {
		private String field3;
	}

	@BBMap(from = Bean1.class, bidi = true,
			fields = {
					@BBField(from = "field1", to = "b.field1"),
					@BBField(from = "field2", to = "b.field2"),
			})
	@NoArgsConstructor
	@AllArgsConstructor
	@Getter
	static public class Bean3 {
		private Bean1 b;
		private String field3;
	}

	@Configuration
	@BBMap(from = Bean2.class, to = Bean3.class)
	static public class Mappings {
	}

	@Autowired
	private BeanToBeanMapper bbm;

	@Test
	public void bean3_to_bean1_create() {
		final var b3 = new Bean3(new Bean1("value1", "value2"), "value3");
		final var b1 = this.bbm.create(Bean1.class, b3);

		assertThat(b1.field1, equalTo("value1"));
		assertThat(b1.field2, equalTo("value2"));
	}

	@Test
	public void bean1_to_bean3_create() {
		final var b1 = new Bean1("value1", "value2");
		final var b3 = this.bbm.create(Bean3.class, b1);

		assertThat(b3.b.field1, equalTo("value1"));
		assertThat(b3.b.field2, equalTo("value2"));
	}

	@Test
	public void bean3_to_bean1_with_nulls() {
		final var b3 = new Bean3(new Bean1("value1", null), "value3");
		final var b1 = new Bean1("old1", "old2");

		this.bbm.copyWithNulls(b1, b3);

		assertThat(b1.field1, equalTo("value1"));
		assertThat(b1.field2, nullValue());
	}

	@Test
	public void bean2_to_bean3_create() {
		final var b2 = new Bean2("value3");
		final var b3 = this.bbm.create(Bean3.class, b2);

		assertThat(b3.field3, equalTo(b2.field3));
	}

	@Test
	public void bean1_to_bean3_with_nulls() {
		final var b1 = new Bean1("value1", null);
		final var b3 = new Bean3(new Bean1("old1", "old2"), "old3");

		this.bbm.copyWithNulls(b3, b1);

		assertThat(b3.b.field1, equalTo("value1"));
		assertThat(b3.b.field2, nullValue());
	}

	@Test
	public void bean3_to_bean1_without_nulls() {
		final var b3 = new Bean3(new Bean1("value1", null), "value3");
		final var b1 = new Bean1("old1", "old2");

		this.bbm.copyWithoutNulls(b1, b3);

		assertThat(b1.field1, equalTo("value1"));
		assertThat(b1.field2, equalTo("old2"));
	}

	@Test
	public void bean1_to_bean3_without_nulls() {
		final var b1 = new Bean1("value1", null);
		final var b3 = new Bean3(new Bean1("old1", "old2"), "old3");

		this.bbm.copyWithoutNulls(b3, b1);

		assertThat(b3.b.field1, equalTo("value1"));
		assertThat(b3.b.field2, equalTo("old2"));
	}

}
