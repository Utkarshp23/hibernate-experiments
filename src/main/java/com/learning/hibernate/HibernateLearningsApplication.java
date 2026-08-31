package com.learning.hibernate;

import com.learning.hibernate.entity.User;
import com.learning.hibernate.experiment.DirtyCheckingExperiment3;
import com.learning.hibernate.experiment.PersistenceContextExperiment1;
import com.learning.hibernate.experiment.PersistenceContextExperiment2;
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

    @Bean
    CommandLineRunner runExperiments(PersistenceContextExperiment1 experiment1,
                                      PersistenceContextExperiment2 experiment2,
                                      DirtyCheckingExperiment3 experiment3) {
        return args -> {
            //experiment1.runExperiment();

            //Long knownUserId = experiment2.ensureKnownUser();
            //User user1 = experiment2.runExperiment(knownUserId);
            //experiment2.runSecondPersistenceContextScenario(knownUserId, user1);

            Long dirtyCheckingUserId = experiment3.ensureKnownUser();
            experiment3.scenarioA(dirtyCheckingUserId);
            experiment3.scenarioB(dirtyCheckingUserId);
            experiment3.scenarioC(dirtyCheckingUserId);
        };
    }
}
