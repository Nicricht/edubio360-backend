package cl.edubio360.academic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import java.math.BigDecimal;

public record OfertaRequest(
        @NotBlank String carrera,
        @NotBlank String modalidad,
        @NotBlank String jornada,
        @NotNull @PositiveOrZero BigDecimal arancel,
        @NotNull @PositiveOrZero BigDecimal matricula,
        @NotNull Long sedeId,
        @Size(max = 180) String area,
        @Min(1) @Max(24) Integer duracion,
        @Size(max = 180) String nivel,
        @Size(max = 2000) String descripcion,
        @Size(max = 180) String grupoComparacion,
        @Size(max = 300) String imagen,
        @PositiveOrZero Integer cupos,
        @PositiveOrZero Integer cupoCritico) {
    public OfertaRequest(String carrera, String modalidad, String jornada, BigDecimal arancel,
                         BigDecimal matricula, Long sedeId) {
        this(carrera, modalidad, jornada, arancel, matricula, sedeId, null, null, null, null, null, null, null, null);
    }
}
