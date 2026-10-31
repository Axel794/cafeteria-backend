package pe.edu.upn.cafeteria.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Table(name = "cupones_descuento")
@Data
public class CuponDescuento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String codigo; // Ej. "VERANO2026"

    private Double porcentajeDescuento;
    private Double montoFijo;
    private LocalDate fechaExpiracion;
    private Boolean usado = false;

    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario cliente; // Opcional, si el cupón es exclusivo
}