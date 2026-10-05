package models;

import com.fasterxml.jackson.annotation.JsonInclude;

//Fields left null are not sent, so an update only touches what the test sets
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CustomerRequest {

    private String email;
    private String password;
    private String name;
    private String phone;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
