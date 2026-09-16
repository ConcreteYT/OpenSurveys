package com.opensurveys.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Core account entity. Serves two roles in this app:
//  1) Authentication subject - AuthController reads/writes this via UserRepository
//     to register/login, and JwtAuthFilter resolves the JWT subject (username) back
//     to a User on every authenticated request.
//  2) Form ownership - Form.creator points to this entity, and FormController exposes
//     creator.getUsername() as "creatorUsername" so anonymous users filling out a form
//     can see who created it.
//
// Maps onto the provided schema's USER table:
//   USER(user_id PK autoincrement, name, username, email, password)
// "USER" is a reserved word in MySQL, so the table name is wrapped in backticks -
// Hibernate treats backtick-wrapped names as "needs quoting" and translates that into
// whatever the active SQL dialect's quote character is, avoiding a DDL/query conflict.
@Entity
@Table(name = "`USER`")
public class User {

    public static final String ROLE_USER = "USER";
    public static final String ROLE_ADMIN = "ADMIN";

    // Java field name (id) is kept as-is so existing code (getId/setId callers) is
    // unaffected; only the DB column name changes to match the schema (user_id).
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(name = "username")
    private String username;

    @Column(name = "name")
    private String name;

    @Column(name = "email")
    private String email;

    // BCrypt hash only (set by AuthController via PasswordEncoder), never the raw password.
    // Null for Google-only accounts (no password login until they set one).
    // @JsonIgnore keeps it out of every JSON response (e.g. GET /users, form creator lookups).
    @JsonIgnore
    @Column(name = "password")
    private String password;

    // Google account `sub` claim. Used to find/link OAuth users; null for password-only accounts.
    @Column(name = "google_id")
    private String googleId;

    // Spring Security authority without the ROLE_ prefix (USER or ADMIN).
    // New signups default to USER; AdminBootstrap can create/promote an ADMIN for testing.
    // Nullable in DB so ddl-auto=update can add the column to existing USER rows without failing.
    @Column(name = "role")
    private String role = ROLE_USER;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

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

    public String getGoogleId() {
        return googleId;
    }

    public void setGoogleId(String googleId) {
        this.googleId = googleId;
    }

    public String getRole() {
        return role == null || role.isBlank() ? ROLE_USER : role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
