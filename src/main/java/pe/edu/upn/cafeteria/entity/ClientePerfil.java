package pe.edu.upn.cafeteria.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Table(name = "cliente_perfiles")
@Data
public class ClientePerfil {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    private Integer puntosAcumulados = 0;
    private LocalDate fechaNacimiento;
}