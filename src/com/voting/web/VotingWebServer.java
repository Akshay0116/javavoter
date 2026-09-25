package com.voting.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.voting.*;
import com.voting.exception.VotingException;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Built-in embedded HTTP Web Server providing real-time REST endpoints and serving
 * the modern Glassmorphic / Minimal Web Application interface.
 * Features Real Two-Factor Authentication (RFC 6238 TOTP) compatible with
 * Google Authenticator, Microsoft Authenticator, Authy, and Apple Passwords.
 */
public class VotingWebServer {
    private static final String ADMIN_USER = "admin";
    private static final String ADMIN_PASS = "admin123";
    private static final String ADMIN_TOTP_SECRET = TotpUtil.DEFAULT_ADMIN_SECRET;

    private final VotingManager manager;
    private final int port;
    private HttpServer server;

    // Active Admin 2FA Sessions
    private String activeTempToken = null;
    private long tempTokenExpiresAt = 0;
    private final Set<String> validAdminSessions = Collections.synchronizedSet(new HashSet<>());

    public VotingWebServer(VotingManager manager, int port) {
        this.manager = manager;
        this.port = port;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);

        // Public Voter API routes
        server.createContext("/api/voter/verify", new VoterVerifyHandler());
        server.createContext("/api/voter/cast", new VoteCastHandler());
        server.createContext("/api/candidates", new CandidatesHandler());
        server.createContext("/api/live-results", new LiveResultsHandler());

        // Admin Real TOTP 2FA routes
        server.createContext("/api/admin/login", new AdminLoginHandler());
        server.createContext("/api/admin/verify-2fa", new AdminVerify2faHandler());
        server.createContext("/api/admin/logout", new AdminLogoutHandler());
        server.createContext("/api/admin/check-session", new AdminCheckSessionHandler());

        // Protected Admin Action routes
        server.createContext("/api/admin/candidate", new AdminAddCandidateHandler());
        server.createContext("/api/admin/voter", new AdminAddVoterHandler());
        server.createContext("/api/audit-log", new AuditLogHandler());
        server.createContext("/api/admin/export", new ExportHandler());

        // Static frontend route
        server.createContext("/", new StaticFileHandler());

        server.setExecutor(null);
        server.start();
        System.out.println("====================================================================");
        System.out.printf("  🚀 ONLINE VOTING WEB APP STARTED AT: http://localhost:%d/%n", port);
        System.out.println("====================================================================");
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    private boolean isAuthorizedAdmin(HttpExchange exchange) {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            return validAdminSessions.contains(token);
        }
        return false;
    }

    // --- REAL RFC 6238 TOTP 2FA HANDLERS ---

    private class AdminLoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendCors(exchange);
                return;
            }
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            Map<String, String> body = parseJsonMap(readRequestBody(exchange));
            String user = body.getOrDefault("username", "").trim();
            String pass = body.getOrDefault("password", "").trim();

            if (ADMIN_USER.equals(user) && ADMIN_PASS.equals(pass)) {
                // Password verified. Now require second factor (TOTP Authenticator code).
                activeTempToken = UUID.randomUUID().toString();
                tempTokenExpiresAt = System.currentTimeMillis() + (10 * 60 * 1000); // 10 minutes

                String otpAuthUri = TotpUtil.getOtpAuthUri(ADMIN_TOTP_SECRET);
                String qrCodeUrl = TotpUtil.getQrCodeUrl(otpAuthUri);

                StringBuilder json = new StringBuilder("{");
                json.append("\"success\":true,");
                json.append("\"step\":\"2FA_REQUIRED\",");
                json.append("\"tempToken\":\"").append(activeTempToken).append("\",");
                json.append("\"secretKey\":\"").append(ADMIN_TOTP_SECRET).append("\",");
                json.append("\"qrCodeUrl\":\"").append(escape(qrCodeUrl)).append("\",");
                json.append("\"message\":\"Two-Factor Authentication required. Scan the QR code in Google Authenticator or enter the secret key manually.\"");
                json.append("}");

                sendResponse(exchange, 200, json.toString());
            } else {
                sendResponse(exchange, 401, "{\"success\":false,\"error\":\"Invalid administrator username or password.\"}");
            }
        }
    }

    private class AdminVerify2faHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendCors(exchange);
                return;
            }
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            Map<String, String> body = parseJsonMap(readRequestBody(exchange));
            String tempToken = body.getOrDefault("tempToken", "").trim();
            String otp = body.getOrDefault("otp", "").trim();

            if (activeTempToken == null || !activeTempToken.equals(tempToken) || System.currentTimeMillis() > tempTokenExpiresAt) {
                sendResponse(exchange, 401, "{\"success\":false,\"error\":\"Session expired or invalid login challenge. Please enter your credentials again.\"}");
                return;
            }

            // Real mathematical RFC 6238 TOTP verification against current time
            boolean codeIsValid = TotpUtil.verifyCode(ADMIN_TOTP_SECRET, otp);

            if (codeIsValid) {
                activeTempToken = null;
                String sessionToken = UUID.randomUUID().toString();
                validAdminSessions.add(sessionToken);

                StringBuilder json = new StringBuilder("{");
                json.append("\"success\":true,");
                json.append("\"adminToken\":\"").append(sessionToken).append("\",");
                json.append("\"message\":\"Two-Factor Authentication successful. Admin session granted.\"");
                json.append("}");

                sendResponse(exchange, 200, json.toString());
            } else {
                sendResponse(exchange, 401, "{\"success\":false,\"error\":\"Invalid 6-digit security code. Check the current code on your Google/Microsoft Authenticator app and try again.\"}");
            }
        }
    }

    private class AdminCheckSessionHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendCors(exchange);
                return;
            }
            boolean valid = isAuthorizedAdmin(exchange);
            sendResponse(exchange, 200, "{\"authenticated\":" + valid + "}");
        }
    }

    private class AdminLogoutHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendCors(exchange);
                return;
            }
            String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                validAdminSessions.remove(authHeader.substring(7).trim());
            }
            sendResponse(exchange, 200, "{\"success\":true,\"message\":\"Logged out successfully.\"}");
        }
    }

    // --- VOTER HANDLERS ---

    private class VoterVerifyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendCors(exchange);
                return;
            }
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            Map<String, String> body = parseJsonMap(readRequestBody(exchange));
            String voterId = body.getOrDefault("voterId", "");
            String name = body.getOrDefault("name", "");

            VoterVerificationResult result = manager.verifyVoter(voterId, name);

            StringBuilder json = new StringBuilder("{");
            json.append("\"status\":\"").append(result.getStatus().name()).append("\",");
            json.append("\"message\":\"").append(escape(result.getMessage())).append("\",");
            json.append("\"voterId\":\"").append(escape(result.getVoterId() == null ? "" : result.getVoterId())).append("\",");
            json.append("\"voterName\":\"").append(escape(result.getVoterName() == null ? "" : result.getVoterName())).append("\",");
            json.append("\"votedTimestamp\":\"").append(result.getVotedTimestamp() == null ? "" : result.getVotedTimestamp().format(VotingManager.DATE_FORMATTER)).append("\",");
            json.append("\"receiptHash\":\"").append(escape(result.getReceiptHash() == null ? "" : result.getReceiptHash())).append("\",");
            json.append("\"encryptedVoteId\":\"").append(escape(result.getEncryptedVoteId() == null ? "" : result.getEncryptedVoteId())).append("\"");
            json.append("}");

            sendResponse(exchange, 200, json.toString());
        }
    }

    private class VoteCastHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendCors(exchange);
                return;
            }
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            Map<String, String> body = parseJsonMap(readRequestBody(exchange));
            String voterId = body.getOrDefault("voterId", "");
            String name = body.getOrDefault("name", "");
            String candidateId = body.getOrDefault("candidateId", "");

            try {
                Vote vote = manager.castVoteByVoter(voterId, name, candidateId);
                StringBuilder json = new StringBuilder("{");
                json.append("\"success\":true,");
                json.append("\"message\":\"Vote encrypted and recorded in real time.\",");
                json.append("\"voteId\":\"").append(vote.getVoteId()).append("\",");
                json.append("\"encryptedVoteId\":\"").append(escape(vote.getEncryptedVoteId())).append("\",");
                json.append("\"receiptHash\":\"").append(vote.getReceiptHash()).append("\",");
                json.append("\"timestamp\":\"").append(vote.getTimestamp().format(VotingManager.DATE_FORMATTER)).append("\"");
                json.append("}");
                sendResponse(exchange, 200, json.toString());
            } catch (VotingException e) {
                sendResponse(exchange, 400, "{\"success\":false,\"error\":\"" + escape(e.getMessage()) + "\"}");
            } catch (Exception e) {
                sendResponse(exchange, 500, "{\"success\":false,\"error\":\"Internal error: " + escape(e.getMessage()) + "\"}");
            }
        }
    }

    private class CandidatesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendCors(exchange);
                return;
            }
            List<Candidate> list = manager.getCandidateList();
            StringBuilder json = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) {
                Candidate c = list.get(i);
                if (i > 0) json.append(",");
                json.append("{")
                        .append("\"candidateId\":\"").append(escape(c.getCandidateId())).append("\",")
                        .append("\"name\":\"").append(escape(c.getName())).append("\",")
                        .append("\"party\":\"").append(escape(c.getParty())).append("\"")
                        .append("}");
            }
            json.append("]");
            sendResponse(exchange, 200, json.toString());
        }
    }

    private class LiveResultsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendCors(exchange);
                return;
            }

            Map<Candidate, Integer> results = manager.getResults();
            Candidate winner = manager.getWinner();
            int totalVotes = manager.getTotalVotesCast();
            int totalVoters = manager.getTotalRegisteredVoters();
            double turnout = manager.getTurnoutPercentage();

            LocalDateTime now = LocalDateTime.now();
            String windowStatus = now.isBefore(manager.getVotingStart()) ? "NOT_STARTED"
                    : (now.isAfter(manager.getVotingEnd()) ? "CLOSED" : "ACTIVE");

            StringBuilder json = new StringBuilder("{");
            json.append("\"electionName\":\"").append(escape(manager.getElectionName())).append("\",");
            json.append("\"votingStart\":\"").append(manager.getVotingStart().format(VotingManager.DATE_FORMATTER)).append("\",");
            json.append("\"votingEnd\":\"").append(manager.getVotingEnd().format(VotingManager.DATE_FORMATTER)).append("\",");
            json.append("\"windowStatus\":\"").append(windowStatus).append("\",");
            json.append("\"totalVotes\":").append(totalVotes).append(",");
            json.append("\"totalVoters\":").append(totalVoters).append(",");
            json.append("\"turnout\":").append(String.format(Locale.US, "%.2f", turnout)).append(",");
            json.append("\"winner\":").append(winner == null ? "null" : "{\"candidateId\":\"" + escape(winner.getCandidateId()) + "\",\"name\":\"" + escape(winner.getName()) + "\",\"party\":\"" + escape(winner.getParty()) + "\"}").append(",");

            json.append("\"tallies\":[");
            int i = 0;
            for (Map.Entry<Candidate, Integer> entry : results.entrySet()) {
                if (i > 0) json.append(",");
                Candidate c = entry.getKey();
                int count = entry.getValue();
                double share = totalVotes == 0 ? 0.0 : ((double) count / totalVotes) * 100.0;
                json.append("{")
                        .append("\"candidateId\":\"").append(escape(c.getCandidateId())).append("\",")
                        .append("\"name\":\"").append(escape(c.getName())).append("\",")
                        .append("\"party\":\"").append(escape(c.getParty())).append("\",")
                        .append("\"votes\":").append(count).append(",")
                        .append("\"share\":").append(String.format(Locale.US, "%.2f", share))
                        .append("}");
                i++;
            }
            json.append("]}");

            sendResponse(exchange, 200, json.toString());
        }
    }

    private class AdminAddCandidateHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendCors(exchange);
                return;
            }
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }
            if (!isAuthorizedAdmin(exchange)) {
                sendResponse(exchange, 401, "{\"success\":false,\"error\":\"Unauthorized. Administrator login and 2FA required.\"}");
                return;
            }

            Map<String, String> body = parseJsonMap(readRequestBody(exchange));
            try {
                Candidate c = new Candidate(body.get("candidateId"), body.get("name"), body.get("party"));
                manager.registerCandidate(c);
                sendResponse(exchange, 200, "{\"success\":true,\"message\":\"Candidate registered successfully.\"}");
            } catch (Exception e) {
                sendResponse(exchange, 400, "{\"success\":false,\"error\":\"" + escape(e.getMessage()) + "\"}");
            }
        }
    }

    private class AdminAddVoterHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendCors(exchange);
                return;
            }
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }
            if (!isAuthorizedAdmin(exchange)) {
                sendResponse(exchange, 401, "{\"success\":false,\"error\":\"Unauthorized. Administrator login and 2FA required.\"}");
                return;
            }

            Map<String, String> body = parseJsonMap(readRequestBody(exchange));
            try {
                String pin = body.getOrDefault("pin", "0000");
                if (pin.isEmpty()) pin = "0000";
                Voter v = new Voter(body.get("voterId"), body.get("name"), pin);
                manager.registerVoter(v);
                sendResponse(exchange, 200, "{\"success\":true,\"message\":\"Voter registered successfully.\"}");
            } catch (Exception e) {
                sendResponse(exchange, 400, "{\"success\":false,\"error\":\"" + escape(e.getMessage()) + "\"}");
            }
        }
    }

    private class AuditLogHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendCors(exchange);
                return;
            }
            List<Vote> list = manager.getVoteAuditLog();
            StringBuilder json = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) {
                Vote v = list.get(i);
                if (i > 0) json.append(",");
                json.append("{")
                        .append("\"voteId\":\"").append(v.getVoteId()).append("\",")
                        .append("\"encryptedVoteId\":\"").append(escape(v.getEncryptedVoteId())).append("\",")
                        .append("\"timestamp\":\"").append(v.getTimestamp().format(VotingManager.DATE_FORMATTER)).append("\",")
                        .append("\"receiptHash\":\"").append(v.getReceiptHash()).append("\"")
                        .append("}");
            }
            json.append("]");
            sendResponse(exchange, 200, json.toString());
        }
    }

    private class ExportHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendCors(exchange);
                return;
            }
            if (!isAuthorizedAdmin(exchange)) {
                sendResponse(exchange, 401, "{\"success\":false,\"error\":\"Unauthorized. Administrator login and 2FA required.\"}");
                return;
            }

            try {
                String fileName = "results_summary.txt";
                manager.exportSummary(fileName);

                File f = new File(fileName);
                byte[] bytes = java.nio.file.Files.readAllBytes(f.toPath());

                exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
                exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.sendResponseHeaders(200, bytes.length);

                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            } catch (Exception e) {
                sendResponse(exchange, 500, "{\"success\":false,\"error\":\"Export error: " + escape(e.getMessage()) + "\"}");
            }
        }
    }

    private class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path == null || path.equals("/") || path.equals("/index.html")) {
                File htmlFile = new File("web/index.html");
                if (htmlFile.exists()) {
                    byte[] bytes = java.nio.file.Files.readAllBytes(htmlFile.toPath());
                    exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                    exchange.sendResponseHeaders(200, bytes.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(bytes);
                    }
                    return;
                }
            }

            File f = new File("web" + path);
            if (f.exists() && !f.isDirectory()) {
                byte[] bytes = java.nio.file.Files.readAllBytes(f.toPath());
                String mime = path.endsWith(".css") ? "text/css" : (path.endsWith(".js") ? "application/javascript" : "text/plain");
                exchange.getResponseHeaders().set("Content-Type", mime + "; charset=UTF-8");
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
                return;
            }

            sendResponse(exchange, 404, "404 Not Found");
        }
    }

    private void sendCors(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
        exchange.sendResponseHeaders(204, -1);
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String responseJson) throws IOException {
        byte[] bytes = responseJson.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody();
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[1024];
            int n;
            while ((n = is.read(buf)) != -1) {
                bos.write(buf, 0, n);
            }
            return bos.toString(StandardCharsets.UTF_8);
        }
    }

    private static Map<String, String> parseJsonMap(String json) {
        Map<String, String> map = new HashMap<>();
        if (json == null) return map;
        String trimmed = json.trim();
        if (trimmed.startsWith("{")) trimmed = trimmed.substring(1);
        if (trimmed.endsWith("}")) trimmed = trimmed.substring(0, trimmed.length() - 1);

        String[] pairs = trimmed.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
        for (String pair : pairs) {
            String[] kv = pair.split(":(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
            if (kv.length == 2) {
                String key = kv[0].trim().replaceAll("^\"|\"$", "");
                String val = kv[1].trim().replaceAll("^\"|\"$", "");
                map.put(key, val);
            }
        }
        return map;
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
