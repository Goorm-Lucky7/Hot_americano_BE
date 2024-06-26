package com.dart.api.application.coupon;

import static org.assertj.core.api.AssertionsForClassTypes.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dart.api.domain.coupon.entity.PriorityCoupon;
import com.dart.api.domain.coupon.entity.PriorityCouponWallet;
import com.dart.api.domain.coupon.repository.PriorityCouponRedisRepository;
import com.dart.api.domain.coupon.repository.PriorityCouponWalletRepository;
import com.dart.api.domain.notification.repository.SSESessionRepository;
import com.dart.api.dto.coupon.request.PriorityCouponPublishDto;
import com.dart.global.common.util.ClockHolder;
import com.dart.global.error.exception.BadRequestException;
import com.dart.global.error.exception.ConflictException;
import com.dart.support.CouponFixture;

@ExtendWith({MockitoExtension.class})
class PriorityCouponManageServiceTest {
	@Mock
	private PriorityCouponCacheService priorityCouponCacheService;

	@Mock
	private PriorityCouponRedisRepository priorityCouponRedisRepository;

	@Mock
	private PriorityCouponWalletRepository priorityCouponWalletRepository;

	@Mock
	private ClockHolder clockHolder;

	@Mock
	private SSESessionRepository sseSessionRepository;

	@InjectMocks
	private PriorityCouponManageService priorityCouponManageService;

	@DisplayName("10명의 사용자가 쿠폰 발행을 성공적으로 한다.")
	@MethodSource("com.dart.support.CouponFixture#provideValues_String")
	@ParameterizedTest
	void publish_all_success(Set<Long> values) {
		// Given
		PriorityCoupon priorityCoupon = CouponFixture.createPriorityCoupon();
		given(priorityCouponCacheService.getByStartAt(any(LocalDate.class))).willReturn(Optional.of(priorityCoupon));
		given(priorityCouponRedisRepository.getCount(eq(priorityCoupon.getId()))).willReturn(
			priorityCoupon.getStock() - 1);
		given(priorityCouponRedisRepository.rangeQueue(eq(priorityCoupon.getId()), any(long.class), any(long.class)))
			.willReturn(values);

		// When
		priorityCouponManageService.publish();

		// Then
		verify(priorityCouponWalletRepository, times(10)).save(any(PriorityCouponWallet.class));
	}

	@DisplayName("현재 발행 가능한 쿠폰이 없다.")
	@Test
	void publish_not_durationAt() {
		// Given
		given(priorityCouponCacheService.getByStartAt(any(LocalDate.class))).willReturn(Optional.empty());

		// When
		priorityCouponManageService.publish();

		// Then
		verify(priorityCouponWalletRepository, times(0)).save(any(PriorityCouponWallet.class));
		verify(priorityCouponRedisRepository, times(0)).getCount(any(Long.class));
		verify(priorityCouponRedisRepository, times(0)).increase(any(Long.class), any(long.class));
		verify(priorityCouponRedisRepository, times(0))
			.rangeQueue(any(Long.class), any(long.class), any(long.class));
	}

	@Test
	@DisplayName("쿠폰 발급 요청을 성공적으로 대기열 큐에 등록한다. - Void")
	void registerQueue_Success() {
		// GIVEN
		PriorityCoupon priorityCoupon = CouponFixture.createPriorityCoupon();
		Long testId = 1L;
		PriorityCouponPublishDto dto = new PriorityCouponPublishDto(priorityCoupon.getId());
		LocalDate nowDate = LocalDate.now();
		LocalDateTime localDateTime = nowDate.minusDays(1).atTime(LocalTime.MAX);

		given(priorityCouponCacheService.getByIdAndStartAt(eq(priorityCoupon.getId()),
			eq(nowDate))).willReturn((priorityCoupon));
		given(clockHolder.minusOneDaysAtTime(eq(priorityCoupon.getEndedAt()))).willReturn(localDateTime);
		given(priorityCouponRedisRepository.hasValue(eq(priorityCoupon.getId()), eq(testId))).willReturn(false);

		// WHEN
		priorityCouponManageService.registerQueue(dto, testId);

		// THEN
		verify(priorityCouponRedisRepository)
			.addIfAbsentQueue(eq(priorityCoupon.getId()), eq(testId), anyDouble(), anyLong());
	}

	@Test
	@DisplayName("이미 해당 쿠폰은 발급받은 쿠폰이다. - ConflictException")
	void registerQueue_No_ConflictException() {
		// GIVEN
		PriorityCoupon priorityCoupon = CouponFixture.createPriorityCoupon();
		Long testId = 1L;
		PriorityCouponPublishDto dto = new PriorityCouponPublishDto(priorityCoupon.getId());
		LocalDate nowDate = LocalDate.now();
		LocalDateTime localDateTime = nowDate.minusDays(1).atTime(LocalTime.MAX);

		given(clockHolder.minusOneDaysAtTime(eq(priorityCoupon.getEndedAt()))).willReturn(localDateTime);
		given(
			priorityCouponCacheService.getByIdAndStartAt(eq(priorityCoupon.getId()), any(LocalDate.class))).willReturn(
			(priorityCoupon));
		given(priorityCouponRedisRepository.hasValue(eq(priorityCoupon.getId()), eq(testId))).willReturn(true);

		// When & Then
		assertThatThrownBy(() -> priorityCouponManageService.registerQueue(dto, testId))
			.isInstanceOf(ConflictException.class);
	}

	@Test
	@DisplayName("해당 쿠폰은 재고가 마감된 쿠폰이다. - BadRequestException")
	@Disabled
	void registerQueue_No_BadRequestException() {
		// GIVEN
		PriorityCoupon priorityCoupon = CouponFixture.createPriorityCoupon();
		Long testId = 1L;
		PriorityCouponPublishDto dto = new PriorityCouponPublishDto(priorityCoupon.getId());
		LocalDate nowDate = LocalDate.now();
		LocalDateTime localDateTime = nowDate.minusDays(1).atTime(LocalTime.MAX);

		given(clockHolder.minusOneDaysAtTime(eq(priorityCoupon.getEndedAt()))).willReturn(localDateTime);
		given(
			priorityCouponCacheService.getByIdAndStartAt(eq(priorityCoupon.getId()), any(LocalDate.class))).willReturn(
			(priorityCoupon));
		given(priorityCouponRedisRepository.hasValue(eq(priorityCoupon.getId()), eq(testId))).willReturn(false);
		given(priorityCouponRedisRepository.sizeQueue(eq(priorityCoupon.getId()))).willReturn(
			priorityCoupon.getStock());

		// When & Then
		assertThatThrownBy(() -> priorityCouponManageService.registerQueue(dto, testId))
			.isInstanceOf(BadRequestException.class);
	}
}

