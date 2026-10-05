package models;

//Body of POST /api/auth/login
public record LoginRequest(String email, String password) {
}
