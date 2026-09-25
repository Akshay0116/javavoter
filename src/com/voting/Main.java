package com.voting;

import com.voting.exception.*;
import com.voting.web.VotingWebServer;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

/**
 * Main application entry point providing:
 * 1. An embedded Real-Time Glassmorphic Web Application (Default on port 8080)
 * 2. An automated end-to-end simulation walkthrough (--walkthrough)
 * 3. An interactive terminal CLI (--interactive)
 */
public class Main {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("--walkthrough")) {
            runAutomatedWalkthrough();
        } else if (args.length > 0 && args[0].equalsIgnoreCase("--interactive")) {
            runInteractiveCLI();
        } else {
            // Read PORT from environment (injected by Render, Railway, Heroku) or default to 8080
            int port = 8080;
            String envPort = System.getenv("PORT");
            if (envPort != null && !envPort.trim().isEmpty()) {
                try {
                    port = Integer.parseInt(envPort.trim());
                } catch (NumberFormatException ignored) {}
            }
            startRealtimeWebServer(port);
        }
    }

    /**
     * Starts the embedded HTTP Web Server with pre-populated election candidates and voters.
     */
    public static void startRealtimeWebServer(int port) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime electionStart = now.minusHours(1); // Active now
        LocalDateTime electionEnd = now.plusHours(8);    // Closes in 8 hours

        VotingManager manager = new VotingManager(
                "National General Election 2026",
                electionStart,
                electionEnd
        );

        // Pre-populate Candidates
        manager.registerCandidate(new Candidate("C1", "Asha Rao", "Progress Alliance"));
        manager.registerCandidate(new Candidate("C2", "Vikram Patel", "Civic Unity Party"));
        manager.registerCandidate(new Candidate("C3", "Meera Joshi", "Independent"));
        manager.registerCandidate(new Candidate("C4", "Dr. Elena Rostova", "Green Future"));

        // Pre-populate Voters
        manager.registerVoter(new Voter("V101", "Ananya Sharma", "1234"));
        manager.registerVoter(new Voter("V102", "Rohan Verma", "5678"));
        manager.registerVoter(new Voter("V103", "Priya Nair", "4321"));
        manager.registerVoter(new Voter("V104", "Arjun Singh", "9876"));
        manager.registerVoter(new Voter("V105", "Kavita Reddy", "1122"));

        try {
            VotingWebServer webServer = new VotingWebServer(manager, port);
            webServer.start();

            System.out.println("========================================================================");
            System.out.println("   ONLINE VOTING SYSTEM — REAL-TIME EMBEDDED WEB SERVER ACTIVE         ");
            System.out.println("========================================================================");
            System.out.printf("  🔗 Web App URL : http://localhost:%d/%n", port);
            System.out.println("  🛡️ Security    : Military-Grade AES-256-GCM + SHA-256 Hashing Active");
            System.out.println("  ⚡ Real-Time   : Instant Voter Verification, Live Tally & Auditing");
            System.out.println("========================================================================");
            System.out.println("  💡 Pre-loaded Test Voters:");
            System.out.println("     • Voter ID: V101  | Name: Ananya Sharma");
            System.out.println("     • Voter ID: V102  | Name: Rohan Verma");
            System.out.println("     • Voter ID: V103  | Name: Priya Nair");
            System.out.println("     • Voter ID: V104  | Name: Arjun Singh");
            System.out.println("     • Voter ID: V105  | Name: Kavita Reddy");
            System.out.println("========================================================================");
            System.out.println("  Press Ctrl+C in this terminal to stop the server anytime.");
            System.out.println("========================================================================");

        } catch (IOException e) {
            System.err.println("Failed to start web server on port " + port + ": " + e.getMessage());
            System.out.println("Falling back to terminal interactive CLI...");
            runInteractiveCLI();
        }
    }

    /**
     * Executes a full, automated, realistic simulation of the election lifecycle.
     */
    public static void runAutomatedWalkthrough() {
        System.out.println("========================================================================");
        System.out.println("       ONLINE VOTING SYSTEM — END-TO-END AUTOMATED SIMULATION           ");
        System.out.println("========================================================================");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime electionStart = now.minusHours(1);
        LocalDateTime electionEnd = now.plusHours(7);

        VotingManager manager = new VotingManager(
                "National Youth Council Election 2026",
                electionStart,
                electionEnd
        );
        System.out.println("✓ Election created: \"" + manager.getElectionName() + "\"");

        Candidate c1 = new Candidate("C1", "Asha Rao", "Progress Alliance");
        Candidate c2 = new Candidate("C2", "Vikram Patel", "Civic Unity Party");
        Candidate c3 = new Candidate("C3", "Meera Joshi", "");

        manager.registerCandidate(c1);
        manager.registerCandidate(c2);
        manager.registerCandidate(c3);

        Voter v1 = new Voter("V101", "Ananya Sharma", "1234");
        Voter v2 = new Voter("V102", "Rohan Verma", "5678");
        Voter v3 = new Voter("V103", "Priya Nair", "4321");
        Voter v4 = new Voter("V104", "Arjun Singh", "9876");
        Voter v5 = new Voter("V105", "Kavita Reddy", "1122");

        manager.registerVoter(v1);
        manager.registerVoter(v2);
        manager.registerVoter(v3);
        manager.registerVoter(v4);
        manager.registerVoter(v5);

        // Verification & Voting by ID & Name (New Workflow)
        System.out.println("\n>>> Constituent Verification by ID & Name:");
        VoterVerificationResult r1 = manager.verifyVoter("V101", "Ananya Sharma");
        System.out.println("  Status for V101: " + r1.getStatus() + " (" + r1.getMessage() + ")");

        try {
            Vote vote1 = manager.castVoteByVoter("V101", "Ananya Sharma", "C1");
            System.out.println("  ✓ V101 voted for C1 (Encrypted Vote Token: " + vote1.getEncryptedVoteId().substring(0, 16) + "...)");
        } catch (Exception e) {
            System.out.println("  ✗ Error: " + e.getMessage());
        }

        // Test Already Voted Detection
        System.out.println("\n>>> Testing Already-Voted Detection on V101:");
        VoterVerificationResult r1Recheck = manager.verifyVoter("V101", "Ananya Sharma");
        System.out.println("  Status for V101: " + r1Recheck.getStatus());
        System.out.println("  Voted Timestamp : " + r1Recheck.getVotedTimestamp().format(FORMATTER));
        System.out.println("  Encrypted Token : " + r1Recheck.getEncryptedVoteId().substring(0, 20) + "...");
        System.out.println("  Receipt Hash    : " + r1Recheck.getReceiptHash());

        // Cast remaining votes
        try {
            manager.castVoteByVoter("V102", "Rohan Verma", "C1");
            manager.castVoteByVoter("V103", "Priya Nair", "C2");
            manager.castVoteByVoter("V104", "Arjun Singh", "C1");
        } catch (Exception e) {
            System.out.println("  ✗ Voting error: " + e.getMessage());
        }

        manager.displayResults();
    }

    /**
     * Interactive console CLI.
     */
    public static void runInteractiveCLI() {
        Scanner scanner = new Scanner(System.in);
        LocalDateTime now = LocalDateTime.now();
        VotingManager manager = new VotingManager(
                "Interactive Election 2026",
                now.minusMinutes(10),
                now.plusHours(2)
        );

        manager.registerCandidate(new Candidate("C1", "Asha Rao", "Progress Alliance"));
        manager.registerCandidate(new Candidate("C2", "Vikram Patel", "Civic Unity Party"));
        manager.registerCandidate(new Candidate("C3", "Meera Joshi", "Independent"));

        manager.registerVoter(new Voter("V1", "Alice", "1111"));
        manager.registerVoter(new Voter("V2", "Bob", "2222"));
        manager.registerVoter(new Voter("V3", "Charlie", "3333"));

        while (true) {
            System.out.println("\n=======================================================");
            System.out.println("       ONLINE VOTING SYSTEM - INTERACTIVE CONSOLE     ");
            System.out.println("=======================================================");
            System.out.println("1. View Election Info & Window");
            System.out.println("2. Verify Voter by ID & Name");
            System.out.println("3. Cast Encrypted Vote");
            System.out.println("4. Display Live Results");
            System.out.println("5. Start Embedded Web App (http://localhost:8080)");
            System.out.println("0. Exit");
            System.out.print("Select an option (0-5): ");

            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1":
                    System.out.println("Name   : " + manager.getElectionName());
                    System.out.println("Window : " + manager.getVotingStart().format(FORMATTER) + " to " + manager.getVotingEnd().format(FORMATTER));
                    break;
                case "2":
                    System.out.print("Enter Voter ID: ");
                    String vid = scanner.nextLine().trim();
                    System.out.print("Enter Voter Name: ");
                    String vname = scanner.nextLine().trim();
                    VoterVerificationResult res = manager.verifyVoter(vid, vname);
                    System.out.println("Status: " + res.getStatus() + " -> " + res.getMessage());
                    if (res.getStatus() == VoterVerificationResult.Status.ALREADY_VOTED) {
                        System.out.println("Voted At: " + res.getVotedTimestamp().format(FORMATTER));
                        System.out.println("Encrypted Vote ID: " + res.getEncryptedVoteId());
                    }
                    break;
                case "3":
                    System.out.print("Voter ID: ");
                    String id = scanner.nextLine().trim();
                    System.out.print("Voter Name: ");
                    String nm = scanner.nextLine().trim();
                    System.out.print("Candidate ID: ");
                    String cid = scanner.nextLine().trim();
                    try {
                        Vote v = manager.castVoteByVoter(id, nm, cid);
                        System.out.println("✓ Vote recorded! Receipt Hash: " + v.getReceiptHash());
                    } catch (Exception e) {
                        System.out.println("✗ Rejected: " + e.getMessage());
                    }
                    break;
                case "4":
                    manager.displayResults();
                    break;
                case "5":
                    startRealtimeWebServer(8080);
                    return;
                case "0":
                    return;
                default:
                    System.out.println("Invalid option.");
            }
        }
    }
}
