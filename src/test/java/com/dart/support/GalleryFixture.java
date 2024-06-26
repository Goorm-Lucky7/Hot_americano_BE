package com.dart.support;

import java.time.LocalDateTime;
import java.util.List;

import com.dart.api.domain.gallery.entity.Cost;
import com.dart.api.domain.gallery.entity.Gallery;
import com.dart.api.domain.member.entity.Member;
import com.dart.api.dto.gallery.request.CreateGalleryDto;
import com.dart.api.dto.gallery.request.ImageInfoDto;

public class GalleryFixture {

	public static Gallery createGalleryEntity() {
		return Gallery.create(
			createGalleryEntityForCreateGalleryDto(),
			"https://example.com/thumbnail.jpg",
			Cost.FREE,
			MemberFixture.createMemberEntity()
		);
	}

	public static Gallery createGalleryEntity(Member member) {
		return Gallery.builder()
			.title("D'ART Gallery")
			.content("This is D'ART Gallery")
			.thumbnail("https://example.com/thumbnail.jpg")
			.startDate(LocalDateTime.now())
			.endDate(LocalDateTime.now().plusDays(10))
			.cost(Cost.FREE)
			.fee(0)
			.member(member)
			.build();
	}

	public static CreateGalleryDto createGalleryEntityForCreateGalleryDto() {
		return CreateGalleryDto.builder()
			.title("D'ART Gallery")
			.content("This is D'ART Gallery")
			.startDate(LocalDateTime.now())
			.endDate(LocalDateTime.now().plusDays(10))
			.fee(1000)
			.generatedCost(0)
			.hashtags(List.of("happy", "good", "excellent"))
			.informations(List.of(
				new ImageInfoDto("image1.jpg", "Image 1"),
				new ImageInfoDto("image2.jpg", "Image 2"))
			)
			.build();
	}
}
