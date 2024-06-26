package com.dart.api.domain.coupon.entity;

import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CouponType {
	TEN_PERCENT("10%", 10),
	FIFTEEN_PERCENT("15%", 15),
	TWENTY_PERCENT("20%", 20),
	THIRTY_PERCENT("30%", 30);

	private final String name;
	private final int value;

	private static final Map<Integer, CouponType> valuesMap = Collections.unmodifiableMap(Stream.of(values())
		.collect(Collectors.toMap(CouponType::getValue, Function.identity())));

	private static final Map<String, CouponType> namesMap = Collections.unmodifiableMap(Stream.of(values())
		.collect(Collectors.toMap(CouponType::getName, Function.identity())));

	public static CouponType fromName(String name) {
		return namesMap.get(name);
	}
}
