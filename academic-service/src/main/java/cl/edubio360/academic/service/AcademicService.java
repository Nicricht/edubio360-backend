package cl.edubio360.academic.service;

import cl.edubio360.academic.dto.OfertaRequest;
import cl.edubio360.academic.dto.OfertaResponse;
import cl.edubio360.academic.dto.SedeResponse;
import cl.edubio360.academic.model.OfertaAcademica;
import cl.edubio360.academic.model.Sede;
import cl.edubio360.academic.repository.OfertaRepository;
import cl.edubio360.academic.repository.SedeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class AcademicService {
    private final OfertaRepository ofertas;
    private final SedeRepository sedes;

    public AcademicService(OfertaRepository ofertas, SedeRepository sedes) {
        this.ofertas = ofertas;
        this.sedes = sedes;
    }

    public List<OfertaResponse> listar(String q) {
        List<OfertaAcademica> resultado = q == null || q.isBlank()
                ? ofertas.findAll()
                : ofertas.findByCarreraContainingIgnoreCaseOrSedeInstitucionContainingIgnoreCase(q.trim(), q.trim());
        return resultado.stream().map(this::toResponse).toList();
    }

    public OfertaResponse obtener(Long id) {
        return toResponse(get(id));
    }

    @Transactional
    public OfertaResponse crear(OfertaRequest request) {
        Sede sede = getSede(request.sedeId());
        OfertaAcademica oferta = new OfertaAcademica(request.carrera(), request.modalidad(), request.jornada(),
                request.arancel(), request.matricula(), sede);
        return toResponse(ofertas.save(oferta));
    }

    @Transactional
    public OfertaResponse actualizar(Long id, OfertaRequest request) {
        OfertaAcademica oferta = get(id);
        Sede sede = getSede(request.sedeId());
        oferta.actualizar(request.carrera(), request.modalidad(), request.jornada(),
                request.arancel(), request.matricula(), sede);
        return toResponse(oferta);
    }

    @Transactional
    public void eliminar(Long id) {
        OfertaAcademica oferta = get(id);
        oferta.getSede().getOfertas().removeIf(asociada -> asociada == oferta || id.equals(asociada.getId()));
        ofertas.delete(oferta);
    }

    public List<SedeResponse> listarSedes() {
        return sedes.findAll().stream()
                .map(s -> new SedeResponse(s.getId(), s.getNombre(), s.getInstitucion(), s.getComuna()))
                .toList();
    }

    private OfertaAcademica get(Long id) {
        return ofertas.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Oferta académica no encontrada"));
    }

    private Sede getSede(Long id) {
        return sedes.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sede no encontrada"));
    }

    private OfertaResponse toResponse(OfertaAcademica o) {
        Sede s = o.getSede();
        return new OfertaResponse(o.getId(), o.getCarrera(), s.getInstitucion(), s.getId(), s.getNombre(),
                s.getComuna(), o.getModalidad(), o.getJornada(), o.getArancel(), o.getMatricula());
    }
}
