package com.dart.api.application.auth;


import static com.dart.global.common.util.AuthConstant.*;
import static com.dart.global.common.util.GlobalConstant.*;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.dart.api.domain.auth.repository.TokenRedisRepository;
import com.dart.api.domain.auth.entity.AuthUser;
import com.dart.api.domain.member.entity.Member;
import com.dart.api.domain.member.repository.MemberRepository;
import com.dart.api.dto.auth.response.TokenResDto;
import com.dart.api.dto.member.request.LoginReqDto;
import com.dart.api.dto.member.response.LoginResDto;
import com.dart.global.common.util.CookieUtil;
import com.dart.global.error.exception.BadRequestException;
import com.dart.global.error.exception.NotFoundException;
import com.dart.global.error.exception.UnauthorizedException;
import com.dart.global.error.model.ErrorCode;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {

	private final MemberRepository memberRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtProviderService jwtProviderService;
	private final TokenRedisRepository tokenRedisRepository;

	private final CookieUtil cookieUtil;

	@Transactional
	public LoginResDto login(LoginReqDto loginReqDto, HttpServletResponse response) {
		final Member member = findByMemberEmail(loginReqDto.email());
		validatePasswordMatch(loginReqDto.password(), member.getPassword());

		final String accessToken = jwtProviderService.generateAccessToken(member.getEmail(), member.getNickname(), member.getProfileImageUrl());
		final String refreshToken = jwtProviderService.generateRefreshToken(member.getEmail());

		tokenRedisRepository.setToken(loginReqDto.email(), refreshToken);

		setTokensInResponse(response, accessToken, refreshToken);

		return new LoginResDto(accessToken, member.getEmail(), member.getNickname(), member.getProfileImageUrl());
	}

	public TokenResDto reissue(HttpServletRequest request, HttpServletResponse response) {
		String accessToken = extractTokenFromHeader(request);
		String refreshToken = cookieUtil.getCookie(request, REFRESH_TOKEN_COOKIE_NAME);

		validateRefreshToken(refreshToken);

		final AuthUser authUser = jwtProviderService.extractAuthUserByAccessToken(accessToken);
		validateSavedRefreshToken(authUser.email(), refreshToken);

		Member member = getMemberByEmail(authUser.email());

		String newAccessToken = jwtProviderService.generateAccessToken(member.getEmail(), member.getNickname(), member.getProfileImageUrl());
		String newRefreshToken = jwtProviderService.generateRefreshToken(authUser.email());

		tokenRedisRepository.deleteToken(authUser.email());
		tokenRedisRepository.setToken(authUser.email(), newRefreshToken);

		setTokensInResponse(response, newAccessToken, newRefreshToken);

		return new TokenResDto(accessToken);
	}

	private Member findByMemberEmail(String email) {
		return memberRepository.findByEmail(email)
			.orElseThrow(() -> new NotFoundException(ErrorCode.FAIL_MEMBER_NOT_FOUND));
	}

	private void validatePasswordMatch(String password, String encodedPassword) {
		if (!passwordEncoder.matches(password, encodedPassword)) {
			throw new BadRequestException(ErrorCode.FAIL_INCORRECT_PASSWORD);
		}
	}

	private void validateRefreshToken(String refreshToken) {
		if (!jwtProviderService.isUsable(refreshToken)) {
			throw new UnauthorizedException(ErrorCode.FAIL_INVALID_TOKEN);
		}
	}

	private void validateSavedRefreshToken(String email, String refreshToken) {
		String savedRefreshToken = tokenRedisRepository.getToken(email);
		if (!savedRefreshToken.equals(refreshToken)) {
			throw new UnauthorizedException(ErrorCode.FAIL_INVALID_TOKEN);
		}
	}

	private Member getMemberByEmail(String email) {
		return memberRepository.findByEmail(email)
			.orElseThrow(() -> new NotFoundException(ErrorCode.FAIL_MEMBER_NOT_FOUND));
	}

	private void setTokensInResponse(HttpServletResponse response, String accessToken, String refreshToken) {
		setAccessToken(response, accessToken);
		setRefreshToken(response, refreshToken);
	}

	private String extractTokenFromHeader(HttpServletRequest request) {
		return request.getHeader(ACCESS_TOKEN_HEADER).replace(BEARER, BLANK).trim();
	}

	private void setAccessToken(HttpServletResponse response, String accessToken) {
		response.setHeader(ACCESS_TOKEN_HEADER, accessToken);
	}

	private void setRefreshToken(HttpServletResponse response, String refreshToken){
		cookieUtil.setRefreshCookie(response, refreshToken);
	}
}
