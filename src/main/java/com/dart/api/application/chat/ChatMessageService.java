package com.dart.api.application.chat;

import static com.dart.global.common.util.ChatConstant.*;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dart.api.domain.chat.entity.ChatMessage;
import com.dart.api.domain.chat.entity.ChatRoom;
import com.dart.api.domain.chat.repository.ChatMessageRepository;
import com.dart.api.domain.chat.repository.ChatRedisRepository;
import com.dart.api.domain.chat.repository.ChatRoomRepository;
import com.dart.api.domain.member.entity.Member;
import com.dart.api.domain.member.repository.MemberRepository;
import com.dart.api.dto.chat.request.ChatMessageCreateDto;
import com.dart.api.dto.chat.request.ChatMessageSendDto;
import com.dart.api.dto.chat.response.ChatMessageReadDto;
import com.dart.api.dto.page.PageInfo;
import com.dart.api.dto.page.PageResponse;
import com.dart.global.error.exception.NotFoundException;
import com.dart.global.error.model.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatMessageService {

	private final ChatRoomRepository chatRoomRepository;
	private final MemberRepository memberRepository;
	private final ChatRedisRepository chatRedisRepository;
	private final ChatMessageRepository chatMessageRepository;

	@Transactional
	public void saveChatMessage(Long chatRoomId, ChatMessageCreateDto chatMessageCreateDto) {
		final ChatRoom chatRoom = getChatRoomById(chatRoomId);
		final Member member = getMemberByNickname(chatMessageCreateDto.sender());
		final ChatMessage chatMessage = ChatMessage.chatMessageFromCreateDto(chatRoom, member, chatMessageCreateDto);
		chatMessageRepository.save(chatMessage);

		final ChatMessageSendDto chatMessageSendDto = chatMessage.toChatMessageSendDto(CHAT_MESSAGE_EXPIRY_SECONDS);
		chatRedisRepository.saveChatMessage(chatMessageSendDto, member);
	}

	@Transactional(readOnly = true)
	public PageResponse<ChatMessageReadDto> getChatMessageList(Long chatRoomId, int page, int size) {
		final PageResponse<ChatMessageReadDto> redisChatMessageReadDtoList = chatRedisRepository.getChatMessageReadDto(
			chatRoomId, page, size);

		if (redisChatMessageReadDtoList != null && !redisChatMessageReadDtoList.pages().isEmpty()) {
			return createPageResponse(new ArrayList<>(redisChatMessageReadDtoList.pages()), page, size);
		}

		final List<ChatMessageReadDto> chatMessageReadDtoList = fetchChatMessagesFromDBAndCache(chatRoomId, page, size);
		return createPageResponse(chatMessageReadDtoList, page, size);
	}

	private ChatRoom getChatRoomById(Long chatRoomId) {
		return chatRoomRepository.findById(chatRoomId)
			.orElseThrow(() -> new NotFoundException(ErrorCode.FAIL_CHAT_ROOM_NOT_FOUND));
	}

	private Member getMemberByNickname(String nickname) {
		return memberRepository.findByNickname(nickname)
			.orElseThrow(() -> new NotFoundException(ErrorCode.FAIL_MEMBER_NOT_FOUND));
	}

	private List<ChatMessageReadDto> fetchChatMessagesFromDBAndCache(Long chatRoomId, int page, int size) {
		final ChatRoom chatRoom = getChatRoomById(chatRoomId);
		final Pageable pageable = PageRequest.of(page, size);
		final Page<ChatMessage> mySQLChatMessages =
			chatMessageRepository.findByChatRoomOrderByCreatedAtDesc(chatRoom, pageable);

		final List<ChatMessageReadDto> mySQLChatMessageReadDtoList = mySQLChatMessages.stream()
			.map(ChatMessage::toChatMessageReadDto)
			.toList();

		cachingChatMessages(chatRoom, mySQLChatMessageReadDtoList);

		return mySQLChatMessageReadDtoList;
	}

	private void cachingChatMessages(ChatRoom chatRoom, List<ChatMessageReadDto> chatMessageReadDtoList) {
		chatMessageReadDtoList.forEach(chatMessages -> {
			final Member member = getMemberByNickname(chatMessages.sender());
			final ChatMessage chatMessage = ChatMessage.chatMessageFromReadDto(chatRoom, member, chatMessages);
			final ChatMessageSendDto chatMessageSendDto = chatMessage.toChatMessageSendDto(CHAT_MESSAGE_EXPIRY_SECONDS);

			chatRedisRepository.saveChatMessage(chatMessageSendDto, member);
		});
	}

	private PageResponse<ChatMessageReadDto> createPageResponse(List<ChatMessageReadDto> chatMessageReadDtoList,
		int page, int size) {
		final boolean isDone = chatMessageReadDtoList.size() < size;
		final PageInfo pageInfo = new PageInfo(page, isDone);

		return new PageResponse<>(chatMessageReadDtoList, pageInfo);
	}
}
