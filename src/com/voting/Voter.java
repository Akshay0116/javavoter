package com.voting;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents a registered voter eligible to cast a single vote in the election.
 * Tracks identity, optional authentication PIN, voting status, and the exact timestamp/receipt
 * of when the ballot was cast.
 */
public class Voter {
    private final String voterId;
    private final String name;
    private final String pin;
    private boolean hasVoted;
    private LocalDateTime votedTimestamp;
    private String voteReceiptHash;
    private String encryptedVoteId;

    /**
     * Constructs a Voter with ID and Name (PIN defaults to "0000").
     */
    public Voter(String voterId, String name) {
        this(voterId, name, "0000");
    }

    /**
     * Constructs a Voter with ID, Name, and 4-digit PIN.
     */
    public Voter(String voterId, String name, String pin) {
        if (voterId == null || voterId.trim().isEmpty()) {
            throw new IllegalArgumentException("Voter ID cannot be null or blank");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Voter name cannot be null or blank");
        }
        if (pin == null || pin.length() != 4 || !pin.chars().allMatch(Character::isDigit)) {
            throw new IllegalArgumentException("PIN must be exactly 4 digits");
        }

        this.voterId = voterId.trim();
        this.name = name.trim();
        this.pin = pin;
        this.hasVoted = false;
        this.votedTimestamp = null;
        this.voteReceiptHash = null;
        this.encryptedVoteId = null;
    }

    public String getVoterId() {
        return voterId;
    }

    public String getName() {
        return name;
    }

    public boolean hasVoted() {
        return hasVoted;
    }

    public LocalDateTime getVotedTimestamp() {
        return votedTimestamp;
    }

    public String getVoteReceiptHash() {
        return voteReceiptHash;
    }

    public String getEncryptedVoteId() {
        return encryptedVoteId;
    }

    /**
     * Package-visible credential check.
     */
    boolean verifyPin(String submittedPin) {
        return submittedPin != null && this.pin.equals(submittedPin);
    }

    /**
     * Package-visible mutation of voting status with receipt metadata.
     */
    void markAsVoted(LocalDateTime timestamp, String receiptHash, String encryptedVoteId) {
        this.hasVoted = true;
        this.votedTimestamp = timestamp;
        this.voteReceiptHash = receiptHash;
        this.encryptedVoteId = encryptedVoteId;
    }

    void markAsVoted() {
        markAsVoted(LocalDateTime.now(), null, null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Voter voter = (Voter) o;
        return Objects.equals(voterId.toUpperCase(), voter.voterId.toUpperCase());
    }

    @Override
    public int hashCode() {
        return Objects.hash(voterId.toUpperCase());
    }

    @Override
    public String toString() {
        return String.format("Voter[id=%s, name=%s, hasVoted=%b, votedAt=%s]",
                voterId, name, hasVoted, votedTimestamp);
    }
}
