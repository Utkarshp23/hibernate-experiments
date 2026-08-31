package com.learning.hibernate.experiment;

import com.learning.hibernate.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lesson 2 - Persistence Context & Entity Lifecycle
 * Experiment 3: Dirty Checking
 *
 * Proves that Hibernate automatically detects changes made to a MANAGED
 * entity and synchronizes them with the database at flush() time, with no
 * explicit update()/save()/merge() call anywhere - and, just as importantly,
 * that an unmodified managed entity produces NO update at flush().
 *
 * Simplified mental model (conceptual - not a claim about one exact
 * implementation mechanism across every Hibernate version/configuration):
 *
 *   find() loads the entity, and Hibernate keeps enough information about
 *   its state at load time to later tell whether it changed.
 *
 *       User#1 name = "Original"           <- state Hibernate remembers
 *              |
 *              | user.setName("Modified")   (plain Java setter call)
 *              v
 *       User#1 name = "Modified"           <- current in-memory state
 *              |
 *              | flush()
 *              v
 *   Conceptually, Hibernate compares the current managed state with its
 *   previously known state during dirty checking. A real difference
 *   produces an UPDATE; no difference produces no SQL at all.
 *
 * This is deliberately separate from Experiment 2 (Persistence Context
 * Identity / First-Level Cache): that experiment was about *which Java
 * object instance* you get back for a given identity, this one is about
 * whether *its state changed* since it was loaded.
 *
 * flush() vs. commit(): every scenario below calls entityManager.flush()
 * explicitly so the synchronization boundary is visible in isolation.
 * flush() only synchronizes the Persistence Context with the database
 * (sends pending SQL); it does not end the transaction. commit() - which
 * Spring still performs automatically when each @Transactional method
 * returns - is what finalizes that transaction. A flush without a commit
 * can still be rolled back.
 */
@Component
public class DirtyCheckingExperiment3 {

    private static final String KNOWN_USER_NAME = "Experiment 3 Dirty Checking User";

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Setup step, deliberately separate from the scenarios below and run
     * only once. Runs in its own transaction/Persistence Context so each
     * scenario afterward starts with a clean, cold find().
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
     * Scenario A: a MANAGED entity is modified via a plain setter, then
     * flush() is called explicitly. No update()/save()/merge() call exists
     * anywhere in this method - flush() alone triggers the UPDATE.
     */
    @Transactional
    public void scenarioA(Long knownUserId) {

        System.out.println("========================================");
        System.out.println("EXPERIMENT 3");
        System.out.println("DIRTY CHECKING");
        System.out.println("========================================");
        System.out.println();
        System.out.println("SCENARIO A - ENTITY MODIFIED");
        System.out.println("----------------------------------------");

        User user = entityManager.find(User.class, knownUserId);

        System.out.println("[1] Entity loaded");
        System.out.println("    State: MANAGED");
        System.out.println("    Name before modification: " + user.getName());

        user.setName("Modified By Dirty Checking");

        System.out.println("[2] user.setName(\"Modified By Dirty Checking\") called");
        System.out.println("    Name after modification: " + user.getName());
        System.out.println("    NOTE: no update()/save()/merge() call was made -");
        System.out.println("    this is a plain setter on a MANAGED object.");

        System.out.println("[3] No update method called. Calling flush()...");
        System.out.println("    >>> Hibernate should now perform dirty checking <<<");

        entityManager.flush();

        System.out.println("[4] flush() completed");
        System.out.println("    (See the Hibernate UPDATE statement logged above this line.)");
        System.out.println("========================================");
    }

    /**
     * Scenario B: a MANAGED entity is loaded and left untouched, then
     * flush() is called. Being MANAGED is not, by itself, a reason for
     * Hibernate to write anything - flush() should produce no SQL here.
     */
    @Transactional
    public void scenarioB(Long knownUserId) {

        System.out.println();
        System.out.println("SCENARIO B - ENTITY NOT MODIFIED");
        System.out.println("----------------------------------------");

        User user = entityManager.find(User.class, knownUserId);

        System.out.println("[1] Entity loaded");
        System.out.println("    State: MANAGED");
        System.out.println("    Name: " + user.getName());

        System.out.println("[2] No changes made");

        System.out.println("[3] Calling flush()");
        System.out.println("    >>> Hibernate performs synchronization,");
        System.out.println("        but there is no changed state to synchronize. <<<");

        entityManager.flush();

        System.out.println("[4] flush() completed");
        System.out.println("    Expected: NO update SQL (check the log above - there should");
        System.out.println("    be no 'update users' statement between steps [3] and [4]).");
        System.out.println("========================================");
    }

    /**
     * Scenario C: the entity is changed and then changed back to its
     * original value within the same Persistence Context, before flush().
     * Demonstrates that dirty checking is concerned with the resulting
     * state at flush time, not with whether a setter was called.
     */
    @Transactional
    public void scenarioC(Long knownUserId) {

        System.out.println();
        System.out.println("SCENARIO C - MODIFIED, THEN REVERTED");
        System.out.println("----------------------------------------");

        User user = entityManager.find(User.class, knownUserId);
        String originalName = user.getName();

        System.out.println("[1] Entity loaded");
        System.out.println("    State: MANAGED");
        System.out.println("    Name: " + originalName);

        user.setName("Temporary Change");
        System.out.println("[2] user.setName(\"Temporary Change\") called");
        System.out.println("    Current in-memory name: " + user.getName());

        user.setName(originalName);
        System.out.println("[3] user.setName(\"" + originalName + "\") called again");
        System.out.println("    Current in-memory name is back to: " + user.getName());
        System.out.println("    Final state == state at load time.");

        System.out.println("[4] Calling flush()");
        System.out.println("    >>> Dirty checking compares the FINAL managed state against");
        System.out.println("        the previously known state, not each intermediate setter call. <<<");

        entityManager.flush();

        System.out.println("[5] flush() completed");
        System.out.println("    Expected: NO meaningful update SQL, because the resulting");
        System.out.println("    state is identical to the state Hibernate already had on record.");
        System.out.println("========================================");
    }
}
