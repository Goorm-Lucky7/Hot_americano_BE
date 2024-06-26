package com.dart.api.domain.coupon.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.dart.api.domain.coupon.entity.GeneralCoupon;

@Repository
public interface GeneralCouponRepository extends JpaRepository<GeneralCoupon, Long> {
}
