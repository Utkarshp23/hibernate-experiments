package com.learning.hibernate.experiment;

import com.learning.hibernate.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lesson 2 - Persistence Context & Entity Lifecycle
 * Experiment 1: Transient -> Managed -> Flush -> SQL
 *
 * This class deliberately uses EntityManager directly (instead of a
 * Spring Data JPA repository) so that the transitions between Java object
 * state and Persistence Context state are visible step by step.
 */
@Component
public class PersistenceContextExperiment1 {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public void runExperiment() {

        System.out.println("========================================");
        System.out.println("EXPERIMENT 1");
        System.out.println("Transient -> Managed -> Flush -> SQL");
        System.out.println("========================================");

        // --- Java object state: a plain new object, not known to Hibernate ---
        User user = new User();
        user.setName("Experiment User");

        System.out.println("[1] New User created with 'new User()'");
        System.out.println("    Java object state : TRANSIENT (plain Java object, Hibernate has no idea it exists)");
        System.out.println("    id field          : " + user.getId());

        // --- Persistence Context state: Hibernate now tracks this object ---
        entityManager.persist(user);

        System.out.println("[2] entityManager.persist(user) called");
        System.out.println("    Persistence Context state : MANAGED (Hibernate is now tracking this object)");
        System.out.println("    id field                  : " + user.getId() + "  <-- IDENTITY generator assigns this only at flush/insert time");
        System.out.println("    NOTE: no INSERT has been sent to the database yet.");

        // --- Modifying a managed entity: Hibernate will pick this up automatically ---
        user.setName("Changed Before Flush");

        System.out.println("[3] Managed entity modified directly (no save()/update() call)");
        System.out.println("    Current in-memory name : " + user.getName());
        System.out.println("    Because 'user' is MANAGED, Hibernate already knows about this change.");

        // --- Database synchronization: Hibernate writes the current managed state to the DB ---
        System.out.println("[4] Calling entityManager.flush() ...");
        System.out.println("    Watch the console below for the Hibernate INSERT statement.");

        entityManager.flush();

        System.out.println("[5] flush() completed");
        System.out.println("    Persistence Context has been synchronized with the database.");
        System.out.println("    The row now in the database reflects the CURRENT managed state,");
        System.out.println("    i.e. name = 'Changed Before Flush', not the original 'Experiment User'.");

        // --- Transaction boundary: commit happens when this @Transactional method returns ---
        System.out.println("[6] Method returning -> Spring will now COMMIT the transaction");
        System.out.println("========================================");
    }
}
