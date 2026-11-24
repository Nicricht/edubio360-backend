package cl.edubio360.auth.dto;

import jakarta.validation.constraints.Size;

public record ProfileData(
        @Size(max = 20) String run,
        @Size(max = 50) String nombre,
        @Size(max = 100) String apellidos,
        @Size(max = 100) String region,
        @Size(max = 120) String comuna,
        @Size(max = 300) String direccion) {
}
