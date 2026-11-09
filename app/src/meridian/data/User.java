package meridian.data;

import java.time.LocalDateTime;

public final class User {
    public final String username;
    public final String fullName;
    public final String role;
    final String password;
    LocalDateTime lastLogin;

    public User(String username, String fullName, String role, String password) {
        this.username = username;
        this.fullName = fullName;
        this.role = role;
        this.password = password;
    }

    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }

    public LocalDateTime getLastLogin() {
        return lastLogin;
    }
}
