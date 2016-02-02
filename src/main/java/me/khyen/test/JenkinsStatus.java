package me.khyen.jenkins;

import java.io.Console;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;

/**
 * @author Kevin Yen
 */
public class JenkinsStatus {

	public static void main(String [] args) throws Exception {
		CommandLineParser parser = new DefaultParser();

		Options options = new Options();

		options.addOption("u", "user", true, "Specify the username used in authentication");

		CommandLine line = parser.parse(options, args);

		JsonGetter jsonGetter = new LocalJsonGetter();

		boolean remote = false;

		if (line.hasOption("u")) {
			Console console = System.console();

			if (console == null) {
				System.out.println("Unable to get Console instance");
				System.exit(0);
			}

			String password = new String(console.readPassword("Enter host password: "));

			String username = line.getOptionValue("u");

			jsonGetter = new RemoteJsonGetter(username, password);

			remote = true;
		}

		List<String> pullRequestJobURLs = JenkinsProperties.getPullRequestJobURLs(remote);

		Set<Future<List<String>>> futures = new HashSet<Future<List<String>>>();

		int threadPoolSize = 120;

		ExecutorService executor = Executors.newFixedThreadPool(threadPoolSize);

		CompletionService<List<String>> completionService = new ExecutorCompletionService<List<String>>(executor);

		System.out.println("Checking " + pullRequestJobURLs.size() + " URLs using with a thead pool size of " + threadPoolSize);

		for (String pullRequestJobURL : pullRequestJobURLs) {
			Callable<List<String>> callable = new ActiveBuildURLsGetter(jsonGetter, pullRequestJobURL);

			futures.add(completionService.submit(callable));
		}

		List<String> activePullRequestURLs = new ArrayList<String>();

		while (futures.size() > 0) {
			Future<List<String>> completedFuture = completionService.take();

			futures.remove(completedFuture);

			List<String> activeBuildURLs = completedFuture.get();

			activePullRequestURLs.addAll(activeBuildURLs);

			System.out.println(futures.size() + " threads still active");
		}

		executor.shutdown();

		System.out.println("Listing currently running pull requests...");

		for (String activePullRequestURL : activePullRequestURLs) {
			System.out.println(activePullRequestURL);
		}

		System.out.println(activePullRequestURLs.size() + " pull requests are currently running");
	}

}