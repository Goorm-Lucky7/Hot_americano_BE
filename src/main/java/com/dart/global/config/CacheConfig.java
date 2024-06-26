package com.dart.global.config;

import java.time.Duration;
import java.time.LocalTime;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Configuration
@EnableCaching
public class CacheConfig {

	@Bean
	public RedisCacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
		RedisCacheConfiguration redisCacheConfiguration = RedisCacheConfiguration.defaultCacheConfig()
			.entryTtl(Duration.ofMinutes(10))
			.serializeValuesWith(RedisSerializationContext.SerializationPair
				.fromSerializer(new Jackson2JsonRedisSerializer<>(Object.class)));

		return RedisCacheManager.builder(redisConnectionFactory)
			.cacheDefaults(redisCacheConfiguration)
			.build();
	}

	@Bean
	@Primary
	public RedisCacheManager couponCacheManager(RedisConnectionFactory redisConnectionFactory) {
		return RedisCacheManager.builder(redisConnectionFactory)
			.cacheDefaults(redisCacheConfiguration())
			.build();
	}

	@Bean
	public RedisCacheConfiguration redisCacheConfiguration() {
		var strSerializePair = RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer());
		var objSerializePair = RedisSerializationContext.SerializationPair.fromSerializer(
			new GenericJackson2JsonRedisSerializer(objectMapper()));

		return RedisCacheConfiguration.defaultCacheConfig()
			.entryTtl(getTtl())
			.serializeKeysWith(strSerializePair)
			.serializeValuesWith(objSerializePair);
	}

	private Duration getTtl() {
		LocalTime now = LocalTime.now();
		LocalTime end = LocalTime.MAX;

		return Duration.between(now, end);
	}

	private ObjectMapper objectMapper() {
		PolymorphicTypeValidator polymorphicTypeValidator = BasicPolymorphicTypeValidator.builder()
			.allowIfSubType(Object.class)
			.build();

		return JsonMapper.builder()
			.polymorphicTypeValidator(polymorphicTypeValidator)
			.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
			.addModule(new JavaTimeModule())
			.activateDefaultTyping(polymorphicTypeValidator, ObjectMapper.DefaultTyping.NON_FINAL)
			.build();
	}
}
