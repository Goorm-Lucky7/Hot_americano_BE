package com.dart.api.application.auth;

import static com.dart.global.common.util.AuthConstant.*;
import static com.dart.global.common.util.GlobalConstant.*;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dart.api.domain.auth.entity.AuthUser;
import com.dart.api.domain.member.entity.Member;
import com.dart.api.domain.member.repository.MemberRepository;
import com.dart.global.error.exception.NotFoundException;
import com.dart.global.error.exception.UnauthorizedException;
import com.dart.global.error.model.ErrorCode;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtProviderService {

	private static final String ID = "id";
	private static final String EMAIL = "email";
	private static final String NICKNAME = "nickname";
	private static final String PROFILE_IMAGE = "profileImage";

	@Value("${jwt.secret.access-key}")
	private String secret;

	@Value("${jwt.access-expire}")
	private long accessTokenExpire;

	@Value("${jwt.refresh-expire}")
	private long refreshTokenExpire;

	private SecretKey secretKey;

	private final MemberRepository memberRepository;

	@PostConstruct
	private void init() {
		secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
	}

	public String generateAccessToken(Long id, String email, String nickname, String profileImage, String uniqueIdentifier) {
		return buildJwt(new Date(), new Date(System.currentTimeMillis() + accessTokenExpire))
			.claim(ID, id)
			.claim(EMAIL, email)
			.claim(NICKNAME, nickname)
			.claim(PROFILE_IMAGE, profileImage)
			.claim("iat", new Date().getTime())
			.claim("jti", uniqueIdentifier)
			.issuedAt(new Date())
			.expiration(new Date(System.currentTimeMillis() + accessTokenExpire))
			.signWith(secretKey, Jwts.SIG.HS256)
			.compact();
	}

	public String generateRefreshToken(String email) {
		return buildJwt(new Date(), new Date(System.currentTimeMillis() + refreshTokenExpire))
			.claim(EMAIL, email)
			.compact();
	}

	@Transactional
	public String reGenerateAccessToken(String accessToken) {
		final Claims claims = getClaimsByToken(accessToken);
		final String email = claims.get(EMAIL, String.class);
		final Member member = memberRepository.findByEmail(email)
			.orElseThrow(() -> new NotFoundException(ErrorCode.FAIL_MEMBER_NOT_FOUND));

		String uniqueIdentifier = UUID.randomUUID().toString();

		return generateAccessToken(member.getId(), member.getEmail(), member.getNickname(),
			member.getProfileImageUrl(), uniqueIdentifier);
	}

	public String extractToken(String header, HttpServletRequest request) {
		String token = request.getHeader(header);

		if (token == null || !token.startsWith(BEARER)) {
			log.warn("====== {} is null or not bearer =======", header);
			return null;
		}

		return token.replaceFirst(BEARER, BLANK).trim();
	}

	public AuthUser extractAuthUserByAccessToken(String token) {
		final Claims claims = getClaimsByToken(token);
		return AuthUser.create(claims.get(ID, Long.class), claims.get(EMAIL, String.class),
			claims.get(NICKNAME, String.class));
	}

	public String extractEmailFromAccessToken(String accessToken) {
		try {
			final Claims claims = getClaimsByToken(accessToken);
			return claims.get(EMAIL, String.class);
		} catch (Exception e) {
			throw new UnauthorizedException(ErrorCode.FAIL_INVALID_TOKEN);
		}
	}

	public String extractEmailFromRefreshToken(String refreshToken) {
		try {
			final Claims claims = getClaimsByToken(refreshToken);
			return claims.get(EMAIL, String.class);
		} catch (Exception e) {
			throw new UnauthorizedException(ErrorCode.FAIL_INVALID_TOKEN);
		}
	}

	public boolean isUsable(String token) {
		try {
			Jwts.parser()
				.verifyWith(secretKey)
				.build()
				.parseSignedClaims(token);

			return true;
		} catch (ExpiredJwtException e) {
			log.warn("====== TOKEN EXPIRED ======");
			throw new UnauthorizedException(ErrorCode.FAIL_TOKEN_EXPIRED);
		} catch (IllegalArgumentException e) {
			log.warn("====== EMPTIED TOKEN ======");
		} catch (UnsupportedJwtException | MalformedJwtException | SignatureException e) {
			log.warn("====== WRONG TYPE TOKEN ======");
			throw new UnauthorizedException(ErrorCode.FAIL_TOKEN_EXPIRED);
		} catch (Exception e) {
			log.warn("====== INVALID TOKEN ======");
			throw new UnauthorizedException(ErrorCode.FAIL_INVALID_TOKEN);
		}

		return false;
	}

	public void validateTokenExists(String token) {
		try {
			Jwts.parser()
				.setSigningKey(secretKey)
				.build()
				.parseClaimsJws(token);
		} catch (ExpiredJwtException e) {
			throw new UnauthorizedException(ErrorCode.FAIL_TOKEN_EXPIRED);
		} catch (UnsupportedJwtException | MalformedJwtException | SignatureException e) {
			throw new UnauthorizedException(ErrorCode.FAIL_INVALID_TOKEN);
		} catch (IllegalArgumentException e) {
			throw new UnauthorizedException(ErrorCode.FAIL_INVALID_TOKEN);
		}
	}

	private JwtBuilder buildJwt(Date issuedDate, Date expiredDate) {
		return Jwts.builder()
			.issuedAt(issuedDate)
			.expiration(expiredDate)
			.signWith(secretKey, Jwts.SIG.HS256);
	}

	private Claims getClaimsByToken(String token) {
		try {
			return Jwts.parser()
				.verifyWith(secretKey)
				.build()
				.parseSignedClaims(token)
				.getPayload();
		} catch (ExpiredJwtException e) {
			return e.getClaims();
		} catch (Exception e) {
			throw new UnauthorizedException(ErrorCode.FAIL_TOKEN_EXPIRED);
		}
	}
}
