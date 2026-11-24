package cl.edubio360.notification.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "notificaciones")
public class Notificacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 60)
    private String tipo;

    @Column(nullable = false, length = 180)
    private String destinatario;

    @Column(nullable = false, length = 1000)
    private String mensaje;

    @Column(nullable = false, length = 30)
    private String estado;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "notificacion", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<Envio> envios = new ArrayList<>();

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean leida;

    protected Notificacion() {}

    public Notificacion(String tipo, String destinatario, String mensaje) {
        this.tipo = tipo;
        this.destinatario = destinatario;
        this.mensaje = mensaje;
        this.estado = "PENDIENTE";
        this.createdAt = LocalDateTime.now();
    }

    public void actualizar(String tipo, String destinatario, String mensaje) {
        this.tipo = tipo;
        this.destinatario = destinatario;
        this.mensaje = mensaje;
    }

    public void marcarProcesada(String canal) {
        this.estado = "PROCESADA";
        addEnvio(canal, "OK");
    }

    public void addEnvio(String canal, String estado) {
        envios.add(new Envio(canal, estado, this));
    }

    public boolean isLeida() { return leida; }
    public void marcarLeida() { leida = true; }

    public Long getId() { return id; }
    public String getTipo() { return tipo; }
    public String getDestinatario() { return destinatario; }
    public String getMensaje() { return mensaje; }
    public String getEstado() { return estado; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<Envio> getEnvios() { return envios; }
}
