package com.learning.hibernate.experiment;

import com.learning.hibernate.entity.Post;
import com.learning.hibernate.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceUnit;
import java.util.List;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lesson 3 - The N+1 Select Problem
 * Experiment 6: One query to load parents, N more to load their children
 *
 * SITUATION
 * ---------
 * We have Users, each with several Posts (a lazy @OneToMany). We load a
 * list of Users with ONE query, then - in a perfectly ordinary-looking
 * loop - ask each User for their posts. Because the collection is LAZY,
 * each access that hasn't been initialized yet fires its OWN "select *
 * from posts where author_id = ?" straight at the database.
 *
 *   1 query  -> select the N users
 *   N queries -> one per user, to lazily load that user's posts
 *   ---------
 *   1 + N queries total, where a single well-written query would do.
 *
 * This is deliberately NOT a Hibernate bug: it's the honest, predictable
 * consequence of combining "load parents" with "lazily touch each
 * parent's collection in a loop." It gets worse linearly with the number
 * of parent rows - fine with 5 users in a demo, catastrophic with 5,000
 * users in production.
 *
 * We don't eyeball the console log to count queries - Hibernate's
 * Statistics API gives us an authoritative number (see
 * spring.jpa.properties.hibernate.generate_statistics=true in
 * application.properties), so there's no ambiguity about what actually
 * hit the database.
 */
@Component
public class NPlusOneExperiment6 {

    private static final int USER_COUNT = 4;
    private static final int POSTS_PER_USER = 3;

    @PersistenceContext
    private EntityManager entityManager;

    @PersistenceUnit
    private EntityManagerFactory entityManagerFactory;

    /**
     * Setup step, deliberately separate from the experiment itself. Wipes
     * any previous run's "N+1 Experiment%" users (and their posts, via
     * cascade-free explicit delete) and creates a fresh, deterministic set:
     * USER_COUNT users, each owning POSTS_PER_USER posts.
     */
    @Transactional
    public List<Long> ensureKnownUsers() {

        entityManager.createQuery(
                        "delete from Post p where p.author.id in (select u.id from User u where u.name like :namePattern)")
                .setParameter("namePattern", "N+1 Experiment%")
                .executeUpdate();
        entityManager.createQuery("delete from User u where u.name like :namePattern")
                .setParameter("namePattern", "N+1 Experiment%")
                .executeUpdate();

        List<Long> userIds = new java.util.ArrayList<>();
        for (int i = 1; i <= USER_COUNT; i++) {
            User user = new User("N+1 Experiment User " + i);
            entityManager.persist(user);
            for (int p = 1; p <= POSTS_PER_USER; p++) {
                entityManager.persist(new Post("Post " + p + " by User " + i, user));
            }
            userIds.add(user.getId());
        }
        entityManager.flush();

        System.out.println("[SETUP] Created " + USER_COUNT + " users, each with " + POSTS_PER_USER + " posts.");
        System.out.println("[SETUP] User ids: " + userIds);
        return userIds;
    }

    /**
     * THE PROBLEM. One query loads the users. The loop below looks
     * completely innocent - it's just reading a property - but
     * user.getPosts() on a LAZY, not-yet-initialized collection triggers
     * a brand new SELECT, every single time, for every single user.
     */
    @Transactional
    public void demonstrateNPlusOneProblem(List<Long> userIds) {

        Statistics statistics = sessionFactory().getStatistics();
        statistics.clear();

        System.out.println("========================================");
        System.out.println("EXPERIMENT 6 - THE N+1 PROBLEM");
        System.out.println("========================================");

        System.out.println();
        System.out.println("[1] Loading all users with ONE query (no join, posts is LAZY)...");

        List<User> users = entityManager
                .createQuery("select u from User u where u.id in :ids order by u.id", User.class)
                .setParameter("ids", userIds)
                .getResultList();

        System.out.println("    Users loaded: " + users.size());
        System.out.println("    Queries so far (Hibernate Statistics): "
                + statistics.getPrepareStatementCount());

        System.out.println();
        System.out.println("[2] Now looping over users and reading user.getPosts().size()...");
        System.out.println("    Watch the SQL log below: a NEW select fires on EVERY iteration.");
        System.out.println();

        for (User user : users) {
            System.out.println("    >>> about to touch posts for: " + user.getName());
            int postCount = user.getPosts().size(); // <-- lazy init fires here, one SELECT per user
            System.out.println("        " + user.getName() + " has " + postCount + " posts"
                    + "  (queries so far: " + statistics.getPrepareStatementCount() + ")");
        }

        System.out.println();
        System.out.println("[3] Final tally, straight from Hibernate's own Statistics API:");
        System.out.println("    Total SQL statements prepared : " + statistics.getPrepareStatementCount());
        System.out.println("    Entity load count              : " + statistics.getEntityLoadCount());
        System.out.println("    Collection load count          : " + statistics.getCollectionLoadCount());
        System.out.println();
        System.out.println("    Expected if this were optimal  : 1 query");
        System.out.println("    Users loaded                   : " + users.size());
        System.out.println("    Expected under N+1             : 1 + " + users.size()
                + " = " + (1 + users.size()) + " queries");
        System.out.println();
        System.out.println("========================================");
        System.out.println("This is the N+1 problem: 1 query for the parents, plus N more,");
        System.out.println("one per parent, to lazily fetch each one's children.");
        System.out.println("========================================");
    }

    @Transactional 
    public void demonstrateJoinFetch(){
        Statistics statistics = sessionFactory().getStatistics();
        statistics.clear();

        List<User> users = entityManager.createQuery("select u from User u join fetch u.posts", User.class).getResultList();
        System.out.println("    Users loaded: " + users.size());
        System.out.println("    Queries so far (Hibernate Statistics): "
                + statistics.getPrepareStatementCount());

        for (User user : users) {
            System.out.println("    >>> about to touch posts for: " + user.getName());
            int postCount = user.getPosts().size(); // <-- lazy init fires here, one SELECT per user
            System.out.println("        " + user.getName() + " has " + postCount + " posts"
                    + "  (queries so far: " + statistics.getPrepareStatementCount() + ")");
        }

        System.out.println();
        System.out.println("[3] Final tally, straight from Hibernate's own Statistics API:");
        System.out.println("    Total SQL statements prepared : " + statistics.getPrepareStatementCount());
        System.out.println("    Entity load count              : " + statistics.getEntityLoadCount());
        System.out.println("    Collection load count          : " + statistics.getCollectionLoadCount());
        System.out.println();
        System.out.println("    Expected if this were optimal  : 1 query");
        System.out.println("    Users loaded                   : " + users.size());
        System.out.println("    Expected under N+1             : 1 + " + users.size()
                + " = " + (1 + users.size()) + " queries");

    }

    private SessionFactory sessionFactory() {
        return entityManagerFactory.unwrap(SessionFactory.class);
    }
}
