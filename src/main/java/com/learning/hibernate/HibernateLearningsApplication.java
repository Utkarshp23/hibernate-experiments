package com.learning.hibernate;

import com.learning.hibernate.entity.User;
import com.learning.hibernate.experiment.DetachedEntityExperiment4;
import com.learning.hibernate.experiment.DirtyCheckingExperiment3;
import com.learning.hibernate.experiment.MergeExperiment5;
import com.learning.hibernate.experiment.NPlusOneExperiment6;
import com.learning.hibernate.experiment.PersistenceContextExperiment1;
import com.learning.hibernate.experiment.PersistenceContextExperiment2;
import java.util.List;
import java.util.TimeZone;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class HibernateLearningsApplication {

    public static void main(String[] args) {
        // On some Windows JVMs the default timezone resolves to the legacy
        // alias "Asia/Calcutta", which PostgreSQL 16 rejects outright.
        // Force a real IANA zone before any DB connection is opened.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(HibernateLearningsApplication.class, args);
    }

    // @Bean
    // CommandLineRunner runExperiments(PersistenceContextExperiment1 experiment1,
    //                                   PersistenceContextExperiment2 experiment2,
    //                                   DirtyCheckingExperiment3 experiment3,
    //                                   DetachedEntityExperiment4 experiment4,
    //                                   MergeExperiment5 experiment5,
    //                                   NPlusOneExperiment6 experiment6) {
    //     return args -> {
    //         //experiment1.runExperiment();

    //         //Long knownUserId = experiment2.ensureKnownUser();
    //         //User user1 = experiment2.runExperiment(knownUserId);
    //         //experiment2.runSecondPersistenceContextScenario(knownUserId, user1);

    //         //Long dirtyCheckingUserId = experiment3.ensureKnownUser();
    //         //experiment3.scenarioA(dirtyCheckingUserId);
    //         //experiment3.scenarioB(dirtyCheckingUserId);
    //         //experiment3.scenarioC(dirtyCheckingUserId);

    //         //Long detachedUserId = experiment4.ensureKnownUser();
    //         //experiment4.scenarioA(detachedUserId);
    //         //experiment4.scenarioB(detachedUserId);
    //         //experiment4.verifyDatabaseState(detachedUserId, "Changed While Detached");

    //         //Long mergeUserId = experiment5.ensureKnownUser();
    //         //experiment5.runExperiment(mergeUserId);
    //         //experiment5.verifyDatabaseState(mergeUserId);

    //         List<Long> nPlusOneUserIds = experiment6.ensureKnownUsers();
    //         // experiment6.demonstrateNPlusOneProblem(nPlusOneUserIds);
    //         experiment6.demonstrateJoinFetch();
    //     };
    // }
}
