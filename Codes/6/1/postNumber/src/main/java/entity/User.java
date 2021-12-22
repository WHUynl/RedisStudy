package entity;

import java.io.Serial;
import java.io.Serializable;

public class User implements Serializable{
    @Serial
    private static final long serialVersionUID = -498139362288544175L;

    public User(Integer id, String username, String password, String email, String telephone) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.email = email;
        this.telephone = telephone;
    }

    private Integer id;
    private String username;
    private String password;
    private String email;
    private String telephone;

    public User() {
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", email='" + email + '\'' +
                ", telephone='" + telephone + '\'' +
                '}';
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }
}
