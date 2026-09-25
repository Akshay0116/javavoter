# 🗳️ SecureVote — Online Voting System in Java

A modern, secure, and production-grade **Online Voting System** designed and implemented using Object-Oriented Programming (OOP) principles in Java. Features military-grade **AES-256-GCM** encryption, **SHA-256** cryptographic receipts, fail-fast election windows, duplicate vote prevention, real-time live tabulation, and an embedded zero-dependency **Glassmorphic Web Application**.

---

## 🌟 Key Features

* **Strict OOP Architecture:** Modular classes (`Voter`, `Candidate`, `Vote`, `VotingManager`) with complete encapsulation and immutable records.
* **Fail-Fast Election Window:** Elections are locked to a fixed time window (`votingStart` to `votingEnd`); out-of-window requests fail immediately at Gate 1, preventing information leakage.
* **Constituent Verification:** Authenticates voters via Voter ID and registered Full Name in real time.
* **Already-Voted Detection:** If a voter has already cast their ballot, the system immediately displays when they voted, the exact timestamp, their encrypted vote token, and their SHA-256 receipt hash.
* **Military-Grade AES-256-GCM Encryption:** Vote IDs and ballot payloads are encrypted in real time before permanent ledger commit.
* **Ballot Secrecy & Auditability:** Generates verifiable SHA-256 receipt hashes. Public audit logs conceal individual ballot choices while allowing voters to confirm inclusion.
* **Real-Time Web Application:** Embedded Java HTTP server hosting a sleek Glassmorphic dashboard with 2-second background polling, turnout gauges, and candidate progress bars.
* **Comprehensive Test Suite:** 76 automated test assertions validating all 8 workflow phases, edge cases, boundaries, and security gates.

---

## 📁 Repository Structure

```text
├── src/
│   └── com/
│       └── voting/
│           ├── Candidate.java                 # Candidate entity (ID, name, party)
│           ├── Voter.java                     # Voter entity (ID, name, status, timestamps)
│           ├── Vote.java                      # Immutable vote record with AES-256 & SHA-256
│           ├── VotingManager.java             # Election coordinator & 5-gate pipeline
│           ├── VoterVerificationResult.java   # Verification payload model
│           ├── EncryptionUtil.java            # AES-256-GCM and SHA-256 cryptographic utility
│           ├── Main.java                      # Main application entry point
│           ├── exception/                     # Custom checked exception hierarchy
│           ├── web/
│           │   └── VotingWebServer.java       # Embedded REST API & HTTP server
│           └── test/
│               └── VotingSystemTestSuite.java # Automated test suite (76 assertions)
├── web/
│   └── index.html                             # Real-Time Glassmorphic SPA frontend
├── DESIGN_DOCUMENT.md                         # Architecture, UML & sequence diagrams
├── USER_MANUAL.md                             # Step-by-step setup & execution guide
├── TEST_CASES.md                              # Formal test cases matrix & sample transcripts
└── results_summary.txt                        # Sample exported report with audit log
```

---

## 🚀 Quick Start Guide

### 1. Compile All Files
```bash
javac -d bin src/com/voting/*.java src/com/voting/exception/*.java src/com/voting/web/*.java src/com/voting/test/*.java
```

### 2. Run the Real-Time Web Application (Default)
```bash
java -cp bin com.voting.Main
```
Then open your browser at: **`http://localhost:8080/`**

### 3. Run the Automated Test Suite (76 Assertions)
```bash
java -cp bin com.voting.test.VotingSystemTestSuite
```

### 4. Run the Terminal Interactive Menu
```bash
java -cp bin com.voting.Main --interactive
```

### 5. Run the Automated Walkthrough Simulation
```bash
java -cp bin com.voting.Main --walkthrough
```

---

## 📚 Deliverables & Documentation

* 📖 **[Design Document (DESIGN_DOCUMENT.md)](DESIGN_DOCUMENT.md):** Complete specifications, UML class diagrams, 5-gate sequence diagrams, and security rationale.
* 🛠️ **[User Manual (USER_MANUAL.md)](USER_MANUAL.md):** Detailed guide on installation, execution modes, receipt verification, and troubleshooting.
* 🧪 **[Test Cases (TEST_CASES.md)](TEST_CASES.md):** 24 formal test scenarios with execution transcripts and coverage metrics.
