package com.voting.exception;

/**
 * Thrown when an invalid credential (such as an incorrect PIN) is provided for a registered voter.
 */
public class InvalidCredentialException extends VotingException {
    private final String voterId;

    public InvalidCredentialException(String voterId) {
        super(String.format("Authentication failed for Voter ID '%s': Invalid PIN provided.", voterId));
        this.voterId = voterId;
    }

    public String getVoterId() {
        return voterId;
    }
}
