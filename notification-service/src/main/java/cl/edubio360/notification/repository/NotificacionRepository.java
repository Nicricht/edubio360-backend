package cl.edubio360.notification.repository;

import cl.edubio360.notification.model.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {
    java.util.List<Notificacion> findByDestinatarioIgnoreCaseOrderByCreatedAtDesc(String destinatario);
}
