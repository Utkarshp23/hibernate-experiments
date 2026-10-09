package com.learning.hibernate.service;

import com.learning.hibernate.dto.PostDto;
import com.learning.hibernate.dto.UserSummaryDto;
import com.learning.hibernate.dto.UserWithPostsDto;
import com.learning.hibernate.repository.UserRepository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Normal lazy flow: one query, posts never touched, so the LAZY
    // collection is simply never initialized.
    @Transactional(readOnly = true)
    public List<UserSummaryDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(u -> new UserSummaryDto(u.getId(), u.getName()))
                .toList();
    }

    // join fetch flow: one query, posts pre-populated, so mapping
    // u.getPosts() below does not trigger any additional SELECTs.
    @Transactional(readOnly = true)
    public List<UserWithPostsDto> getAllUsersWithPosts() {
        return userRepository.findAllWithPosts().stream()
                .map(u -> new UserWithPostsDto(
                        u.getId(),
                        u.getName(),
                        u.getPosts().stream()
                                .map(p -> new PostDto(p.getId(), p.getTitle()))
                                .toList()))
                .toList();
    }

     @Transactional(readOnly = true)
    public List<UserWithPostsDto> getAllUsersWithPostsUsingEntity() {
        return userRepository.findAllWithPostsUsingEntity().stream()
                .map(u -> new UserWithPostsDto(
                        u.getId(),
                        u.getName(),
                        u.getPosts().stream()
                                .map(p -> new PostDto(p.getId(), p.getTitle()))
                                .toList()))
                .toList();
    }

    // Paginated flow: join fetch + LIMIT/OFFSET don't mix (a fetch-joined
    // collection multiplies rows, so Hibernate can't paginate the SQL
    // correctly and either errors or pages in memory). So this page of
    // users is loaded with a plain query, posts stay LAZY, and
    // u.getPosts() below triggers lazy loads - but @BatchSize(size = 3) on
    // User.posts groups those into "where author_id in (?,?,?)" batches
    // instead of one query per user.
    @Transactional(readOnly = true)
    public Page<UserWithPostsDto> getUsersPagedWithPosts(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(u -> new UserWithPostsDto(
                        u.getId(),
                        u.getName(),
                        u.getPosts().stream()
                                .map(p -> new PostDto(p.getId(), p.getTitle()))
                                .toList()));
    }
}
