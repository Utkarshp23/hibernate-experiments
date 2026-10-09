package com.learning.hibernate.controller;

import com.learning.hibernate.dto.UserSummaryDto;
import com.learning.hibernate.dto.UserWithPostsDto;
import com.learning.hibernate.service.UserService;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<UserSummaryDto> getUsers() {
        return userService.getAllUsers();
    }

    @GetMapping("/posts")
    public List<UserWithPostsDto> getUsersWithPosts() {
        return userService.getAllUsersWithPosts();
    }

    @GetMapping("/entity")
    public List<UserWithPostsDto> getUserWithPostsUsingEntity() {
        return userService.getAllUsersWithPostsUsingEntity();
    }

    @GetMapping("/paged")
    public Page<UserWithPostsDto> getUsersPagedWithPosts(Pageable pageable) {
        return userService.getUsersPagedWithPosts(pageable);
    }

}
