package cl.edubio360.auth.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthRequests {
    private AuthRequests() {}

    public record RegisterRequest(@NotBlank @Email String email,
                                  @NotBlank @Size(min = 8, max = 72) String password, @Valid ProfileData perfil) {
        public RegisterRequest(String email, String password) { this(email, password, null); }
    }

    public record LoginRequest(@NotBlank @Email String email,
                               @NotBlank String password) {}

    public record LoginResponse(String token, String email, String role) {}
}
