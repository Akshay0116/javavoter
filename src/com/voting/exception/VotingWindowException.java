package com.voting.exception;

/**
 * Thrown when an operation (e.g. casting a vote) is attempted outside the configured election window.
 */
public class VotingWindowException extends VotingException {
    public VotingWindowException(String message) {
        super(message);
    }
}
