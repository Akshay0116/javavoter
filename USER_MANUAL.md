# Online Voting System — User Manual

## 1. Introduction & Overview

The **Online Voting System** is a secure Java-based application implementing Object-Oriented Programming (OOP) principles to conduct elections. It features strict time-window enforcement, credential verification, duplicate vote prevention, cryptographic SHA-256 receipt generation, real-time result tabulation, and formatted file exports with voter secrecy preservation.

---

## 2. System Requirements & Prerequisites

* **Operating System:** Windows, macOS, or Linux.
* **Java Development Kit (JDK):** JDK 17, 21, or higher (Tested and certified on OpenJDK / Oracle JDK 26).
* **Terminal/Command Prompt:** PowerShell, Command Prompt, or Bash.
* **Build Tool:** Standard `javac` compiler (no Maven or Gradle required; completely self-contained).

---

## 3. Project Directory Structure

```text
javaproject222/
├── src/
│   └── com/
│       └── voting/
│           ├── Candidate.java                 # Candidate entity (ID, name, party)
│           ├── Voter.java                     # Voter entity (ID, name, 4-digit PIN, hasVoted)
│           ├── Vote.java                      # Immutable vote record with SHA-256 receipt
│           ├── VotingManager.java             # Election coordinator & 5-gate pipeline
│           ├── Main.java                      # Demo walkthrough & Interactive Console CLI
│           ├── exception/
│           │   ├── VotingException.java       # Base checked exception
│           │   ├── VotingWindowException.java # Gate 3a exception
│           │   ├── VoterNotFoundException.java# Gate 3b exception
│           │   ├── InvalidCredentialException.java # Gate 3c exception
│           │   ├── DuplicateVoteException.java# Gate 3d exception
│           │   └── CandidateNotFoundException.java # Gate 3e exception
│           └── test/
│               └── VotingSystemTestSuite.java # Automated test runner (61 test assertions)
├── bin/                                       # Compiled bytecode (.class files)
├── results_summary.txt                        # Exported election report & audit log
├── DESIGN_DOCUMENT.md                         # Architecture, UML diagrams, & security rationale
├── USER_MANUAL.md                             # Step-by-step operational guide (this file)
└── TEST_CASES.md                              # Formal test cases matrix & sample transcripts
```

---

## 4. Compilation & Build Instructions

Open your terminal in the root directory (`javaproject222`) and compile all source files into the `bin/` directory:

### Windows (PowerShell / Command Prompt):
```powershell
javac -d bin src\com\voting\*.java src\com\voting\exception\*.java src\com\voting\test\*.java
```

### macOS / Linux (Bash):
```bash
javac -d bin src/com/voting/*.java src/com/voting/exception/*.java src/com/voting/test/*.java
```

---

## 5. Execution Modes
 
 The application can be run in three distinct modes:
 
-### Mode 1: Automated Simulation Walkthrough
-Runs an end-to-end simulation of an entire election lifecycle, demonstrating all 8 phases, security gates, and exception triggers:
+### Mode 1: Embedded Real-Time Web Application (Default)
+Launches the real-time embedded HTTP server with the modern Glassmorphic web application at `http://localhost:8080/`:
 
 ```powershell
 java -cp bin com.voting.Main
 ```
 
-### Mode 2: Interactive Voting Console
-Launches an interactive menu where an election official or voter can register participants, cast ballots, view live counts, and export reports in real time:
+### Mode 2: Interactive Terminal Console CLI
+Launches a terminal menu where an election official or voter can register participants, cast ballots, view live counts, and export reports:
 
 ```powershell
 java -cp bin com.voting.Main --interactive
+```
+
+### Mode 3: Automated Simulation Walkthrough
+Runs a complete programmatic end-to-end walkthrough showing all validation gates and exception triggers:
+
+```powershell
+java -cp bin com.voting.Main --walkthrough
+```
+
+### Mode 4: Automated Test Suite Execution (76 Assertions)
+Executes the full automated regression test suite:
+
+```powershell
+java -cp bin com.voting.test.VotingSystemTestSuite
 ```

---

## 6. Step-by-Step Interactive Operational Guide

When running in **Interactive Mode** (`--interactive`), the system presents the following menu:

```text
=======================================================
       ONLINE VOTING SYSTEM - INTERACTIVE CONSOLE     
=======================================================
1. View Election Info & Voting Window
2. List Candidates
3. List Registered Voters
4. Register a New Candidate
5. Register a New Voter
6. Cast a Vote
7. Display Live Election Results
8. Export Summary Report & Audit Trail
9. Run Automated Simulation Walkthrough
0. Exit
Select an option (0-9):
```

### Step 1: Check Election Window (Option 1)
Displays the configured election name, opening timestamp, closing timestamp, and current active status (`ACTIVE / OPEN`, `NOT OPEN YET`, or `CLOSED`).

### Step 2: Register a Candidate (Option 4)
* **Candidate ID:** A non-blank unique identifier (e.g., `C10`).
* **Candidate Name:** Full display name (e.g., `Dr. Elena Rostova`).
* **Political Party:** Organization or party name. If left blank, it defaults automatically to `"Independent"`.

### Step 3: Register an Eligible Voter (Option 5)
* **Voter ID:** Unique identification code (e.g., `V205`).
* **Voter Name:** Constituent's full name (e.g., `Mark Johnson`).
* **PIN:** A private, exactly 4-digit numeric code (e.g., `4590`).
  * *Note:* Non-numeric characters or PINs shorter/longer than 4 digits are rejected immediately.

### Step 4: Cast a Ballot (Option 6)
To cast a vote, the user provides:
1. **Voter ID**
2. **4-Digit Secret PIN**
3. **Candidate ID**

#### Successful Vote:
```text
🎉 Vote recorded successfully!
   Receipt UUID : 484c23d2-8f7f-43c5-9da3-5d11c06ff6dc
   Receipt Hash : 818fca3d5986c6f1561c9d90e7f5e606ca9eee23ca287b53d8078863ede93634
```
The voter receives an immutable receipt containing a random UUID and a cryptographic SHA-256 hash.

### Step 5: View Real-Time Results (Option 7)
Displays the current standings in a clean formatted table:
* Candidate ID, Name, Political Party
* Votes garnered and percentage share of total votes cast
* Participation stats: Total votes cast, total registered voters, turnout %
* Current winner or leader

### Step 6: Export Official Summary & Audit Trail (Option 8)
Prompts for an output file path (press Enter for the default `results_summary.txt`). The system writes:
1. Official election header and voting window.
2. Complete candidate results table.
3. Overall voter turnout percentage.
4. Declared election winner.
5. **Cryptographic Audit Log:** Every vote UUID, timestamp, and SHA-256 receipt hash.
   * *Privacy Assurance:* Individual candidate choices are withheld to protect constitutional ballot secrecy.

---

## 7. How to Verify a Voter Receipt

A voter who has cast a ballot can independently verify that their vote was recorded without tampering:

1. Open the exported `results_summary.txt` report.
2. Scroll to the `OFFICIAL VOTE AUDIT LOG` section.
3. Search for your **Receipt Vote UUID** and compare the **SHA-256 Receipt Hash**.
4. If the hash matches the receipt given at vote submission time, the vote is verified as recorded and untampered with.

---

## 8. Error Codes & Troubleshooting Reference

| Error Encountered | Cause | Resolution |
| :--- | :--- | :--- |
| `VotingWindowException` | Vote attempted before `votingStart` or after `votingEnd`. | Votes can only be cast within the legal election hours. Wait until the window opens or review the closing time. |
| `VoterNotFoundException` | Submitted Voter ID does not exist in the voter registry. | Verify the voter ID spelling or register the voter prior to voting. |
| `InvalidCredentialException`| Submitted PIN does not match the registered 4-digit PIN. | Re-enter the correct 4-digit PIN for this voter. |
| `DuplicateVoteException` | The voter has already cast a ballot in this election. | The "One Voter, One Vote" rule is strictly enforced. The voter cannot vote again. |
| `CandidateNotFoundException`| Chosen Candidate ID does not exist in the candidate registry. | Select a valid registered candidate ID. The voter is **not** locked out and can re-attempt. |
| `IllegalArgumentException` | Registration attempted with blank names, invalid PIN lengths, or duplicate IDs. | Ensure IDs are unique and PINs are exactly 4 numeric digits. |
