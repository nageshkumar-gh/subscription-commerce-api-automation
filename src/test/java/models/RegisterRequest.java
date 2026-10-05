package models;

//Body of POST /api/auth/register
public record RegisterRequest(String name, String email, String phone, String password) {

    public RegisterRequest withName(String newName) {
        return new RegisterRequest(newName, email, phone, password);
    }

    public RegisterRequest withEmail(String newEmail) {
        return new RegisterRequest(name, newEmail, phone, password);
    }

    public RegisterRequest withPhone(String newPhone) {
        return new RegisterRequest(name, email, newPhone, password);
    }

    public RegisterRequest withPassword(String newPassword) {
        return new RegisterRequest(name, email, phone, newPassword);
    }
}
