package com.voting.exception;

/**
 * Base checked exception for domain-specific voting operations.
 */
public class VotingException extends Exception {
    public VotingException(String message) {
        super(message);
    }

    public VotingException(String message, Throwable cause) {
        super(message, cause);
    }
}
