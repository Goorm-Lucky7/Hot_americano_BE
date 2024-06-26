package com.dart.api.domain.payment.repository;

import static com.dart.global.common.util.RedisConstant.*;
import static com.dart.global.common.util.PaymentConstant.*;

import org.springframework.stereotype.Repository;

import com.dart.api.infrastructure.redis.ValueRedisRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PaymentRedisRepository {

	private final ValueRedisRepository valueRedisRepository;

	public void deleteData(String key) {
		valueRedisRepository.deleteValue(REDIS_PAYMENT_PREFIX + key);
	}

	public void setData(String key, String value) {
		valueRedisRepository.saveValueWithExpiry(REDIS_PAYMENT_PREFIX + key, value, THIRTY_MINUTE);
	}
}
