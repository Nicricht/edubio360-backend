package cl.edubio360.guidance.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
public record GestionSolicitudRequest(
        @NotBlank @Pattern(regexp = "CONFIRMADA|REPROGRAMADA|COMPLETADA|CANCELADA") String estado,
        LocalDateTime fechaHora,
        @Size(max = 1000) String resultado,
        @Size(max = 1000) String observacion) {}
