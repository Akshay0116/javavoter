package com.voting;

import java.time.LocalDateTime;

/**
 * Result payload returned when a voter enters their Voter ID and Name.
 */
public class VoterVerificationResult {
    public enum Status {
        ELIGIBLE_TO_VOTE,
        ALREADY_VOTED,
        VOTER_NOT_FOUND,
        NAME_MISMATCH
    }

    private final Status status;
    private final String message;
    private final String voterId;
    private final String voterName;
    private final LocalDateTime votedTimestamp;
    private final String receiptHash;
    private final String encryptedVoteId;

    public VoterVerificationResult(Status status, String message, String voterId, String voterName,
                                   LocalDateTime votedTimestamp, String receiptHash, String encryptedVoteId) {
        this.status = status;
        this.message = message;
        this.voterId = voterId;
        this.voterName = voterName;
        this.votedTimestamp = votedTimestamp;
        this.receiptHash = receiptHash;
        this.encryptedVoteId = encryptedVoteId;
    }

    public static VoterVerificationResult eligible(String voterId, String name) {
        return new VoterVerificationResult(Status.ELIGIBLE_TO_VOTE, "Voter verified. Ready to cast ballot.",
                voterId, name, null, null, null);
    }

    public static VoterVerificationResult alreadyVoted(String voterId, String name, LocalDateTime votedAt,
                                                       String receiptHash, String encryptedVoteId) {
        return new VoterVerificationResult(Status.ALREADY_VOTED,
                "You have already cast your vote in this election.",
                voterId, name, votedAt, receiptHash, encryptedVoteId);
    }

    public static VoterVerificationResult notFound(String voterId) {
        return new VoterVerificationResult(Status.VOTER_NOT_FOUND,
                "Voter ID '" + voterId + "' is not registered in the voter database.",
                voterId, null, null, null, null);
    }

    public static VoterVerificationResult nameMismatch(String voterId, String expectedName) {
        return new VoterVerificationResult(Status.NAME_MISMATCH,
                "Provided name does not match the registered voter record for ID '" + voterId + "'.",
                voterId, null, null, null, null);
    }

    public Status getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public String getVoterId() {
        return voterId;
    }

    public String getVoterName() {
        return voterName;
    }

    public LocalDateTime getVotedTimestamp() {
        return votedTimestamp;
    }

    public String getReceiptHash() {
        return receiptHash;
    }

    public String getEncryptedVoteId() {
        return encryptedVoteId;
    }
}
