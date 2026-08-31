package com.learning.hibernate.experiment;

import com.learning.hibernate.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lesson 2 - Persistence Context & Entity Lifecycle
 * Experiment 5: merge() - Detached -> Managed Copy
 *
 * Experiment 4 proved that a DETACHED entity is no longer dirty-checked.
 * This experiment proves how you get back to a synchronized state, and -
 * more importantly - clears up the most common misconception about how:
 *
 *   MISCONCEPTION: "merge() reattaches the object you pass it."
 *   REALITY:       merge() copies the state of the given (detached) entity
 *                  onto a MANAGED instance and returns THAT instance. The
 *                  object you passed in is still detached afterward.
 *
 *       detachedUser                         detachedUser
 *            |                                     |
 *            | merge()                             X   (stays DETACHED,
 *            v                                         forever unmanaged
 *      Persistence Context                             by this call)
 *            |
 *            v
 *       managedUser   <-- a DIFFERENT Java object, same database identity
 *            |
 *            | flush()
 *            v
 *          UPDATE
 *
 * This is deliberately the mirror image of Experiment 4: Experiment 4
 * showed that modifying a detached instance is invisible to Hibernate;
 * this experiment shows that merge() does not fix that by "waking up"
 * the same instance - it hands you a different, managed one.
 */
@Component
public class MergeExperiment5 {

    private static final String KNOWN_USER_NAME = "Experiment 5 Merge User";

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Setup step, deliberately separate from the experiment itself and run
     * in its own Persistence Context. Resets the row back to a deterministic
     * baseline name on every run (rather than inserting a new row each time)
     * so repeated runs stay understandable. In practice this project's
     * ddl-auto=create already gives every application startup a clean
     * schema, so the reset path mainly matters if that setting ever changes.
     */
    @Transactional
    public Long ensureKnownUser() {
        List<User> existing = entityManager
                .createQuery("select u from User u where u.name like :namePattern order by u.id asc", User.class)
                .setParameter("namePattern", "Experiment 5%")
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
     * The core experiment. Everything happens inside one Persistence
     * Context/transaction so that contains() checks are meaningful against
     * the same EntityManager throughout.
     */
    @Transactional
    public void runExperiment(Long knownUserId) {

        System.out.println("========================================");
        System.out.println("EXPERIMENT 5");
        System.out.println("MERGE: DETACHED -> MANAGED");
        System.out.println("========================================");

        User detachedUser = entityManager.find(User.class, knownUserId);

        System.out.println();
        System.out.println("[1] Entity loaded");
        System.out.println("    State: MANAGED");
        System.out.println("    contains(detachedUser): " + entityManager.contains(detachedUser));
        System.out.println("    ID: " + detachedUser.getId());
        System.out.println("    Name: " + detachedUser.getName());

        entityManager.detach(detachedUser);

        System.out.println();
        System.out.println("[2] detach() called");
        System.out.println("    State: DETACHED");
        System.out.println("    contains(detachedUser): " + entityManager.contains(detachedUser));

        detachedUser.setName("Changed Through Detached Object");

        System.out.println();
        System.out.println("[3] Detached entity modified");
        System.out.println("    detachedUser.name: " + detachedUser.getName());
        System.out.println("    (Per Experiment 4, this change alone would never reach the");
        System.out.println("    database - no flush is attempted here to prove that again.)");

        System.out.println();
        System.out.println("[4] Calling merge(detachedUser)...");

        User managedUser = entityManager.merge(detachedUser);

        System.out.println();
        System.out.println("[5] merge() returned");
        System.out.println("    detachedUser == managedUser : " + (detachedUser == managedUser));
        System.out.println();
        System.out.println("    detachedUser identity: " + System.identityHashCode(detachedUser));
        System.out.println("    managedUser identity : " + System.identityHashCode(managedUser));
        System.out.println();
        System.out.println("    detachedUser ID: " + detachedUser.getId());
        System.out.println("    managedUser ID : " + managedUser.getId());
        System.out.println();
        System.out.println("    contains(detachedUser): " + entityManager.contains(detachedUser));
        System.out.println("    contains(managedUser) : " + entityManager.contains(managedUser));
        System.out.println();
        System.out.println("    IMPORTANT:");
        System.out.println("    merge() did NOT reattach detachedUser. It copied detachedUser's");
        System.out.println("    state onto a managed instance and returned THAT instance.");
        System.out.println("    The original object remains DETACHED, permanently, from this point on.");

        detachedUser.setName("Changed AGAIN On Original Detached Object");

        System.out.println();
        System.out.println("[6] Modify the ORIGINAL detached object again, AFTER merge()");
        System.out.println("    detachedUser.name: " + detachedUser.getName());
        System.out.println("    managedUser.name : " + managedUser.getName());
        System.out.println("    managedUser did not change - it is a different object, and it is");
        System.out.println("    the only one Hibernate is tracking. detachedUser is still just an");
        System.out.println("    ordinary, unmanaged Java object as far as this Persistence Context");
        System.out.println("    is concerned.");

        System.out.println();
        System.out.println("[7] Calling flush()");
        System.out.println("    >>> Only managedUser is MANAGED, so dirty checking synchronizes");
        System.out.println("        managedUser's state - \"Changed Through Detached Object\" -");
        System.out.println("        not detachedUser's later, untracked change. <<<");

        entityManager.flush();

        System.out.println();
        System.out.println("    flush() completed");
        System.out.println("    Expected UPDATE value: Changed Through Detached Object");
    }

    /**
     * Verifies the database value using a brand new Persistence Context,
     * kept in its own @Transactional method so it does not share an
     * EntityManager/transaction with runExperiment(). merge() is not
     * called again here - this is a plain read of what actually landed
     * in PostgreSQL.
     */
    @Transactional
    public void verifyDatabaseState(Long knownUserId) {

        System.out.println();
        System.out.println("[8] Database verification (NEW Persistence Context)");

        User freshUser = entityManager.find(User.class, knownUserId);

        System.out.println("    Database name: " + freshUser.getName());

        System.out.println();
        System.out.println("========================================");
        System.out.println("CONCLUSION");
        System.out.println("========================================");
        System.out.println("merge() did NOT make the original detached object managed.");
        System.out.println("merge() returned a DIFFERENT managed instance.");
        System.out.println();
        System.out.println("detachedUser == managedUser -> false");
        System.out.println("contains(detachedUser)      -> false");
        System.out.println("contains(managedUser)       -> true");
        System.out.println();
        System.out.println("persist(): the SAME object you pass in becomes managed.");
        System.out.println("merge():   the object you pass in stays detached; a DIFFERENT,");
        System.out.println("           managed copy carrying its state is returned instead.");
        System.out.println("========================================");
    }
}
