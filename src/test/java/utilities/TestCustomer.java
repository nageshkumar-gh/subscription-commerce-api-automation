package utilities;

//A customer registered by a test, with the token to act as them
public record TestCustomer(String id, String name, String email, String phone, String password, String token) {
}
