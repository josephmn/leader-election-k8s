package com.example.leaderelectionk8s;

import com.example.leaderelectionk8s.config.LeaderElectionConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@RequiredArgsConstructor
public class LeaderElectionK8sApplication implements CommandLineRunner {

	private final LeaderElectionConfig leaderElectionConfig;

	public static void main(String[] args) {
		SpringApplication.run(LeaderElectionK8sApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		leaderElectionConfig.startLeaderElection();
	}
}
