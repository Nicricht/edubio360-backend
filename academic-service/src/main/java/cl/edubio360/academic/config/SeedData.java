package cl.edubio360.academic.config;

import cl.edubio360.academic.model.OfertaAcademica;
import cl.edubio360.academic.model.Sede;
import cl.edubio360.academic.repository.OfertaRepository;
import cl.edubio360.academic.repository.SedeRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
public class SeedData {
    @Bean
    ApplicationRunner seedAcademicData(SedeRepository sedes, OfertaRepository ofertas) {
        return args -> {
            if (ofertas.count() > 0) return;
            guardar(sedes, ofertas, "Ingeniería en Informática", "Ingeniería en Informática", "IP DUOC UC", "Concepción", "Tecnología", "Diurno", 8, 216000, 2190000, "oferta-01-informatica-duoc.jpg", 8);
            guardar(sedes, ofertas, "Ingeniería en Informática", "Ingeniería en Informática", "IP Santo Tomás", "Concepción", "Tecnología", "Diurno", 8, 89000, 1827000, "oferta-02-informatica-santo-tomas.jpg", 24);
            guardar(sedes, ofertas, "Ingeniería en Informática", "Ingeniería en Informática", "IP INACAP", "Talcahuano", "Tecnología", "Diurno", 8, 220000, 2066000, "oferta-03-informatica-inacap.jpg", 36);
            guardar(sedes, ofertas, "Enfermería", "Enfermería", "Universidad de Concepción", "Concepción", "Salud", "Diurno", 10, 120000, 4243207, "oferta-04-enfermeria-udec.jpg", 12);
            guardar(sedes, ofertas, "Enfermería", "Enfermería", "Universidad San Sebastián", "Concepción", "Salud", "Diurno", 10, 185300, 4643800, "oferta-05-enfermeria-uss.jpg", 30);
            guardar(sedes, ofertas, "Enfermería", "Enfermería", "Universidad Santo Tomás", "Los Ángeles", "Salud", "Diurno", 10, 134000, 3662000, "oferta-06-enfermeria-ust.jpg", 8);
            guardar(sedes, ofertas, "Psicología", "Psicología", "Universidad de Concepción", "Concepción", "Ciencias Sociales", "Diurno", 11, 120000, 4155549, "oferta-07-psicologia-udec.jpg", 24);
            guardar(sedes, ofertas, "Psicología", "Psicología", "Universidad San Sebastián", "Concepción", "Ciencias Sociales", "Diurno", 10, 185300, 4799100, "oferta-08-psicologia-uss.jpg", 36);
            guardar(sedes, ofertas, "Psicología", "Psicología", "Universidad Santo Tomás", "Concepción", "Ciencias Sociales", "Diurno", 10, 134000, 3987000, "oferta-09-psicologia-ust.jpg", 12);
            guardar(sedes, ofertas, "Ingeniería Comercial", "Ingeniería Comercial", "Universidad del Bío-Bío", "Concepción", "Administración y Comercio", "Diurno", 10, 105000, 3255000, "oferta-10-comercial-ubb.jpg", 30);
            guardar(sedes, ofertas, "Ingeniería Comercial", "Ingeniería Comercial", "Universidad de Concepción", "Concepción", "Administración y Comercio", "Diurno", 10, 120000, 4584271, "oferta-11-comercial-udec.jpg", 8);
            guardar(sedes, ofertas, "Ingeniería Comercial", "Ingeniería Comercial", "Universidad Católica de la Santísima Concepción", "Concepción", "Administración y Comercio", "Diurno", 10, 90000, 3325000, "oferta-12-comercial-ucsc.jpg", 24);
            guardar(sedes, ofertas, "Trabajo Social", "Trabajo Social", "Universidad Católica de la Santísima Concepción", "Concepción", "Ciencias Sociales", "Diurno", 9, 90000, 2488000, "oferta-13-trabajo-social-ucsc.jpg", 36);
            guardar(sedes, ofertas, "Trabajo Social", "Trabajo Social", "Universidad de Concepción", "Concepción", "Ciencias Sociales", "Diurno", 10, 120000, 3530746, "oferta-14-trabajo-social-udec.jpg", 12);
            guardar(sedes, ofertas, "Trabajo Social", "Trabajo Social", "Universidad Santo Tomás", "Los Ángeles", "Ciencias Sociales", "Diurno", 9, 134000, 2245000, "oferta-15-trabajo-social-ust.jpg", 30);
            guardar(sedes, ofertas, "Analista Programador", "Programación", "CFT INACAP", "Talcahuano", "Tecnología", "Diurno", 4, 220000, 2066000, "oferta-16-programacion-inacap-talcahuano.jpg", 8);
            guardar(sedes, ofertas, "Analista Programador", "Programación", "CFT INACAP", "Los Ángeles", "Tecnología", "Vespertino", 4, 220000, 1988000, "oferta-17-programacion-inacap-los-angeles.jpg", 24);
            guardar(sedes, ofertas, "Técnico Analista Programador", "Programación", "IP Dr. Virginio Gómez G.", "Concepción", "Tecnología", "Vespertino", 4, 144000, 1558000, "oferta-18-programacion-virginio-gomez.jpg", 36);
            guardar(sedes, ofertas, "Técnico en Automatización y Control Industrial", "Automatización", "IP AIEP", "Concepción", "Tecnología", "Vespertino", 5, 190000, 1840000, "oferta-19-automatizacion-aiep.jpg", 12);
            guardar(sedes, ofertas, "Técnico en Automatización y Robótica", "Automatización", "CFT INACAP", "San Pedro de la Paz", "Tecnología", "Diurno", 4, 220000, 2214000, "oferta-20-robotica-inacap.jpg", 30);
            guardar(sedes, ofertas, "Técnico Universitario en Automatización y Control", "Automatización", "Universidad Técnica Federico Santa María", "Hualpén", "Tecnología", "Diurno", 5, 186000, 2150000, "oferta-21-automatizacion-usm.jpg", 8);
        };
    }
    private void guardar(SedeRepository sedes, OfertaRepository ofertas, String carrera, String grupo,
                         String institucion, String comuna, String area, String jornada, int duracion,
                         int matricula, int arancel, String imagen, int cupos) {
        Sede sede = sedes.findAll().stream()
                .filter(s -> s.getInstitucion().equals(institucion) && s.getComuna().equals(comuna))
                .findFirst().orElseGet(() -> sedes.save(new Sede("Sede " + comuna, institucion, comuna)));
        OfertaAcademica oferta = new OfertaAcademica(carrera, "Presencial", jornada,
                BigDecimal.valueOf(arancel), BigDecimal.valueOf(matricula), sede);
        String nivel = carrera.startsWith("Técnico") || carrera.startsWith("Analista") ? "Técnico" : "Profesional";
        oferta.actualizarDetalles(area, duracion, nivel,
                "Oferta de educación superior de la Región del Biobío. Consulta valores y requisitos con la institución.",
                grupo, imagen, cupos, 10);
        ofertas.save(oferta);
    }
}
