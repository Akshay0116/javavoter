package com.voting.exception;

/**
 * Thrown when a vote is cast for a candidate ID that does not exist in the candidate registry.
 */
public class CandidateNotFoundException extends VotingException {
    private final String candidateId;

    public CandidateNotFoundException(String candidateId) {
        super(String.format("Candidate ID '%s' is not registered in this election.", candidateId));
        this.candidateId = candidateId;
    }

    public String getCandidateId() {
        return candidateId;
    }
}
