package com.voting;

import java.util.Objects;

/**
 * Represents an eligible candidate running in the election.
 * Each candidate has an immutable ID, a display name, and an associated political party.
 */
public class Candidate {
    private final String candidateId;
    private final String name;
    private final String party;

    /**
     * Constructs a new Candidate.
     *
     * @param candidateId Unique identifier for the candidate (non-blank).
     * @param name        Full name of the candidate (non-blank).
     * @param party       Political party or affiliation (defaults to "Independent" if null/blank).
     * @throws IllegalArgumentException if candidateId or name is null or blank.
     */
    public Candidate(String candidateId, String name, String party) {
        if (candidateId == null || candidateId.trim().isEmpty()) {
            throw new IllegalArgumentException("Candidate ID cannot be null or blank");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Candidate name cannot be null or blank");
        }

        this.candidateId = candidateId.trim();
        this.name = name.trim();
        this.party = (party == null || party.trim().isEmpty()) ? "Independent" : party.trim();
    }

    public String getCandidateId() {
        return candidateId;
    }

    public String getName() {
        return name;
    }

    public String getParty() {
        return party;
    }

    /**
     * Equality is determined strictly by candidateId.
     * This ensures correct behavior when Candidate objects are used as keys in results maps.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Candidate candidate = (Candidate) o;
        return Objects.equals(candidateId, candidate.candidateId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(candidateId);
    }

    @Override
    public String toString() {
        return String.format("[%s] %s (%s)", candidateId, name, party);
    }

    /**
     * Standalone demonstration entry point for Candidate.
     */
    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("        CANDIDATE COMPONENT DEMONSTRATION        ");
        System.out.println("=================================================");

        Candidate c1 = new Candidate("C1", "Asha Rao", "Progress Alliance");
        Candidate c2 = new Candidate("C2", "Vikram Patel", ""); // Defaults to 'Independent'
        Candidate c3 = new Candidate("C3", "Meera Joshi", "Civic Unity Party");

        System.out.println("Candidate 1 : " + c1);
        System.out.println("Candidate 2 : " + c2 + " (Party defaulted from blank)");
        System.out.println("Candidate 3 : " + c3);

        System.out.println("\nEquality & HashCode Test (strictly by ID):");
        Candidate c1Duplicate = new Candidate("C1", "Asha R.", "Different Party");
        System.out.println("c1.equals(c1Duplicate) -> " + c1.equals(c1Duplicate) + " (Expected: true)");

        System.out.println("\nValidation Guard Test (blank name):");
        try {
            new Candidate("C4", "   ", "Independent");
            System.out.println("Failed: should have thrown exception");
        } catch (IllegalArgumentException e) {
            System.out.println("Caught expected exception: " + e.getMessage());
        }

        System.out.println("\nTo run the complete Online Voting System, execute:");
        System.out.println("  java -cp bin com.voting.Main");
        System.out.println("=================================================");
    }
}
