package cl.edubio360.guidance.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "solicitudes_orientacion")
public class SolicitudOrientacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String estudianteEmail;

    @Column(nullable = false)
    private Long ofertaId;

    @Column(nullable = false, length = 1000)
    private String motivo;

    @Column(nullable = false)
    private LocalDateTime fechaHora;

    @Column(nullable = false, length = 30)
    private String estado;

    private String orientadorEmail;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "solicitud", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<HistorialSolicitud> historial = new ArrayList<>();

    @Column(length = 1000)
    private String resultado;
    @Column(length = 1000)
    private String observacion;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "solicitud_ofertas_comparadas", joinColumns = @JoinColumn(name = "solicitud_id"))
    @Column(name = "oferta_id")
    private java.util.Set<Long> ofertasComparadas = new java.util.LinkedHashSet<>();

    protected SolicitudOrientacion() {}

    public SolicitudOrientacion(String estudianteEmail, Long ofertaId, String motivo, LocalDateTime fechaHora) {
        this.estudianteEmail = estudianteEmail;
        this.ofertaId = ofertaId;
        this.motivo = motivo;
        this.fechaHora = fechaHora;
        this.estado = "PENDIENTE";
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
        addHistorial("PENDIENTE", "Solicitud creada");
    }

    public void actualizar(String motivo, LocalDateTime fechaHora) {
        if ("COMPLETADA".equals(estado) || "CANCELADA".equals(estado)) {
            throw new IllegalStateException("La solicitud no puede editarse desde el estado " + estado);
        }
        this.motivo = motivo;
        this.fechaHora = fechaHora;
        this.updatedAt = LocalDateTime.now();
        addHistorial(estado, "Solicitud actualizada");
    }

    public void confirmar(String orientadorEmail) {
        if (this.orientadorEmail != null && !this.orientadorEmail.equalsIgnoreCase(orientadorEmail)) throw new IllegalStateException("La solicitud ya tiene orientador");
        if (!estado.equals("PENDIENTE") && !estado.equals("REPROGRAMADA")) {
            throw new IllegalStateException("La solicitud no puede confirmarse desde el estado " + estado);
        }
        this.estado = "CONFIRMADA";
        this.orientadorEmail = orientadorEmail;
        this.updatedAt = LocalDateTime.now();
        addHistorial("CONFIRMADA", "Solicitud confirmada");
    }

    public void cancelar() {
        if (estado.equals("COMPLETADA") || estado.equals("CANCELADA") || estado.equals("RECHAZADA")) {
            throw new IllegalStateException("La solicitud no puede cancelarse desde el estado " + estado);
        }
        this.estado = "CANCELADA";
        this.updatedAt = LocalDateTime.now();
        addHistorial("CANCELADA", "Solicitud cancelada");
    }

    public void guardarComparadas(java.util.List<Long> ids) {
        if (ids != null) ofertasComparadas.addAll(ids);
    }

    public void reprogramar(LocalDateTime fecha) {
        if (estado.equals("COMPLETADA") || estado.equals("CANCELADA")) throw new IllegalStateException("Solicitud finalizada");
        if (fecha == null || fecha.isBefore(LocalDateTime.now())) throw new IllegalStateException("Fecha inválida");
        fechaHora = fecha;
        estado = "REPROGRAMADA";
        updatedAt = LocalDateTime.now();
        addHistorial(estado, "Solicitud reprogramada");
    }

    public void gestionar(String nuevoEstado, LocalDateTime fecha, String resultado, String observacion) {
        if (estado.equals("COMPLETADA") || estado.equals("CANCELADA")) throw new IllegalStateException("Solicitud finalizada");
        switch (nuevoEstado) {
            case "REPROGRAMADA" -> reprogramar(fecha);
            case "COMPLETADA" -> {
                if (!estado.equals("CONFIRMADA")) throw new IllegalStateException("Confirma primero la solicitud");
                if (resultado == null || resultado.isBlank()) throw new IllegalStateException("Resultado obligatorio");
                estado = nuevoEstado;
            }
            case "CANCELADA" -> cancelar();
            case "CONFIRMADA" -> {
                if (!estado.equals("CONFIRMADA") && !estado.equals("REPROGRAMADA")) throw new IllegalStateException("Estado inválido");
                estado = nuevoEstado;
            }
            default -> throw new IllegalStateException("Estado inválido");
        }
        this.resultado = resultado;
        this.observacion = observacion;
        updatedAt = LocalDateTime.now();
        addHistorial(estado, "Gestión de orientación");
    }

    public String getResultado() { return resultado; }
    public String getObservacion() { return observacion; }
    public java.util.Set<Long> getOfertasComparadas() { return ofertasComparadas; }

    public void addHistorial(String estado, String detalle) {
        historial.add(new HistorialSolicitud(estado, detalle, this));
    }

    public Long getId() { return id; }
    public String getEstudianteEmail() { return estudianteEmail; }
    public Long getOfertaId() { return ofertaId; }
    public String getMotivo() { return motivo; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public String getEstado() { return estado; }
    public String getOrientadorEmail() { return orientadorEmail; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public List<HistorialSolicitud> getHistorial() { return historial; }
}
