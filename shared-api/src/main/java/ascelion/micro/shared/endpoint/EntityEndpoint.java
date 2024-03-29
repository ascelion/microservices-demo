package ascelion.micro.shared.endpoint;

import java.util.UUID;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.groups.Default;

import ascelion.micro.shared.model.AbstractEntity;
import ascelion.micro.shared.validation.OnCreate;
import ascelion.micro.shared.validation.OnPatch;
import ascelion.micro.shared.validation.OnUpdate;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;

public interface EntityEndpoint<T extends AbstractEntity<T>, U> extends ViewEntityEndpoint<T> {

	@ApiOperation("Create a new entity")
	@PostMapping(consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	@Validated({ Default.class, OnCreate.class })
	public T createEntity(
	//@formatter:off
	        @ApiParam(value = "The entity data", required = true)
	        @RequestBody
	        @NotNull @Valid U request
	//@formatter:on
	);

	@ApiOperation("Update an existing entity")
	@PutMapping(path = "{id}", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	// @PreAuthorize(Main.IS_ADMIN)
	@Validated({ Default.class, OnUpdate.class })
	public T updateEntity(
	//@formatter:off
	        @ApiParam(value = "The entity identifier", required = true)
	        @PathVariable("id")
	        @NotNull UUID id,

	        @ApiParam(value = "The entity data", required = true)
	        @RequestBody
	        @NotNull @Valid U request
	//@formatter:on
	);

	@ApiOperation("Partially update an existing entity")
	@PatchMapping(path = "{id}", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	// @PreAuthorize(Main.IS_ADMIN)
	@Validated({ Default.class, OnPatch.class })
	public T patchEntity(
	//@formatter:off
	        @ApiParam(value = "The entity identifier", required = true)
	        @PathVariable("id")
	        @NotNull UUID id,

	        @ApiParam(value = "The entity data", required = true)
	        @RequestBody
	        @NotNull @Valid U patch
	//@formatter:on
	);
}
