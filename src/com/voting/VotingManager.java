package com.voting;

import com.voting.exception.*;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Coordinates the entire election lifecycle.
 * Responsible for participant registration, enforcing the 5-gate vote verification pipeline,
 * tabulating results, real-time live verification, and generating formatted audit reports.
 */
public class VotingManager {
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String electionName;
    private final LocalDateTime votingStart;
    private final LocalDateTime votingEnd;

    private final Map<String, Voter> voters;
    private final Map<String, Candidate> candidates;
    private final List<Vote> voteLog;

    /**
     * Phase 0 — System Initialization.
     * Enforces fail-fast time window verification and initializes immutable registries.
     */
    public VotingManager(String electionName, LocalDateTime votingStart, LocalDateTime votingEnd) {
        if (electionName == null || electionName.trim().isEmpty()) {
            throw new IllegalArgumentException("Election name cannot be null or blank");
        }
        if (votingStart == null || votingEnd == null) {
            throw new IllegalArgumentException("Voting window timestamps cannot be null");
        }
        if (!votingEnd.isAfter(votingStart)) {
            throw new IllegalArgumentException("Voting end time must be strictly after voting start time");
        }

        this.electionName = electionName.trim();
        this.votingStart = votingStart;
        this.votingEnd = votingEnd;

        this.voters = new LinkedHashMap<>();
        this.candidates = new LinkedHashMap<>();
        this.voteLog = new ArrayList<>();
    }

    public String getElectionName() {
        return electionName;
    }

    public LocalDateTime getVotingStart() {
        return votingStart;
    }

    public LocalDateTime getVotingEnd() {
        return votingEnd;
    }

    public synchronized void registerCandidate(Candidate candidate) {
        if (candidate == null) {
            throw new IllegalArgumentException("Candidate cannot be null");
        }
        String key = candidate.getCandidateId().toUpperCase();
        if (candidates.containsKey(key)) {
            throw new IllegalArgumentException("Candidate ID already registered: " + candidate.getCandidateId());
        }
        candidates.put(key, candidate);
    }

    public synchronized void registerVoter(Voter voter) {
        if (voter == null) {
            throw new IllegalArgumentException("Voter cannot be null");
        }
        String key = voter.getVoterId().toUpperCase();
        if (voters.containsKey(key)) {
            throw new IllegalArgumentException("Voter ID already registered: " + voter.getVoterId());
        }
        voters.put(key, voter);
    }

    /**
     * Verifies voter identity using Voter ID and Name.
     * Evaluates whether the voter exists and checks if they have already voted.
     * Returns timestamp and cryptographic receipt if already voted.
     */
    public synchronized VoterVerificationResult verifyVoter(String voterId, String name) {
        if (voterId == null || voterId.trim().isEmpty()) {
            return VoterVerificationResult.notFound("EMPTY_ID");
        }

        Voter voter = voters.get(voterId.trim().toUpperCase());
        if (voter == null) {
            return VoterVerificationResult.notFound(voterId.trim());
        }

        if (name != null && !name.trim().isEmpty()) {
            if (!voter.getName().equalsIgnoreCase(name.trim())) {
                return VoterVerificationResult.nameMismatch(voterId.trim(), voter.getName());
            }
        }

        if (voter.hasVoted()) {
            return VoterVerificationResult.alreadyVoted(
                    voter.getVoterId(),
                    voter.getName(),
                    voter.getVotedTimestamp(),
                    voter.getVoteReceiptHash(),
                    voter.getEncryptedVoteId()
            );
        }

        return VoterVerificationResult.eligible(voter.getVoterId(), voter.getName());
    }

    /**
     * Casts a vote using Voter ID, Name, and Candidate ID with real-time encryption.
     */
    public Vote castVoteByVoter(String voterId, String name, String candidateId)
            throws VotingWindowException, VoterNotFoundException,
            InvalidCredentialException, DuplicateVoteException, CandidateNotFoundException {
        return castVoteByVoter(voterId, name, candidateId, LocalDateTime.now());
    }

    /**
     * Casts a vote with explicit/simulated timestamp.
     */
    public synchronized Vote castVoteByVoter(String voterId, String name, String candidateId, LocalDateTime currentInstant)
            throws VotingWindowException, VoterNotFoundException,
            InvalidCredentialException, DuplicateVoteException, CandidateNotFoundException {

        // Gate 3a: Time Window Gate
        if (currentInstant.isBefore(votingStart)) {
            throw new VotingWindowException(String.format(
                    "Voting has not opened yet. Window opens at: %s (Current: %s)",
                    votingStart.format(DATE_FORMATTER), currentInstant.format(DATE_FORMATTER)));
        }
        if (currentInstant.isAfter(votingEnd)) {
            throw new VotingWindowException(String.format(
                    "Voting has closed. Window closed at: %s (Current: %s)",
                    votingEnd.format(DATE_FORMATTER), currentInstant.format(DATE_FORMATTER)));
        }

        // Gate 3b: Voter Identity Gate
        if (voterId == null || voterId.trim().isEmpty()) {
            throw new VoterNotFoundException("NULL");
        }
        Voter voter = voters.get(voterId.trim().toUpperCase());
        if (voter == null) {
            throw new VoterNotFoundException(voterId.trim());
        }

        // Gate 3c: Name Verification Gate
        if (name != null && !name.trim().isEmpty()) {
            if (!voter.getName().equalsIgnoreCase(name.trim())) {
                throw new InvalidCredentialException(voterId + " (Name does not match registered voter record)");
            }
        }

        // Gate 3d: Duplicate-Vote Gate
        if (voter.hasVoted()) {
            throw new DuplicateVoteException(voterId);
        }

        // Gate 3e: Candidate Validity Gate
        if (candidateId == null || candidateId.trim().isEmpty()) {
            throw new CandidateNotFoundException("NULL");
        }
        Candidate candidate = candidates.get(candidateId.trim().toUpperCase());
        if (candidate == null) {
            throw new CandidateNotFoundException(candidateId.trim());
        }

        // Phase 4: Atomic Encrypted Vote Commit
        Vote vote = new Vote(voter.getVoterId(), candidate.getCandidateId(), currentInstant);
        voteLog.add(vote);
        voter.markAsVoted(currentInstant, vote.getReceiptHash(), vote.getEncryptedVoteId());

        return vote;
    }

    /**
     * Backward-compatible PIN-based vote entry point (from original workflow).
     */
    public Vote castVote(String voterId, String pin, String candidateId)
            throws VotingWindowException, VoterNotFoundException,
            InvalidCredentialException, DuplicateVoteException, CandidateNotFoundException {
        return castVote(voterId, pin, candidateId, LocalDateTime.now());
    }

    public synchronized Vote castVote(String voterId, String pin, String candidateId, LocalDateTime currentInstant)
            throws VotingWindowException, VoterNotFoundException,
            InvalidCredentialException, DuplicateVoteException, CandidateNotFoundException {

        if (currentInstant.isBefore(votingStart)) {
            throw new VotingWindowException(String.format(
                    "Voting has not opened yet. Window opens at: %s (Current: %s)",
                    votingStart.format(DATE_FORMATTER), currentInstant.format(DATE_FORMATTER)));
        }
        if (currentInstant.isAfter(votingEnd)) {
            throw new VotingWindowException(String.format(
                    "Voting has closed. Window closed at: %s (Current: %s)",
                    votingEnd.format(DATE_FORMATTER), currentInstant.format(DATE_FORMATTER)));
        }

        if (voterId == null) throw new VoterNotFoundException("NULL");
        Voter voter = voters.get(voterId.trim().toUpperCase());
        if (voter == null) {
            throw new VoterNotFoundException(voterId);
        }

        if (!voter.verifyPin(pin)) {
            throw new InvalidCredentialException(voterId);
        }

        if (voter.hasVoted()) {
            throw new DuplicateVoteException(voterId);
        }

        if (candidateId == null) throw new CandidateNotFoundException("NULL");
        Candidate candidate = candidates.get(candidateId.trim().toUpperCase());
        if (candidate == null) {
            throw new CandidateNotFoundException(candidateId);
        }

        Vote vote = new Vote(voter.getVoterId(), candidate.getCandidateId(), currentInstant);
        voteLog.add(vote);
        voter.markAsVoted(currentInstant, vote.getReceiptHash(), vote.getEncryptedVoteId());

        return vote;
    }

    public synchronized Map<Candidate, Integer> getResults() {
        Map<Candidate, Integer> tally = new LinkedHashMap<>();
        for (Candidate c : candidates.values()) {
            tally.put(c, 0);
        }

        for (Vote vote : voteLog) {
            Candidate c = candidates.get(vote.getCandidateId().toUpperCase());
            if (c != null) {
                tally.merge(c, 1, Integer::sum);
            }
        }

        return tally.entrySet().stream()
                .sorted(Map.Entry.<Candidate, Integer>comparingByValue().reversed()
                        .thenComparing(e -> e.getKey().getName()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    public synchronized Candidate getWinner() {
        if (voteLog.isEmpty()) {
            return null;
        }

        Map<Candidate, Integer> results = getResults();
        if (results.isEmpty()) {
            return null;
        }

        Map.Entry<Candidate, Integer> topEntry = results.entrySet().iterator().next();
        if (topEntry.getValue() == 0) {
            return null;
        }

        return topEntry.getKey();
    }

    public synchronized int getTotalVotesCast() {
        return voteLog.size();
    }

    public synchronized int getTotalRegisteredVoters() {
        return voters.size();
    }

    public synchronized int getTotalRegisteredCandidates() {
        return candidates.size();
    }

    public synchronized double getTurnoutPercentage() {
        int registered = voters.size();
        if (registered == 0) return 0.0;
        return ((double) voteLog.size() / registered) * 100.0;
    }

    public synchronized List<Candidate> getCandidateList() {
        return new ArrayList<>(candidates.values());
    }

    public synchronized List<Voter> getVoterList() {
        return new ArrayList<>(voters.values());
    }

    public synchronized List<Vote> getVoteAuditLog() {
        return Collections.unmodifiableList(new ArrayList<>(voteLog));
    }

    public synchronized void displayResults() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("                   ELECTION RESULTS: " + electionName);
        System.out.println("=".repeat(70));
        System.out.printf("%-10s | %-24s | %-16s | %-6s | %-8s%n", "ID", "Candidate Name", "Party", "Votes", "Share");
        System.out.println("-".repeat(70));

        Map<Candidate, Integer> results = getResults();
        int totalVotes = voteLog.size();

        for (Map.Entry<Candidate, Integer> entry : results.entrySet()) {
            Candidate c = entry.getKey();
            int count = entry.getValue();
            double share = totalVotes == 0 ? 0.0 : ((double) count / totalVotes) * 100.0;
            System.out.printf("%-10s | %-24s | %-16s | %-6d | %6.2f%%%n",
                    c.getCandidateId(), c.getName(), c.getParty(), count, share);
        }

        System.out.println("-".repeat(70));
        System.out.printf("Total Votes Cast       : %d%n", totalVotes);
        System.out.printf("Total Registered Voters: %d%n", voters.size());
        System.out.printf("Turnout Percentage     : %.2f%%%n", getTurnoutPercentage());
        System.out.println("-".repeat(70));

        Candidate winner = getWinner();
        if (winner != null) {
            System.out.printf("WINNER: %s (%s) with %d votes!%n",
                    winner.getName(), winner.getParty(), results.get(winner));
        } else {
            System.out.println("WINNER: No votes cast yet.");
        }
        System.out.println("=".repeat(70) + "\n");
    }

    public synchronized void exportSummary(String filePath) throws IOException {
        Map<Candidate, Integer> results = getResults();
        int totalVotes = voteLog.size();
        int registeredVoters = voters.size();
        double turnout = getTurnoutPercentage();
        Candidate winner = getWinner();

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, StandardCharsets.UTF_8))) {
            writer.write("========================================================================\n");
            writer.write("                     OFFICIAL ELECTION RESULTS REPORT                   \n");
            writer.write("========================================================================\n");
            writer.write(String.format("Election Name    : %s%n", electionName));
            writer.write(String.format("Voting Window    : %s  TO  %s%n",
                    votingStart.format(DATE_FORMATTER), votingEnd.format(DATE_FORMATTER)));
            writer.write(String.format("Report Generated : %s%n", LocalDateTime.now().format(DATE_FORMATTER)));
            writer.write("========================================================================\n\n");

            writer.write("--- CANDIDATE TALLY ---\n");
            writer.write(String.format("%-10s | %-24s | %-16s | %-6s | %-8s%n",
                    "ID", "Candidate Name", "Party", "Votes", "Share"));
            writer.write("-".repeat(72) + "\n");

            for (Map.Entry<Candidate, Integer> entry : results.entrySet()) {
                Candidate c = entry.getKey();
                int count = entry.getValue();
                double share = totalVotes == 0 ? 0.0 : ((double) count / totalVotes) * 100.0;
                writer.write(String.format("%-10s | %-24s | %-16s | %-6d | %6.2f%%%n",
                        c.getCandidateId(), c.getName(), c.getParty(), count, share));
            }
            writer.write("-".repeat(72) + "\n\n");

            writer.write("--- PARTICIPATION SUMMARY ---\n");
            writer.write(String.format("Total Votes Cast        : %d%n", totalVotes));
            writer.write(String.format("Total Registered Voters : %d%n", registeredVoters));
            writer.write(String.format("Turnout Percentage      : %.2f%%%n", turnout));
            if (winner != null) {
                writer.write(String.format("Declared Winner         : %s (%s) with %d votes%n",
                        winner.getName(), winner.getParty(), results.get(winner)));
            } else {
                writer.write("Declared Winner         : No votes cast yet\n");
            }
            writer.write("\n");

            writer.write("========================================================================\n");
            writer.write("                     OFFICIAL VOTE AUDIT LOG                           \n");
            writer.write(" Note: Individual candidate choices are withheld to preserve ballot     \n");
            writer.write(" secrecy. Encrypted vote IDs and SHA-256 receipt hashes are displayed. \n");
            writer.write("========================================================================\n");
            writer.write(String.format("%-38s | %-20s | %s%n", "Vote UUID", "Timestamp", "SHA-256 Receipt Hash"));
            writer.write("-".repeat(128) + "\n");

            if (voteLog.isEmpty()) {
                writer.write("No votes recorded in this election.\n");
            } else {
                for (Vote v : voteLog) {
                    writer.write(String.format("%-38s | %-20s | %s%n",
                            v.getVoteId(),
                            v.getTimestamp().format(DATE_FORMATTER),
                            v.getReceiptHash()));
                }
            }
            writer.write("-".repeat(128) + "\n");
            writer.write("### END OF REPORT ###\n");
        }
    }
}
