package com.voting.exception;

/**
 * Thrown when a voter ID cannot be found in the registered voters registry.
 */
public class VoterNotFoundException extends VotingException {
    private final String voterId;

    public VoterNotFoundException(String voterId) {
        super(String.format("Voter ID '%s' is not registered in this election.", voterId));
        this.voterId = voterId;
    }

    public String getVoterId() {
        return voterId;
    }
}
