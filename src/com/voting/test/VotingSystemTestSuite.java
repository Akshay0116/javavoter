package com.voting.test;

import com.voting.*;
import com.voting.exception.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Automated test suite verifying all 8 phases and edge cases of the Online Voting System.
 */
public class VotingSystemTestSuite {
    private static int testsPassed = 0;
    private static int testsFailed = 0;

    public static void main(String[] args) {
        System.out.println("===============================================================");
        System.out.println("    ONLINE VOTING SYSTEM - AUTOMATED TEST SUITE EXECUTION    ");
        System.out.println("===============================================================");

        testPhase0_SystemInitialization();
        testPhase1_CandidateRegistration();
        testPhase2_VoterRegistration();
        testPhase3a_TimeWindowGate();
        testPhase3b_VoterIdentityGate();
        testPhase3c_AuthenticationGate();
        testPhase3d_DuplicateVoteGate();
        testPhase3e_CandidateValidityGate();
        testPhase4_VoteRecordAndReceiptHash();
        testPhase6_ResultTabulationAndWinner();
        testPhase7_FileExportAndAuditLog();
        testRealtimeEncryptionAndVoterVerification();

        System.out.println("\n---------------------------------------------------------------");
        System.out.printf("TEST SUMMARY: Total Tests Run: %d | Passed: %d | Failed: %d%n",
                (testsPassed + testsFailed), testsPassed, testsFailed);
        System.out.println("---------------------------------------------------------------");

        if (testsFailed > 0) {
            System.err.println("❌ SOME TESTS FAILED!");
            System.exit(1);
        } else {
            System.out.println("✅ ALL TESTS PASSED SUCCESSFULLY!");
        }
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if ((expected == null && actual == null) || (expected != null && expected.equals(actual))) {
            pass(message);
        } else {
            fail(message + " [Expected: " + expected + ", Got: " + actual + "]");
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (condition) {
            pass(message);
        } else {
            fail(message + " [Condition evaluated to false]");
        }
    }

    private static void pass(String message) {
        testsPassed++;
        System.out.println("  [PASS] " + message);
    }

    private static void fail(String message) {
        testsFailed++;
        System.err.println("  [FAIL] " + message);
    }

    // --- PHASE 0 TESTS ---
    private static void testPhase0_SystemInitialization() {
        System.out.println("\n--- Testing Phase 0: System Initialization ---");
        LocalDateTime start = LocalDateTime.of(2026, 10, 1, 9, 0);
        LocalDateTime end = LocalDateTime.of(2026, 10, 1, 17, 0);

        // Valid initialization
        try {
            VotingManager vm = new VotingManager("Campus Election 2026", start, end);
            assertEquals("Campus Election 2026", vm.getElectionName(), "Election name matches");
            assertEquals(start, vm.getVotingStart(), "Voting start matches");
            assertEquals(end, vm.getVotingEnd(), "Voting end matches");
            assertEquals(0, vm.getTotalRegisteredVoters(), "Initial voter count is 0");
            assertEquals(0, vm.getTotalRegisteredCandidates(), "Initial candidate count is 0");
            assertEquals(0, vm.getTotalVotesCast(), "Initial votes count is 0");
        } catch (Exception e) {
            fail("Valid initialization threw unexpected exception: " + e.getMessage());
        }

        // Invalid window: end is before start
        try {
            new VotingManager("Invalid", end, start);
            fail("Phase 0 should reject end before start");
        } catch (IllegalArgumentException e) {
            pass("Phase 0 rejects end before start with IllegalArgumentException");
        }

        // Invalid window: end equals start (zero length)
        try {
            new VotingManager("ZeroLength", start, start);
            fail("Phase 0 should reject end equal to start");
        } catch (IllegalArgumentException e) {
            pass("Phase 0 rejects end equal to start with IllegalArgumentException");
        }

        // Blank election name
        try {
            new VotingManager("   ", start, end);
            fail("Phase 0 should reject blank election name");
        } catch (IllegalArgumentException e) {
            pass("Phase 0 rejects blank election name");
        }
    }

    // --- PHASE 1 TESTS ---
    private static void testPhase1_CandidateRegistration() {
        System.out.println("\n--- Testing Phase 1: Candidate Registration ---");
        VotingManager vm = new VotingManager("Test Election",
                LocalDateTime.of(2026, 10, 1, 9, 0),
                LocalDateTime.of(2026, 10, 1, 17, 0));

        Candidate c1 = new Candidate("C1", "Asha Rao", "Progress Party");
        vm.registerCandidate(c1);
        assertEquals(1, vm.getTotalRegisteredCandidates(), "Candidate C1 registered successfully");

        // Default party to 'Independent' if blank/null
        Candidate c2 = new Candidate("C2", "Vikram Patel", "");
        assertEquals("Independent", c2.getParty(), "Blank party defaults to 'Independent'");
        vm.registerCandidate(c2);
        assertEquals(2, vm.getTotalRegisteredCandidates(), "Candidate C2 registered successfully");

        // Duplicate candidate ID rejection
        try {
            vm.registerCandidate(new Candidate("C1", "Duplicate Asha", "Other Party"));
            fail("Duplicate candidate ID should be rejected");
        } catch (IllegalArgumentException e) {
            pass("Duplicate candidate ID rejected with IllegalArgumentException");
        }

        // Invalid candidate instantiation
        try {
            new Candidate("", "Valid Name", "Party");
            fail("Blank candidate ID should be rejected");
        } catch (IllegalArgumentException e) {
            pass("Blank candidate ID rejected");
        }
    }

    // --- PHASE 2 TESTS ---
    private static void testPhase2_VoterRegistration() {
        System.out.println("\n--- Testing Phase 2: Voter Registration ---");
        VotingManager vm = new VotingManager("Test Election",
                LocalDateTime.of(2026, 10, 1, 9, 0),
                LocalDateTime.of(2026, 10, 1, 17, 0));

        Voter v1 = new Voter("V1", "Ananya Sharma", "1234");
        assertEquals(false, v1.hasVoted(), "Voter starts with hasVoted = false");
        vm.registerVoter(v1);
        assertEquals(1, vm.getTotalRegisteredVoters(), "Voter V1 registered successfully");

        // Duplicate voter ID rejection
        try {
            vm.registerVoter(new Voter("V1", "Another Person", "9999"));
            fail("Duplicate voter ID should be rejected");
        } catch (IllegalArgumentException e) {
            pass("Duplicate voter ID rejected with IllegalArgumentException");
        }

        // Invalid PINs: non-digits, length != 4
        try {
            new Voter("V2", "User", "12a4");
            fail("Non-digit PIN should be rejected");
        } catch (IllegalArgumentException e) {
            pass("Non-digit PIN rejected");
        }

        try {
            new Voter("V3", "User", "123");
            fail("3-digit PIN should be rejected");
        } catch (IllegalArgumentException e) {
            pass("3-digit PIN rejected");
        }

        try {
            new Voter("V4", "User", "12345");
            fail("5-digit PIN should be rejected");
        } catch (IllegalArgumentException e) {
            pass("5-digit PIN rejected");
        }
    }

    // --- PHASE 3a TESTS ---
    private static void testPhase3a_TimeWindowGate() {
        System.out.println("\n--- Testing Phase 3a: Time Window Gate ---");
        LocalDateTime start = LocalDateTime.of(2026, 10, 1, 9, 0);
        LocalDateTime end = LocalDateTime.of(2026, 10, 1, 17, 0);
        VotingManager vm = new VotingManager("Window Test", start, end);

        vm.registerCandidate(new Candidate("C1", "Asha Rao", "Progress"));
        vm.registerVoter(new Voter("V1", "Ananya", "1234"));

        // Attempt vote BEFORE start window
        LocalDateTime beforeTime = LocalDateTime.of(2026, 10, 1, 8, 59, 59);
        try {
            vm.castVote("V1", "1234", "C1", beforeTime);
            fail("Vote before start window must be rejected");
        } catch (VotingWindowException e) {
            pass("Vote before start window correctly throws VotingWindowException: " + e.getMessage());
        } catch (Exception e) {
            fail("Expected VotingWindowException but got: " + e.getClass().getSimpleName());
        }

        // Attempt vote AFTER end window
        LocalDateTime afterTime = LocalDateTime.of(2026, 10, 1, 17, 0, 1);
        try {
            vm.castVote("V1", "1234", "C1", afterTime);
            fail("Vote after end window must be rejected");
        } catch (VotingWindowException e) {
            pass("Vote after end window correctly throws VotingWindowException: " + e.getMessage());
        } catch (Exception e) {
            fail("Expected VotingWindowException but got: " + e.getClass().getSimpleName());
        }
    }

    // --- PHASE 3b TESTS ---
    private static void testPhase3b_VoterIdentityGate() {
        System.out.println("\n--- Testing Phase 3b: Voter Identity Gate ---");
        LocalDateTime start = LocalDateTime.of(2026, 10, 1, 9, 0);
        LocalDateTime end = LocalDateTime.of(2026, 10, 1, 17, 0);
        VotingManager vm = new VotingManager("Identity Test", start, end);
        vm.registerCandidate(new Candidate("C1", "Asha Rao", "Progress"));

        LocalDateTime validTime = LocalDateTime.of(2026, 10, 1, 10, 0);

        try {
            vm.castVote("UNKNOWN_VOTER", "1234", "C1", validTime);
            fail("Unregistered voter ID should throw VoterNotFoundException");
        } catch (VoterNotFoundException e) {
            assertEquals("UNKNOWN_VOTER", e.getVoterId(), "VoterNotFoundException captures voterId");
            pass("Gate 3b correctly throws VoterNotFoundException");
        } catch (Exception e) {
            fail("Expected VoterNotFoundException but got: " + e.getClass().getSimpleName());
        }
    }

    // --- PHASE 3c TESTS ---
    private static void testPhase3c_AuthenticationGate() {
        System.out.println("\n--- Testing Phase 3c: Authentication Gate ---");
        LocalDateTime start = LocalDateTime.of(2026, 10, 1, 9, 0);
        LocalDateTime end = LocalDateTime.of(2026, 10, 1, 17, 0);
        VotingManager vm = new VotingManager("Auth Test", start, end);
        vm.registerCandidate(new Candidate("C1", "Asha Rao", "Progress"));
        vm.registerVoter(new Voter("V1", "Ananya", "1234"));

        LocalDateTime validTime = LocalDateTime.of(2026, 10, 1, 10, 0);

        try {
            vm.castVote("V1", "9999", "C1", validTime);
            fail("Wrong PIN should throw InvalidCredentialException");
        } catch (InvalidCredentialException e) {
            assertEquals("V1", e.getVoterId(), "InvalidCredentialException captures voterId");
            pass("Gate 3c correctly throws InvalidCredentialException on incorrect PIN");
        } catch (Exception e) {
            fail("Expected InvalidCredentialException but got: " + e.getClass().getSimpleName());
        }
    }

    // --- PHASE 3d TESTS ---
    private static void testPhase3d_DuplicateVoteGate() {
        System.out.println("\n--- Testing Phase 3d: Duplicate-Vote Gate ---");
        LocalDateTime start = LocalDateTime.of(2026, 10, 1, 9, 0);
        LocalDateTime end = LocalDateTime.of(2026, 10, 1, 17, 0);
        VotingManager vm = new VotingManager("Duplicate Test", start, end);
        vm.registerCandidate(new Candidate("C1", "Asha Rao", "Progress"));
        vm.registerVoter(new Voter("V1", "Ananya", "1234"));

        LocalDateTime validTime = LocalDateTime.of(2026, 10, 1, 10, 0);

        try {
            Vote v = vm.castVote("V1", "1234", "C1", validTime);
            assertTrue(v != null, "First vote succeeds");
        } catch (Exception e) {
            fail("First vote should succeed, but got: " + e.getMessage());
        }

        // Second vote with same voter
        try {
            vm.castVote("V1", "1234", "C1", validTime.plusMinutes(5));
            fail("Second vote by same voter must throw DuplicateVoteException");
        } catch (DuplicateVoteException e) {
            assertEquals("V1", e.getVoterId(), "DuplicateVoteException captures voterId");
            pass("Gate 3d correctly throws DuplicateVoteException on second attempt");
        } catch (Exception e) {
            fail("Expected DuplicateVoteException but got: " + e.getClass().getSimpleName());
        }
    }

    // --- PHASE 3e TESTS ---
    private static void testPhase3e_CandidateValidityGate() {
        System.out.println("\n--- Testing Phase 3e: Candidate Validity Gate ---");
        LocalDateTime start = LocalDateTime.of(2026, 10, 1, 9, 0);
        LocalDateTime end = LocalDateTime.of(2026, 10, 1, 17, 0);
        VotingManager vm = new VotingManager("Candidate Validity Test", start, end);
        vm.registerCandidate(new Candidate("C1", "Asha Rao", "Progress"));
        Voter v1 = new Voter("V1", "Ananya", "1234");
        vm.registerVoter(v1);

        LocalDateTime validTime = LocalDateTime.of(2026, 10, 1, 10, 0);

        try {
            vm.castVote("V1", "1234", "NON_EXISTENT_CANDIDATE", validTime);
            fail("Vote for non-existent candidate should throw CandidateNotFoundException");
        } catch (CandidateNotFoundException e) {
            assertEquals("NON_EXISTENT_CANDIDATE", e.getCandidateId(), "Exception captures candidateId");
            pass("Gate 3e correctly throws CandidateNotFoundException");
            // Check that voter was NOT marked as voted and can retry!
            assertEquals(false, v1.hasVoted(), "Voter hasVoted remains false after candidate rejection");
        } catch (Exception e) {
            fail("Expected CandidateNotFoundException but got: " + e.getClass().getSimpleName());
        }

        // Retry with valid candidate succeeds
        try {
            Vote v = vm.castVote("V1", "1234", "C1", validTime.plusMinutes(1));
            assertTrue(v != null, "Retry with valid candidate succeeds");
            assertEquals(true, v1.hasVoted(), "Voter is marked as voted only after success");
            pass("Voter successfully cast ballot on retry after candidate ID correction");
        } catch (Exception e) {
            fail("Retry failed with: " + e.getMessage());
        }
    }

    // --- PHASE 4 TESTS ---
    private static void testPhase4_VoteRecordAndReceiptHash() {
        System.out.println("\n--- Testing Phase 4: Vote Record and Cryptographic Receipt ---");
        LocalDateTime start = LocalDateTime.of(2026, 10, 1, 9, 0);
        LocalDateTime end = LocalDateTime.of(2026, 10, 1, 17, 0);
        VotingManager vm = new VotingManager("Receipt Test", start, end);
        vm.registerCandidate(new Candidate("C1", "Asha Rao", "Progress"));
        vm.registerVoter(new Voter("V1", "Ananya", "1234"));

        LocalDateTime voteTime = LocalDateTime.of(2026, 10, 1, 11, 30);
        try {
            Vote receipt = vm.castVote("V1", "1234", "C1", voteTime);
            assertTrue(receipt.getVoteId() != null && !receipt.getVoteId().isEmpty(), "Vote ID is generated UUID");
            assertEquals("V1", receipt.getVoterId(), "Vote voter ID matches");
            assertEquals("C1", receipt.getCandidateId(), "Vote candidate ID matches");
            assertEquals(voteTime, receipt.getTimestamp(), "Vote timestamp matches");

            String hash = receipt.getReceiptHash();
            assertTrue(hash != null && hash.length() == 64, "Receipt hash is valid 64-char SHA-256 hex string");
            pass("Vote receipt hash successfully generated: " + hash.substring(0, 16) + "...");
        } catch (Exception e) {
            fail("Phase 4 test threw: " + e.getMessage());
        }
    }

    // --- PHASE 6 TESTS ---
    private static void testPhase6_ResultTabulationAndWinner() {
        System.out.println("\n--- Testing Phase 6: Result Tabulation and Zero-Vote Handling ---");
        LocalDateTime start = LocalDateTime.of(2026, 10, 1, 9, 0);
        LocalDateTime end = LocalDateTime.of(2026, 10, 1, 17, 0);
        VotingManager vm = new VotingManager("Tally Test", start, end);

        Candidate c1 = new Candidate("C1", "Asha Rao", "Progress");
        Candidate c2 = new Candidate("C2", "Vikram Patel", "Unity");
        Candidate c3 = new Candidate("C3", "Priya Nair", "Independent");

        vm.registerCandidate(c1);
        vm.registerCandidate(c2);
        vm.registerCandidate(c3);

        vm.registerVoter(new Voter("V1", "Voter 1", "1111"));
        vm.registerVoter(new Voter("V2", "Voter 2", "2222"));
        vm.registerVoter(new Voter("V3", "Voter 3", "3333"));
        vm.registerVoter(new Voter("V4", "Voter 4", "4444"));

        // Prior to any votes, winner should be null
        assertEquals(null, vm.getWinner(), "Winner is null when 0 votes cast");

        LocalDateTime voteTime = LocalDateTime.of(2026, 10, 1, 10, 0);
        try {
            // V1 and V2 vote for C1 (2 votes)
            vm.castVote("V1", "1111", "C1", voteTime);
            vm.castVote("V2", "2222", "C1", voteTime.plusMinutes(1));

            // V3 votes for C2 (1 vote)
            vm.castVote("V3", "3333", "C2", voteTime.plusMinutes(2));

            // C3 receives 0 votes

            Map<Candidate, Integer> results = vm.getResults();
            assertEquals(3, results.size(), "Results map includes all 3 candidates");
            assertEquals(2, results.get(c1), "C1 has 2 votes");
            assertEquals(1, results.get(c2), "C2 has 1 vote");
            assertEquals(0, results.get(c3), "Zero-vote candidate C3 is explicitly present with 0 votes");

            assertEquals(3, vm.getTotalVotesCast(), "Total votes cast is 3");
            assertEquals(4, vm.getTotalRegisteredVoters(), "Total registered voters is 4");
            assertEquals(75.0, vm.getTurnoutPercentage(), "Turnout is 75.00%");

            Candidate winner = vm.getWinner();
            assertEquals(c1, winner, "Winner is correctly identified as C1 (Asha Rao)");
            pass("Phase 6 tabulation and winner derivation verified");
        } catch (Exception e) {
            fail("Phase 6 tabulation failed: " + e.getMessage());
        }
    }

    // --- PHASE 7 TESTS ---
    private static void testPhase7_FileExportAndAuditLog() {
        System.out.println("\n--- Testing Phase 7: Export Summary and Audit Trail ---");
        LocalDateTime start = LocalDateTime.of(2026, 10, 1, 9, 0);
        LocalDateTime end = LocalDateTime.of(2026, 10, 1, 17, 0);
        VotingManager vm = new VotingManager("Audit Export Test", start, end);

        Candidate c1 = new Candidate("C1", "Asha Rao", "Progress");
        Candidate c2 = new Candidate("C2", "Vikram Patel", "Unity");
        vm.registerCandidate(c1);
        vm.registerCandidate(c2);

        vm.registerVoter(new Voter("V1", "Ananya", "1234"));
        vm.registerVoter(new Voter("V2", "Rohan", "5678"));

        LocalDateTime voteTime = LocalDateTime.of(2026, 10, 1, 10, 0);
        String exportedFile = "test_results_summary.txt";

        try {
            Vote vote1 = vm.castVote("V1", "1234", "C1", voteTime);
            Vote vote2 = vm.castVote("V2", "5678", "C2", voteTime.plusMinutes(10));

            vm.exportSummary(exportedFile);

            File f = new File(exportedFile);
            assertTrue(f.exists(), "Export file was created on disk");

            String content = Files.readString(Paths.get(exportedFile));
            assertTrue(content.contains("OFFICIAL ELECTION RESULTS REPORT"), "File contains official header");
            assertTrue(content.contains("Asha Rao"), "File contains Candidate Asha Rao");
            assertTrue(content.contains("Vikram Patel"), "File contains Candidate Vikram Patel");
            assertTrue(content.contains(vote1.getReceiptHash()), "Audit trail contains Vote 1 receipt hash");
            assertTrue(content.contains(vote2.getReceiptHash()), "Audit trail contains Vote 2 receipt hash");
            assertTrue(content.contains(vote1.getVoteId()), "Audit trail contains Vote 1 UUID");
            assertTrue(content.contains(vote2.getVoteId()), "Audit trail contains Vote 2 UUID");

            // Verify secrecy: Candidate choices are NOT printed in the audit trail table section
            int auditLogSectionIdx = content.indexOf("OFFICIAL VOTE AUDIT LOG");
            String auditSection = content.substring(auditLogSectionIdx);
            assertTrue(!auditSection.contains("Progress") && !auditSection.contains("Unity"),
                    "Audit trail preserves ballot secrecy (does not reveal political party or choice in audit section)");

            pass("Summary export and ballot secrecy audit log successfully verified");

            // Clean up temporary test file
            f.delete();
        } catch (Exception e) {
            fail("Phase 7 export test failed: " + e.getMessage());
        }
    }

    // --- REAL-TIME ENCRYPTION & ID/NAME VERIFICATION TESTS ---
    private static void testRealtimeEncryptionAndVoterVerification() {
        System.out.println("\n--- Testing Real-Time AES-256 Encryption & ID/Name Verification ---");

        // 1. Test EncryptionUtil round-trip
        String testMessage = "SECRET_BALLOT_VOTER_101";
        String cipherToken = EncryptionUtil.encrypt(testMessage);
        assertTrue(cipherToken != null && !cipherToken.equals(testMessage), "AES-256 token is encrypted");
        String decrypted = EncryptionUtil.decrypt(cipherToken);
        assertEquals(testMessage, decrypted, "AES-256 decryption matches original plaintext");

        // 2. Setup VotingManager
        VotingManager vm = new VotingManager("Constituent Test Election",
                LocalDateTime.of(2026, 10, 1, 9, 0),
                LocalDateTime.of(2026, 10, 1, 17, 0));
        vm.registerCandidate(new Candidate("C1", "Asha Rao", "Progress"));
        vm.registerVoter(new Voter("V101", "Ananya Sharma", "1234"));

        // 3. Test Unknown Voter
        VoterVerificationResult notFoundRes = vm.verifyVoter("V999", "Ananya Sharma");
        assertEquals(VoterVerificationResult.Status.VOTER_NOT_FOUND, notFoundRes.getStatus(), "Unknown voter ID returns VOTER_NOT_FOUND");

        // 4. Test Name Mismatch
        VoterVerificationResult mismatchRes = vm.verifyVoter("V101", "Wrong Name");
        assertEquals(VoterVerificationResult.Status.NAME_MISMATCH, mismatchRes.getStatus(), "Mismatched name returns NAME_MISMATCH");

        // 5. Test Eligible Voter (hasVoted = false)
        VoterVerificationResult eligibleRes = vm.verifyVoter("V101", "Ananya Sharma");
        assertEquals(VoterVerificationResult.Status.ELIGIBLE_TO_VOTE, eligibleRes.getStatus(), "Unvoted voter returns ELIGIBLE_TO_VOTE");

        // 6. Cast Vote with Real-Time Encryption
        LocalDateTime voteTime = LocalDateTime.of(2026, 10, 1, 11, 0);
        try {
            Vote v = vm.castVoteByVoter("V101", "Ananya Sharma", "C1", voteTime);
            assertTrue(v.getEncryptedVoteId() != null, "Vote has encrypted vote ID token");
            assertTrue(v.getEncryptedPayload() != null, "Vote has encrypted payload token");

            // Decrypt payload and check content
            String decryptedPayload = EncryptionUtil.decrypt(v.getEncryptedPayload());
            assertTrue(decryptedPayload.contains("VOTER=V101") && decryptedPayload.contains("CANDIDATE=C1"),
                    "Decrypted payload matches cast vote metadata");
            pass("Vote record encrypted with AES-256-GCM successfully");

            // 7. Verify ALREADY_VOTED status with timestamp and encrypted receipt
            VoterVerificationResult alreadyVotedRes = vm.verifyVoter("V101", "Ananya Sharma");
            assertEquals(VoterVerificationResult.Status.ALREADY_VOTED, alreadyVotedRes.getStatus(), "Voter is marked ALREADY_VOTED");
            assertEquals(voteTime, alreadyVotedRes.getVotedTimestamp(), "Already voted result contains exact timestamp");
            assertEquals(v.getReceiptHash(), alreadyVotedRes.getReceiptHash(), "Already voted result contains receipt hash");
            assertEquals(v.getEncryptedVoteId(), alreadyVotedRes.getEncryptedVoteId(), "Already voted result contains encrypted vote ID");
            pass("Voter state transitions to ALREADY_VOTED with timestamp and encrypted receipt verification");

            // 8. Attempt Duplicate Vote through castVoteByVoter
            try {
                vm.castVoteByVoter("V101", "Ananya Sharma", "C1", voteTime.plusMinutes(5));
                fail("Duplicate vote must be rejected");
            } catch (DuplicateVoteException e) {
                pass("Duplicate vote rejected with DuplicateVoteException");
            }
        } catch (Exception e) {
            fail("Encryption and verification test failed: " + e.getMessage());
        }
    }
}
