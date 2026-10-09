package com.learning.hibernate.repository;

import com.learning.hibernate.entity.User;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<User, Long> {

    // findAll() (inherited) is the plain case: one query, posts left untouched and LAZY.

    // join fetch: one query, posts eagerly populated for this result only.
    @Query("select u from User u left join fetch u.posts order by u.id")
    List<User> findAllWithPosts();

    @EntityGraph(attributePaths = "posts")
    @Query("select u from User u order by u.id")
    List<User> findAllWithPostsUsingEntity();
}
