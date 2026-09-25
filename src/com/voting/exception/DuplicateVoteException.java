package com.voting.exception;

/**
 * Thrown when a voter who has already cast a vote attempts to vote again.
 */
public class DuplicateVoteException extends VotingException {
    private final String voterId;

    public DuplicateVoteException(String voterId) {
        super(String.format("Duplicate vote rejected: Voter ID '%s' has already voted.", voterId));
        this.voterId = voterId;
    }

    public String getVoterId() {
        return voterId;
    }
}
