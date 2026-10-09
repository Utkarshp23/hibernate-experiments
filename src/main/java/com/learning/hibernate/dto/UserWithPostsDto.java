package com.learning.hibernate.dto;

import java.util.List;

public record UserWithPostsDto(Long id, String name, List<PostDto> posts) {
}
