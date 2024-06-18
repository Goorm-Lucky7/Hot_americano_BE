package com.dart.api.infrastructure.redis;

import static com.dart.global.common.util.RedisConstant.*;

import java.util.List;

import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.listener.KeyExpirationEventMessageListener;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.dart.api.application.chat.ChatMessageArchiveService;
import com.dart.api.application.gallery.ImageService;
import com.dart.api.domain.chat.entity.ChatRoom;
import com.dart.api.domain.chat.repository.ChatRoomRepository;
import com.dart.api.domain.gallery.entity.Gallery;
import com.dart.api.domain.gallery.entity.Hashtag;
import com.dart.api.domain.gallery.repository.GalleryRepository;
import com.dart.api.domain.gallery.repository.HashtagRepository;
import com.dart.global.error.exception.NotFoundException;
import com.dart.global.error.model.ErrorCode;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Transactional
public class RedisKeyExpirationListener extends KeyExpirationEventMessageListener {

	private final GalleryRepository galleryRepository;
	private final ImageService imageService;
	private final HashtagRepository hashtagRepository;
	private final ChatRoomRepository chatRoomRepository;
	private final ChatMessageArchiveService chatMessageArchiveService;

	public RedisKeyExpirationListener(
		RedisMessageListenerContainer listenerContainer,
		GalleryRepository galleryRepository,
		ImageService imageService,
		HashtagRepository hashtagRepository,
		ChatRoomRepository chatRoomRepository,
		ChatMessageArchiveService chatMessageArchiveService
	) {
		super(listenerContainer);
		this.galleryRepository = galleryRepository;
		this.imageService = imageService;
		this.hashtagRepository = hashtagRepository;
		this.chatRoomRepository = chatRoomRepository;
		this.chatMessageArchiveService = chatMessageArchiveService;
	}

	@Override
	public void onMessage(Message message, byte[] pattern) {
		final String expiredKey = message.toString();
		log.info("[✅ LOGGER] REDIS KEY EXPIRED: {}", expiredKey);

		if (isPaymentKey(expiredKey)) {
			final Long galleryId = Long.parseLong(expiredKey.replace(REDIS_PAYMENT_PREFIX, ""));
			handleExpiredGallery(galleryId);
		}

		chatMessageArchiveService.handleRedisExpiredEvent(expiredKey);
	}

	public void handleExpiredGallery(Long galleryId) {
		final Gallery gallery = galleryRepository.findById(galleryId)
			.orElseThrow(() -> new NotFoundException(ErrorCode.FAIL_GALLERY_NOT_FOUND));
		final List<Hashtag> hashtags = hashtagRepository.findByGallery(gallery);
		final ChatRoom chatRoom = chatRoomRepository.findByGallery(gallery)
			.orElseThrow(() -> new NotFoundException(ErrorCode.FAIL_CHAT_ROOM_NOT_FOUND));

		chatRoomRepository.delete(chatRoom);
		imageService.deleteImagesByGallery(gallery);
		imageService.deleteThumbnail(gallery);
		hashtagRepository.deleteAll(hashtags);
		galleryRepository.delete(gallery);
	}

	private boolean isPaymentKey(String str) {
		return str.contains(REDIS_PAYMENT_PREFIX);
	}
}
