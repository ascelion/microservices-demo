package ascelion.micro.mapper;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;

import com.github.dozermapper.core.DozerBeanMapperBuilder;
import com.github.dozermapper.core.Mapper;
import com.github.dozermapper.core.loader.api.BeanMappingBuilder;
import com.github.dozermapper.core.loader.api.FieldsMappingOption;
import com.github.dozermapper.core.loader.api.FieldsMappingOptions;
import com.github.dozermapper.core.loader.api.TypeMappingOptions;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;

@Component
@Lazy
@RequiredArgsConstructor
public class BeanToBeanMapper implements InitializingBean, BeanClassLoaderAware {

	static private final Logger L = LoggerFactory.getLogger(BeanToBeanMapper.class);

	@RequiredArgsConstructor
	@EqualsAndHashCode
	@ToString
	static private class Key {
		final Class<?> typeA;
		final Class<?> typeB;
		final boolean mapNulls;
	}

	private final Map<Key, Mapper> mappers = new ConcurrentHashMap<>();

	private ClassLoader cld = getClass().getClassLoader();

	public <T> T[] createArray(Class<T> target, Object[] sources, @NonNull Object... extra) {
		return createArray(target, stream(sources), extra);
	}

	public <T> T[] createArray(Class<T> target, Collection<?> sources, @NonNull Object... extra) {
		return createArray(target, sources.stream(), extra);
	}

	@SuppressWarnings("unchecked")
	public <T> T[] createArray(Class<T> target, Stream<?> sources, @NonNull Object... extra) {
		return sources
				.map(source -> create(target, Stream.concat(Stream.of(source), stream(extra))))
				.toArray(n -> (T[]) Array.newInstance(target, n));
	}

	public <T> List<T> createList(Class<T> target, Object[] sources, @NonNull Object... extra) {
		return createList(target, stream(sources), extra);
	}

	public <T> List<T> createList(Class<T> target, Collection<?> sources, @NonNull Object... extra) {
		return createList(target, sources.stream(), extra);
	}

	public <T> List<T> createList(Class<T> target, Stream<?> sources, @NonNull Object... extra) {
		return sources
				.map(source -> create(target, Stream.concat(Stream.of(source), stream(extra))))
				.collect(toList());
	}

	public <T> T create(@NonNull Class<T> target, @NonNull Object... sources) {
		return create(target, stream(sources));
	}

	public <T> T create(@NonNull Class<T> target, @NonNull Collection<?> sources) {
		return create(target, sources.stream());
	}

	public <T> T create(@NonNull Class<T> target, @NonNull Stream<?> sources) {
		T instance = null;

		for (final Iterator<?> it = sources.iterator(); it.hasNext();) {
			final var next = it.next();

			instance = mapper(next, target, true)
					.apply(instance);
		}

		return instance;
	}

	public <T> T copyWithNulls(@NonNull T target, @NonNull Object... sources) {
		return copy(true, target, sources);
	}

	public <T> T copyWithoutNulls(@NonNull T target, @NonNull Object... sources) {
		return copy(false, target, sources);
	}

	private <T> T copy(boolean mapNulls, @NonNull T target, @NonNull Object... sources) {
		@SuppressWarnings("unchecked")
		final var targetType = (Class<T>) target.getClass();

		for (final Object source : sources) {
			mapper(source, targetType, mapNulls)
					.apply(target);
		}

		return target;
	}

	@Override
	public void afterPropertiesSet() {
		this.mappers.clear();

		final var cps = new ClassPathScanningCandidateComponentProvider(false);

		cps.addIncludeFilter(new AnnotationTypeFilter(BBMap.class));
		cps.addIncludeFilter(new AnnotationTypeFilter(BBMap.Repeatable.class));
		cps.findCandidateComponents("ascelion")
				.forEach(this::initMapperFromBean);
	}

	@Override
	public void setBeanClassLoader(ClassLoader cld) {
		this.cld = cld;
	}

	@SneakyThrows
	private void initMapperFromBean(BeanDefinition def) {
		final var type = this.cld.loadClass(def.getBeanClassName());

		if (AnnotationUtils.findAnnotation(type, Component.class) != null) {
			initMapperFromComponent(type);
		} else {
			initMapperFromType(type);
		}
	}

	private void initMapperFromComponent(Class<?> type) {
		for (final BBMap map : type.getAnnotationsByType(BBMap.class)) {
			final var typeA = map.from();
			final var typeB = map.to();

			if (typeA == Void.class || typeB == Void.class) {
				throw new RuntimeException("Both @BBMap.from() and @BBMap.to() must be specified at component " + type.getName());
			}
			if (typeA == typeB) {
				throw new RuntimeException("Cannot use the same type for @BBMap.from() and @BBMap.to() at component " + type.getName());
			}

			initMapper(new Key(typeA, typeB, true), map, type);
			initMapper(new Key(typeA, typeB, false), map, type);
		}
	}

	private void initMapperFromType(Class<?> type) {
		for (BBMap map : type.getAnnotationsByType(BBMap.class)) {
			final var typeA = map.from();
			final var typeB = map.to();

			if (typeA != Void.class && typeB != Void.class) {
				throw new RuntimeException("Only one of @BBMap.from() or @BBMap.to() must be specified at type " + type.getName());
			}
			if (typeA == typeB) {
				throw new RuntimeException("Cannot use the same type for @BBMap.from() and @BBMap.to() at component " + type.getName());
			}

			final var bld = AnnotationBuilder.create(map);

			if (typeA == type) {
				throw new RuntimeException("@BBMap.from() is the same as the declaring type " + type.getName());
			}
			if (typeB == type) {
				throw new RuntimeException("@BBMap.to() is the same as the declaring type " + type.getName());
			}

			bld.with("from", typeA == Void.class ? type : typeA);
			bld.with("to", typeB == Void.class ? type : typeB);

			map = bld.build();

			initMapper(new Key(map.from(), map.to(), true), map, type);
			initMapper(new Key(map.from(), map.to(), false), map, type);
		}

		final var map = AnnotationBuilder.create(BBMap.class)
				.with("from", type)
				.with("to", type)
				.build();

		initMapper(new Key(type, type, true), map, type);
		initMapper(new Key(type, type, false), map, type);
	}

	private void initMapper(Key key, BBMap map, Class<?> component) {
		initOneWayMapper(key, map, component);

		if (key.typeA != key.typeB && map.bidi()) {
			final var bld = AnnotationBuilder.create(BBMap.class)
					.with("from", key.typeB)
					.with("to", key.typeA)
					.with("excludes", map.excludes());

			final var fields = stream(map.fields())
					.map(f -> {
						return AnnotationBuilder.create(BBField.class)
								.with("from", f.to().isEmpty() ? f.from() : f.to())
								.with("to", f.from())
								.with("hintFrom", f.hintTo())
								.with("hintTo", f.hintFrom())
								.build();
					})
					.toArray(BBField[]::new);

			bld.with("fields", fields);

			map = bld.build();

			key = new Key(map.from(), map.to(), key.mapNulls);

			initOneWayMapper(key, map, component);
		}
	}

	private void initOneWayMapper(Key key, BBMap map, Class<?> component) {
		if (this.mappers.containsKey(key)) {
			throw new RuntimeException("Already found " + key);
		}

		this.mappers.put(key, createOneWayMapper(key, map, component));
	}

	private <T> UnaryOperator<T> mapper(Object source, Class<T> target, boolean mapNulls) {
		final var sourceType = source.getClass();
		final var key = new Key(sourceType, target, mapNulls);
		final var mf = this.mappers.compute(key, (k, v) -> {
			if (v != null) {
				return v;
			}

			final BBMap map = AnnotationBuilder.create(BBMap.class)
					.with("from", sourceType)
					.with("to", target)
					.build();

			return createOneWayMapper(key, map, sourceType);
		});

		return i -> {
			if (i == null) {
				return mf.map(source, target);
			}

			mf.map(source, i);

			return i;
		};
	}

	private Mapper createOneWayMapper(Key key, BBMap map, Class<?> component) {
		return DozerBeanMapperBuilder.create()
				.withClassLoader(this.cld)
				.withMappingBuilder(new BeanMappingBuilder() {
					@Override
					protected void configure() {
						configureBMB(this, key, map, component);
					}
				})
				.build();
	}

	protected void configureBMB(BeanMappingBuilder bmb, Key key, BBMap map, Class<?> component) {
		L.trace("{}: adding mapping {} nulls from {} to {}", component.getName(), key.mapNulls ? "with" : "without", key.typeA.getName(), key.typeB.getName());

		final var typeA = bmb.type(key.typeA)
				.accessible();
		final var typeB = bmb.type(key.typeB)
				.accessible()
				.mapNull(key.mapNulls);
		final var tmb = bmb.mapping(typeA, typeB, TypeMappingOptions.oneWay());

		for (final var field : map.fields()) {
			var to = field.to();

			if (to.isEmpty()) {
				to = field.from();
			}

			L.trace("\tincluded from {} to {}", field.from(), to);

			final var options = new ArrayList<FieldsMappingOption>();

			if (field.hintFrom() != Void.class) {
				options.add(FieldsMappingOptions.hintA(field.hintFrom()));
			}
			if (field.hintTo() != Void.class) {
				options.add(FieldsMappingOptions.hintB(field.hintTo()));
			}

			tmb.fields(field.from(), to, options.toArray(FieldsMappingOption[]::new));
		}
	}

//	private MapperFactory buildFactory(boolean mapNulls) {
//		final var bld = new DefaultMapperFactory.Builder()
//				.constructorResolverStrategy(this::resolve)
//				.mapNulls(mapNulls);
//
//		if (this.sources) {
//			bld.compilerStrategy(new EclipseJdtCompilerStrategy());
//		}
//
//		return bld.build();
//	}
//
//	@SuppressWarnings("unchecked")
//	private <T, A, B> ConstructorMapping<T> resolve(ClassMap<A, B> classMap, Type<T> sourceType) {
//		final var aToB = classMap.getBType().equals(sourceType);
//		final var targetClass = aToB ? classMap.getBType() : classMap.getAType();
//		final var def = stream(targetClass.getRawType().getConstructors())
//				.filter(c -> c.getParameterCount() == 0)
//				.findFirst().orElse(null);
//
//		if (def == null || !Modifier.isPublic(def.getModifiers())) {
//			return new SimpleConstructorResolverStrategy().resolve(classMap, sourceType);
//		}
//
//		final var cm = new ConstructorMapping<T>();
//
//		cm.setConstructor((Constructor<T>) def);
//		cm.setParameterTypes(new Type[0]);
//
//		return cm;
//	}
}
