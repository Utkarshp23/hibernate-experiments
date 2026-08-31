package com.learning.hibernate.experiment;

import com.learning.hibernate.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lesson 2 - Persistence Context & Entity Lifecycle
 * Experiment 4: Detached Entity
 *
 * Proves that once an entity is DETACHED from the Persistence Context,
 * Hibernate no longer dirty-checks it: further changes made to that Java
 * object are simply invisible to Hibernate and produce NO SQL at flush().
 *
 *   MANAGED                          MANAGED
 *      |                                |
 *   setName()                       detach()
 *      |                                |
 *   flush()                         DETACHED
 *      |                                |
 *   UPDATE (SQL)                    setName()
 *                                       |
 *                                    flush()
 *                                       |
 *                                   NO UPDATE
 *
 * Managed:   Hibernate is responsible for tracking the entity. State
 *            changes on a managed instance are picked up by dirty checking
 *            and written out at flush() time.
 *
 * Detached:  Hibernate is no longer tracking that entity INSTANCE. State
 *            changes on it are not compared against anything, because
 *            there is nothing in the Persistence Context to compare them
 *            to - the instance was removed from the identity map that
 *            dirty checking walks.
 *
 * IMPORTANT: detached does NOT mean "Hibernate forgets the database row".
 * Hibernate still knows perfectly well that a row with that id exists; it
 * simply no longer manages this particular Java object as a live view onto
 * that row. The Java object itself is untouched by detach() - its fields
 * still hold whatever was last set on them - it is only its RELATIONSHIP
 * to this Persistence Context that changes.
 *
 * This experiment deliberately never calls merge()/persist()/remove() on
 * the detached instance - reattaching it is Experiment 5's job. Here we
 * only want to observe what happens when a detached entity is left alone.
 */
@Component
public class DetachedEntityExperiment4 {

    private static final String KNOWN_USER_NAME = "Experiment 4 Detached User";

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Setup step, deliberately separate from the scenarios below. Runs in
     * its own transaction/Persistence Context. Because scenarioA below
     * legitimately commits a name change (that is the whole point of the
     * control scenario), this method resets the row back to a deterministic
     * baseline name on every run, instead of creating a new row each time -
     * so repeated runs stay understandable and the users table does not
     * accumulate throwaway rows.
     */
    @Transactional
    public Long ensureKnownUser() {
        List<User> existing = entityManager
                .createQuery("select u from User u where u.name like :namePattern order by u.id asc", User.class)
                .setParameter("namePattern", "Experiment 4%")
                .setMaxResults(1)
                .getResultList();

        if (!existing.isEmpty()) {
            User user = existing.get(0);
            user.setName(KNOWN_USER_NAME);
            entityManager.flush();
            System.out.println("[SETUP] Known user reset to baseline, id = " + user.getId());
            return user.getId();
        }

        User seedUser = new User(KNOWN_USER_NAME);
        entityManager.persist(seedUser);
        entityManager.flush();

        System.out.println("[SETUP] Known user created, id = " + seedUser.getId());
        return seedUser.getId();
    }

    /**
     * Scenario A - MANAGED CONTROL.
     * Baseline: a plain managed entity, modified, then flushed. This is the
     * "normal" case dirty checking already covered in Experiment 3 - it is
     * repeated here only so Scenario B has something concrete to contrast
     * against in the same run.
     */
    @Transactional
    public void scenarioA(Long knownUserId) {

        System.out.println("========================================");
        System.out.println("EXPERIMENT 4");
        System.out.println("DETACHED ENTITY");
        System.out.println("========================================");
        System.out.println();
        System.out.println("SCENARIO A - MANAGED CONTROL");
        System.out.println("----------------------------------------");

        User user = entityManager.find(User.class, knownUserId);

        System.out.println("[1] Entity loaded");
        System.out.println("    State: MANAGED");
        System.out.println("    entityManager.contains(user): " + entityManager.contains(user));
        System.out.println("    Name: " + user.getName());

        user.setName("Experiment 4 Managed Change");

        System.out.println("[2] user.setName(\"Experiment 4 Managed Change\") called");
        System.out.println("    Name in Java object: " + user.getName());

        System.out.println("[3] Calling flush()");
        System.out.println("    >>> entity is MANAGED, so dirty checking should produce an UPDATE <<<");

        entityManager.flush();

        System.out.println("[4] flush() completed");
        System.out.println("    UPDATE occurred because the entity was MANAGED throughout.");
        System.out.println("    (See the Hibernate UPDATE statement logged above this line.)");
    }

    /**
     * Scenario B - DETACHED.
     * The entity starts MANAGED exactly like Scenario A, but is explicitly
     * detached before it is modified. Only ONE User instance exists in this
     * method - it is never re-managed, merged, persisted, or removed.
     */
    @Transactional
    public void scenarioB(Long knownUserId) {

        System.out.println();
        System.out.println("SCENARIO B - DETACHED");
        System.out.println("----------------------------------------");

        User user = entityManager.find(User.class, knownUserId);

        System.out.println("[1] Entity loaded");
        System.out.println("    State: MANAGED");
        System.out.println("    entityManager.contains(user): " + entityManager.contains(user));
        System.out.println("    Name: " + user.getName());

        entityManager.detach(user);

        System.out.println("[2] entityManager.detach(user) called");
        System.out.println("    State: DETACHED");
        System.out.println("    entityManager.contains(user): " + entityManager.contains(user));
        System.out.println("    NOTE: this is the SAME Java object as before - detach() did not");
        System.out.println("    destroy it or touch its fields. Only its relationship to this");
        System.out.println("    Persistence Context changed: Hibernate removed it from the set");
        System.out.println("    of instances it dirty-checks at flush() time.");

        user.setName("Changed While Detached");

        System.out.println("[3] user.setName(\"Changed While Detached\") called while DETACHED");
        System.out.println("    Name in Java object: " + user.getName());
        System.out.println("    This is a plain field assignment on a plain Java object at this");
        System.out.println("    point - Hibernate has no instance in its Persistence Context to");
        System.out.println("    compare this against, so there is nothing for it to dirty-check.");

        System.out.println("[4] Calling flush()");
        System.out.println("    >>> entity is DETACHED, so this change should produce NO SQL <<<");

        entityManager.flush();

        System.out.println("[5] flush() completed");
        System.out.println("    Expected: NO UPDATE SQL for \"Changed While Detached\" above this line.");
        System.out.println("    In-memory (detached) name : " + user.getName());
    }

    /**
     * Verifies the database value using a brand new Persistence Context,
     * deliberately kept in its own @Transactional method so it does not
     * share an EntityManager/transaction with scenarioB. This is a plain
     * read - no merge()/persist() of the detached instance from scenarioB
     * happens anywhere, so this find() cannot be influenced by it.
     */
    @Transactional
    public void verifyDatabaseState(Long knownUserId, String detachedInMemoryName) {

        System.out.println();
        System.out.println("[6] Database verification (NEW Persistence Context)");

        User freshUser = entityManager.find(User.class, knownUserId);

        System.out.println("    Database name           : " + freshUser.getName());
        System.out.println("    Detached object name was: " + detachedInMemoryName);

        if (freshUser.getName().equals(detachedInMemoryName)) {
            System.out.println("    UNEXPECTED: database matches the detached in-memory change.");
        } else {
            System.out.println("    CONFIRMED: the detached change never reached the database.");
            System.out.println("    Java object state and database state have diverged, because");
            System.out.println("    the object that changed was no longer managed by anything.");
        }
        System.out.println("========================================");
    }
}
