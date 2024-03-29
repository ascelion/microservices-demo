package ascelion.micro.shared.endpoint;

import java.util.UUID;

import ascelion.micro.shared.model.AbstractEntity;
import ascelion.micro.shared.model.EntityRepo;

public abstract class EntityEndpointBase<T extends AbstractEntity<T>, R extends EntityRepo<T>, U>
		extends ViewEntityEndpointBase<T, R>
		implements EntityEndpoint<T, U> {

	public EntityEndpointBase(R repo) {
		super(repo);
	}

	@Override
	public T createEntity(U request) {
		return this.repo.save(this.bbm.create(this.type, request));
	}

	@Override
	public T updateEntity(UUID id, U request) {
		final T ent = this.repo.getById(id);

		this.bbm.copyWithNulls(ent, request);

		return this.repo.save(ent);
	}

	@Override
	public T patchEntity(UUID id, U request) {
		final T ent = this.repo.getById(id);

		this.bbm.copyWithoutNulls(ent, request);

		return this.repo.save(ent);
	}
}
