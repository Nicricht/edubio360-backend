package cl.edubio360.auth.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(name = "uk_users_email", columnNames = "email"))
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 180)
    private String email;

    @Column(nullable = false, length = 120)
    private String passwordHash;

    @Column(nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<UserRole> roles = new ArrayList<>();

    @Column(length = 20)
    private String run;
    @Column(length = 50)
    private String nombre;
    @Column(length = 100)
    private String apellidos;
    @Column(length = 100)
    private String region;
    @Column(length = 120)
    private String comuna;
    @Column(length = 300)
    private String direccion;

    protected UserEntity() {}

    public UserEntity(String email, String passwordHash, String role) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.active = true;
        addRole(role);
    }

    public void actualizar(String email, String passwordHash, String role, boolean active) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.active = active;
        replaceRoles(List.of(role));
    }

    public void addRole(String role) {
        UserRole assignment = new UserRole(role, this);
        roles.add(assignment);
    }

    public void replaceRoles(List<String> nuevas) {
        roles.clear();
        if (nuevas != null) nuevas.forEach(this::addRole);
    }

    public void actualizarPerfil(cl.edubio360.auth.dto.ProfileData perfil) {
        if (perfil == null) return;
        run = perfil.run();
        nombre = perfil.nombre();
        apellidos = perfil.apellidos();
        region = perfil.region();
        comuna = perfil.comuna();
        direccion = perfil.direccion();
    }

    public cl.edubio360.auth.dto.ProfileData getPerfil() {
        return new cl.edubio360.auth.dto.ProfileData(run, nombre, apellidos, region, comuna, direccion);
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public boolean isActive() { return active; }
    public List<UserRole> getRoles() { return roles; }
    public String getPrimaryRole() { return roles.isEmpty() ? "STUDENT" : roles.get(0).getName(); }
}
