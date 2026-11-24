package cl.edubio360.academic.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "ofertas_academicas")
public class OfertaAcademica {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 180)
    private String carrera;

    @Column(nullable = false, length = 60)
    private String modalidad;

    @Column(nullable = false, length = 60)
    private String jornada;

    @Column(nullable = false, precision = 12, scale = 0)
    private BigDecimal arancel;

    @Column(nullable = false, precision = 12, scale = 0)
    private BigDecimal matricula;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "sede_id", nullable = false)
    private Sede sede;

    @Column(length = 180)
    private String area;

    @Column
    private Integer duracion;

    @Column(length = 180)
    private String nivel;

    @Column(length = 2000)
    private String descripcion;

    @Column(length = 180)
    private String grupoComparacion;

    @Column(length = 300)
    private String imagen;

    @Column
    private Integer cupos;

    @Column
    private Integer cupoCritico;

    protected OfertaAcademica() {}

    public OfertaAcademica(String carrera, String modalidad, String jornada,
                           BigDecimal arancel, BigDecimal matricula, Sede sede) {
        this.carrera = carrera;
        this.modalidad = modalidad;
        this.jornada = jornada;
        this.arancel = arancel;
        this.matricula = matricula;
        this.sede = sede;
    }

    public void actualizar(String carrera, String modalidad, String jornada,
                           BigDecimal arancel, BigDecimal matricula, Sede sede) {
        this.carrera = carrera;
        this.modalidad = modalidad;
        this.jornada = jornada;
        this.arancel = arancel;
        this.matricula = matricula;
        this.sede = sede;
    }

    public void actualizarDetalles(String area, Integer duracion, String nivel, String descripcion, String grupoComparacion, String imagen, Integer cupos, Integer cupoCritico) {
        if (area != null) this.area = area;
        if (duracion != null) this.duracion = duracion;
        if (nivel != null) this.nivel = nivel;
        if (descripcion != null) this.descripcion = descripcion;
        if (grupoComparacion != null) this.grupoComparacion = grupoComparacion;
        if (imagen != null) this.imagen = imagen;
        if (cupos != null) this.cupos = cupos;
        if (cupoCritico != null) this.cupoCritico = cupoCritico;
    }

    public String getArea() { return area; }
    public Integer getDuracion() { return duracion; }
    public String getNivel() { return nivel; }
    public String getDescripcion() { return descripcion; }
    public String getGrupoComparacion() { return grupoComparacion; }
    public String getImagen() { return imagen; }
    public Integer getCupos() { return cupos; }
    public Integer getCupoCritico() { return cupoCritico; }

    public Long getId() { return id; }
    public String getCarrera() { return carrera; }
    public String getModalidad() { return modalidad; }
    public String getJornada() { return jornada; }
    public BigDecimal getArancel() { return arancel; }
    public BigDecimal getMatricula() { return matricula; }
    public Sede getSede() { return sede; }
    public void setSede(Sede sede) { this.sede = sede; }
}
