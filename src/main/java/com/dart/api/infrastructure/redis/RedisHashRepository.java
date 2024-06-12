package com.dart.api.infrastructure.redis;

import java.time.Duration;
import java.util.Map;

import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class RedisHashRepository {

	private final StringRedisTemplate redisTemplate;

	public String get(String key) {
		ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
		return valueOperations.get(key);
	}

	public boolean exists(String key) {
		return Boolean.TRUE.equals(redisTemplate.hasKey(key));
	}

	public void setExpire(String key, String value, long duration) {
		ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
		Duration expireDuration = Duration.ofSeconds(duration);
		valueOperations.set(key, value, expireDuration);
	}

	public void delete(String key) {
		redisTemplate.delete(key);
	}

	public void setHashOps(String key, Map<String, String> data) {
		HashOperations<String, Object, Object> values = redisTemplate.opsForHash();
		values.putAll(key, data);
	}

	@Transactional(readOnly = true)
	public String getHashOps(String key, String hashKey) {
		HashOperations<String, Object, Object> values = redisTemplate.opsForHash();
		return Boolean.TRUE.equals(values.hasKey(key, hashKey)) ? (String) values.get(key, hashKey) : "";
	}

	public void deleteHashOps(String key, String hashKey) {
		HashOperations<String, Object, Object> values = redisTemplate.opsForHash();
		values.delete(key, hashKey);
	}
}