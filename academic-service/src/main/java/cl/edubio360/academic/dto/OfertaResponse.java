package cl.edubio360.academic.dto;

import java.math.BigDecimal;

public record OfertaResponse(
        Long id,
        String carrera,
        String institucion,
        Long sedeId,
        String sede,
        String comuna,
        String modalidad,
        String jornada,
        BigDecimal arancel,
        BigDecimal matricula,
        String area,
        Integer duracion,
        String nivel,
        String descripcion,
        String grupoComparacion,
        String imagen,
        Integer cupos,
        Integer cupoCritico) {
    public OfertaResponse(Long id, String carrera, String institucion, Long sedeId, String sede,
                          String comuna, String modalidad, String jornada, BigDecimal arancel, BigDecimal matricula) {
        this(id, carrera, institucion, sedeId, sede, comuna, modalidad, jornada, arancel, matricula,
                null, null, null, null, null, null, null, null);
    }
}
