package com.voting;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents an immutable vote record created upon successful voting.
 * Contains voter ID, candidate ID, unique vote ID, timestamp, and military-grade
 * AES-256-GCM encryption tokens for vote ID and ballot payload, plus a SHA-256 receipt hash.
 */
public final class Vote {
    private final String voteId;
    private final String voterId;
    private final String candidateId;
    private final LocalDateTime timestamp;
    private final String receiptHash;
    private final String encryptedVoteId;
    private final String encryptedPayload;

    public Vote(String voterId, String candidateId) {
        this(voterId, candidateId, LocalDateTime.now());
    }

    public Vote(String voterId, String candidateId, LocalDateTime timestamp) {
        if (voterId == null || voterId.trim().isEmpty()) {
            throw new IllegalArgumentException("Voter ID cannot be null or blank");
        }
        if (candidateId == null || candidateId.trim().isEmpty()) {
            throw new IllegalArgumentException("Candidate ID cannot be null or blank");
        }
        Objects.requireNonNull(timestamp, "Timestamp cannot be null");

        this.voteId = UUID.randomUUID().toString();
        this.voterId = voterId.trim();
        this.candidateId = candidateId.trim();
        this.timestamp = timestamp;

        // Compute SHA-256 public verification hash
        this.receiptHash = EncryptionUtil.sha256(this.voteId + ":" + this.voterId + ":" + this.candidateId + ":" + this.timestamp);

        // Encrypt vote ID and vote payload with AES-256-GCM
        this.encryptedVoteId = EncryptionUtil.encrypt(this.voteId);
        String payloadToEncrypt = String.format("VOTER=%s|CANDIDATE=%s|TIMESTAMP=%s|VOTE_ID=%s",
                this.voterId, this.candidateId, this.timestamp, this.voteId);
        this.encryptedPayload = EncryptionUtil.encrypt(payloadToEncrypt);
    }

    public String getVoteId() {
        return voteId;
    }

    public String getVoterId() {
        return voterId;
    }

    public String getCandidateId() {
        return candidateId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getReceiptHash() {
        return receiptHash;
    }

    public String getEncryptedVoteId() {
        return encryptedVoteId;
    }

    public String getEncryptedPayload() {
        return encryptedPayload;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Vote vote = (Vote) o;
        return Objects.equals(voteId, vote.voteId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(voteId);
    }

    @Override
    public String toString() {
        return String.format("Vote[ID=%s, Voter=%s, EncryptedID=%s..., Hash=%s...]",
                voteId, voterId, encryptedVoteId.substring(0, 12), receiptHash.substring(0, 12));
    }
}
