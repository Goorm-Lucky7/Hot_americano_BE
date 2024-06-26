package com.dart.global.config;

import static com.dart.global.common.util.ChatConstant.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.SockJsServiceRegistration;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.StompWebSocketEndpointRegistration;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

@ExtendWith(MockitoExtension.class)
class WebSocketConfigTest {

	@Mock
	private StompEndpointRegistry stompEndpointRegistry;

	@Mock
	private StompWebSocketEndpointRegistration stompWebSocketEndpointRegistration;

	@Mock
	private WebSocketTransportRegistration webSocketTransportRegistration;

	@Mock
	private MessageBrokerRegistry messageBrokerRegistry;

	@Mock
	private SockJsServiceRegistration sockJsServiceRegistration;

	@InjectMocks
	private WebSocketConfig webSocketConfig;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
	}

	@Test
	@DisplayName("CONFIGURE MESSAGE BROKER(⭕️ SUCCESS): 브로커를 활성화하고, 애플리케이션 목적지 접두사를 설정합니다.")
	void configureMessageBroker_void_success() {
		// WHEN
		webSocketConfig.configureMessageBroker(messageBrokerRegistry);

		// THEN
		verify(messageBrokerRegistry).enableSimpleBroker(SUBSCRIPTION_PREFIX);
		verify(messageBrokerRegistry).setApplicationDestinationPrefixes(APPLICATION_DESTINATION_PREFIX);
	}

	@Test
	@DisplayName("REGISTER STOMP END POINTS(⭕️ SUCCESS): 핸드세이크 핸들러 없이 엔드포인트를 추가하고 모든 출처를 허용합니다.")
	void registerStompEndpoints_void_success() {
		when(stompEndpointRegistry.addEndpoint(any(String.class)))
			.thenReturn(stompWebSocketEndpointRegistration);
		when(stompWebSocketEndpointRegistration.setAllowedOriginPatterns(any(String[].class)))
			.thenReturn(stompWebSocketEndpointRegistration);
		when(stompWebSocketEndpointRegistration.withSockJS())
			.thenReturn(sockJsServiceRegistration);

		// WHEN
		webSocketConfig.registerStompEndpoints(stompEndpointRegistry);

		// THEN
		verify(stompEndpointRegistry).addEndpoint(WEBSOCKET_ENDPOINT);
		verify(stompWebSocketEndpointRegistration).setAllowedOriginPatterns(ALLOWED_ORIGIN_PATTERN);
		verify(stompWebSocketEndpointRegistration).withSockJS();
	}

	@Test
	@DisplayName("CONFIGURE WEB SOCKET TRANSPORT(⭕️ SUCCESS): 메시지 크기, 전송 시간, 버퍼 크기 제한을 설정합니다.")
	void configureWebSocketTransport_void_success() {
		// WHEN
		webSocketConfig.configureWebSocketTransport(webSocketTransportRegistration);

		// THEN
		verify(webSocketTransportRegistration).setMessageSizeLimit(MESSAGE_SIZE_LIMIT);
		verify(webSocketTransportRegistration).setSendTimeLimit(SEND_TIME_LIMIT);
		verify(webSocketTransportRegistration).setSendBufferSizeLimit(SEND_BUFFER_SIZE_LIMIT);
	}
}
