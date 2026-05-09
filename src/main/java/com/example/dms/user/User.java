package com.example.dms.user;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity // tells Hibernate this class represents a database table
@Table(name = "users") // tells Hibernate which table this maps to
//Data  generates getters, setters, toString(), and equals(), hashCode() this can cause problems with id
@Getter
@Setter
@Builder // provides a nice way to create complex objects (.builder().name()...)
@NoArgsConstructor // generates no args constructor
@AllArgsConstructor // generates all args constructor
public class User {

    @Id // mark as primary key of the table
    @GeneratedValue(strategy = GenerationType.IDENTITY) // tells Hibernate the database generates the ID automatically
    private Long id;

    @Column(nullable = false, unique = true) // maps to the database column, field is required, no two users can have the same email
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "first_name", nullable = false) // tells Hibernate which column to map to (different ways to write firstName/first_name)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Enumerated(EnumType.STRING) // tells Hibernate to store the role as a string
    @Column(nullable = false)
    private Role role; // ADMIN or USER

    @Column(nullable = false)
    private boolean active = true;

    @CreationTimestamp // sets the field once on insert
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt; //Java built-in type for date and time

    @UpdateTimestamp // refreshes it on every update.
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
