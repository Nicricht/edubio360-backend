package cl.edubio360.auth.dto;

public record UserResponse(Long id, String email, String role, boolean active, ProfileData perfil) {
    public UserResponse(Long id, String email, String role, boolean active) {
        this(id, email, role, active, null);
    }
}
