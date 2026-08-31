package com.learning.hibernate.experiment;

import com.learning.hibernate.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lesson 2 - Persistence Context & Entity Lifecycle
 * Experiment 2: Persistence Context Identity / First-Level Cache
 *
 * Proves that, within a single Persistence Context, loading the same
 * database row twice via EntityManager.find() returns the exact same
 * managed Java object instance (user1 == user2), and that this guarantee
 * is scoped to that Persistence Context: a new @Transactional boundary
 * gets a new Persistence Context and therefore a different instance for
 * the same database row.
 */
@Component
public class PersistenceContextExperiment2 {

    private static final String KNOWN_USER_NAME = "Experiment 2 Known User";

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Setup step, deliberately separate from the experiment itself.
     * Runs in its own transaction/Persistence Context so that the
     * User it returns is NOT already managed when runExperiment()
     * later calls find() for the first time.
     */
    @Transactional
    public Long ensureKnownUser() {
        List<User> existing = entityManager
                .createQuery("select u from User u where u.name = :name", User.class)
                .setParameter("name", KNOWN_USER_NAME)
                .getResultList();

        if (!existing.isEmpty()) {
            Long id = existing.get(0).getId();
            System.out.println("[SETUP] Known user already exists, id = " + id);
            return id;
        }

        User seedUser = new User(KNOWN_USER_NAME);
        entityManager.persist(seedUser);
        entityManager.flush();

        System.out.println("[SETUP] Known user created, id = " + seedUser.getId());
        return seedUser.getId();
    }

    /**
     * The core experiment: two find() calls for the same ID, inside the
     * SAME Persistence Context (a single @Transactional method = a single
     * EntityManager for the duration of this call).
     */
    @Transactional
    public User runExperiment(Long knownUserId) {

        System.out.println("========================================");
        System.out.println("EXPERIMENT 2");
        System.out.println("Persistence Context Identity");
        System.out.println("========================================");
        System.out.println("All find() calls below happen inside ONE Persistence Context");
        System.out.println("(this single @Transactional method / EntityManager).");

        User user1 = entityManager.find(User.class, knownUserId);

        System.out.println("[1] First find(User.class, " + knownUserId + ")");
        System.out.println("    -> not yet known to this Persistence Context, so Hibernate");
        System.out.println("       issues a SELECT and stores the result as a managed entity.");
        System.out.println("user1 = " + user1);
        System.out.println("user1 id = " + user1.getId());
        System.out.println("user1 identity = " + System.identityHashCode(user1));

        User user2 = entityManager.find(User.class, knownUserId);

        System.out.println("[2] Second find(User.class, " + knownUserId + ")");
        System.out.println("    -> Hibernate first checks the Persistence Context for an");
        System.out.println("       entity already identified by (User.class, " + knownUserId + ").");
        System.out.println("       It finds user1 there and returns THAT SAME instance.");
        System.out.println("       No second SELECT is issued.");
        System.out.println("user2 = " + user2);
        System.out.println("user2 id = " + user2.getId());
        System.out.println("user2 identity = " + System.identityHashCode(user2));

        System.out.println("[3] Comparing references");
        System.out.println("user1 == user2 : " + (user1 == user2));

        System.out.println("[4] Comparing IDs");
        System.out.println("user1.getId().equals(user2.getId()) : " + user1.getId().equals(user2.getId()));

        // --- Stronger proof: mutate through one reference, observe through the other ---
        user1.setName("Changed Through User1");

        System.out.println("[5] user1.setName(\"Changed Through User1\") called");
        System.out.println("    Because user1 and user2 are literally the same object in");
        System.out.println("    memory, this change is visible through user2 as well -");
        System.out.println("    there is no separate copy to keep in sync.");
        System.out.println("user2 name = " + user2.getName());
        System.out.println("user1 == user2 : " + (user1 == user2));
        System.out.println("(Note: since user1 is still MANAGED in this Persistence Context,");
        System.out.println(" this name change will also be flushed as an UPDATE when this");
        System.out.println(" transaction commits - that is ordinary dirty checking, already");
        System.out.println(" covered in Experiment 1, and is not what this experiment is proving.)");

        System.out.println("----------------------------------------");
        System.out.println("First-Level Cache vs. Entity Identity vs. Persistence Context:");
        System.out.println("  First-Level Cache : the Persistence Context avoided a second");
        System.out.println("                      database read for the same identity.");
        System.out.println("  Entity Identity    : within this Persistence Context, the pair");
        System.out.println("                      (User.class, id=" + knownUserId + ") maps to exactly");
        System.out.println("                      ONE managed Java object instance.");
        System.out.println("  Persistence Context: is the thing that OWNS that identity map and");
        System.out.println("                      also tracks changes for dirty checking / flush -");
        System.out.println("                      it is more than just a read cache.");
        System.out.println("========================================");

        return user1;
    }

    /**
     * Negative experiment: a NEW @Transactional method gets a NEW
     * Persistence Context. Loading the same database row here proves the
     * identity guarantee above is scoped to a single Persistence Context,
     * not to the database row itself.
     */
    @Transactional
    public void runSecondPersistenceContextScenario(Long knownUserId, User user1) {

        System.out.println("========================================");
        System.out.println("EXPERIMENT 2 (b)");
        System.out.println("A different Persistence Context => a different instance");
        System.out.println("========================================");
        System.out.println("[6] This method is a NEW @Transactional boundary, so Spring/");
        System.out.println("    Hibernate hands us a brand new EntityManager, i.e. a brand");
        System.out.println("    new Persistence Context. user1's old Persistence Context is gone.");

        User user3 = entityManager.find(User.class, knownUserId);

        System.out.println("[7] find(User.class, " + knownUserId + ") in the NEW Persistence Context");
        System.out.println("    -> this Persistence Context has never seen this identity before,");
        System.out.println("       so Hibernate issues a fresh SELECT.");
        System.out.println("user3 = " + user3);
        System.out.println("user3 name = " + user3.getName() + "  (reflects the earlier committed change)");
        System.out.println("user3 identity = " + System.identityHashCode(user3));

        System.out.println("[8] Cross-Persistence-Context comparison");
        System.out.println("user1 == user3 : " + (user1 == user3));
        System.out.println("user1.getId().equals(user3.getId()) : " + user1.getId().equals(user3.getId()));

        System.out.println("CONCLUSION: same database row (same id, same data), but a");
        System.out.println("DIFFERENT managed Java object instance, because the identity");
        System.out.println("guarantee only holds within a single Persistence Context.");
        System.out.println("========================================");
    }
}
