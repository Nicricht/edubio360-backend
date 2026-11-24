package cl.edubio360.auth.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserRequest(
        @NotBlank @Email String email,
        @Size(min = 8, max = 72) String password,
        @NotBlank @Pattern(regexp = "STUDENT|ORIENTADOR|ADMIN") String role,
        boolean active,
        @Valid ProfileData perfil) {
    public UserRequest(String email, String password, String role, boolean active) {
        this(email, password, role, active, null);
    }
}
