package com.dart.global.common.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SSEConstant {

	public static final long SSE_DEFAULT_TIMEOUT = 60 * 60 * 1000L;
	public static final String SSE_EMITTER_EVENT_NAME = "SSE";
}
