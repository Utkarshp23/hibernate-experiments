package com.learning.hibernate.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.BatchSize;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    // LAZY is the JPA default for @OneToMany anyway - spelled out here
    // because this collection is the whole point of the N+1 experiment.
    //
    // @BatchSize only matters once the collection is actually lazily
    // initialized (e.g. in the paginated flow, where join fetch isn't an
    // option - see UserService.getUsersPagedWithPosts). Instead of one
    // "where author_id = ?" per user, Hibernate groups up to `size` pending
    // owner ids into one "where author_id in (?, ?, ?)" per batch.
    @OneToMany(mappedBy = "author", fetch = FetchType.LAZY)
    @BatchSize(size = 3)
    private List<Post> posts = new ArrayList<>();

    public User() {
    }

    public User(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Post> getPosts() {
        return posts;
    }

    public void setPosts(List<Post> posts) {
        this.posts = posts;
    }
}
