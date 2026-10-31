package pe.edu.upn.cafeteria.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upn.cafeteria.entity.Categoria;
import pe.edu.upn.cafeteria.entity.Configuracion;
import pe.edu.upn.cafeteria.entity.Producto;
import pe.edu.upn.cafeteria.repository.CategoriaRepository;
import pe.edu.upn.cafeteria.repository.ConfiguracionRepository;
import pe.edu.upn.cafeteria.repository.ProductoRepository;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * Carga datos de PRUEBA la primera vez que se levanta la aplicacion con la base vacia.
 * Los precios y el horario son ejemplos para poder probar la API; no son datos reales del local.
 */
@Component
@Profile("!test")
public class DataLoader implements CommandLineRunner {

    private final CategoriaRepository categoriaRepository;
    private final ProductoRepository productoRepository;
    private final ConfiguracionRepository configuracionRepository;

    public DataLoader(CategoriaRepository categoriaRepository,
                      ProductoRepository productoRepository,
                      ConfiguracionRepository configuracionRepository) {
        this.categoriaRepository = categoriaRepository;
        this.productoRepository = productoRepository;
        this.configuracionRepository = configuracionRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (categoriaRepository.count() > 0) {
            return;
        }

        Categoria cafes = categoriaRepository.save(new Categoria("Cafes"));
        Categoria frias = categoriaRepository.save(new Categoria("Bebidas Frias"));
        Categoria reposteria = categoriaRepository.save(new Categoria("Reposteria"));
        Categoria metodos = categoriaRepository.save(new Categoria("Metodos de Extraccion"));

        productoRepository.save(producto("Espresso", "Shot de cafe de origen", "8.00", cafes, null, true));
        productoRepository.save(producto("Cappuccino", "Espresso con leche vaporizada y espuma", "12.00", cafes, "Lactosa", true));
        productoRepository.save(producto("Latte", "Espresso con abundante leche", "13.00", cafes, "Lactosa", true));
        productoRepository.save(producto("Cold Brew", "Cafe infusionado en frio durante 12 horas", "14.00", frias, null, true));
        productoRepository.save(producto("Frappe de Caramelo", "Bebida helada con caramelo", "16.00", frias, "Lactosa", false));
        productoRepository.save(producto("Brownie Artesanal", "Brownie de chocolate con nueces", "9.50", reposteria, "Gluten, frutos secos", true));
        productoRepository.save(producto("Cheesecake de Maracuya", "Porcion individual", "12.50", reposteria, "Gluten, lactosa", true));
        productoRepository.save(producto("V60", "Cafe filtrado de origen, preparado al momento", "15.00", metodos, null, true));

        Configuracion configuracion = new Configuracion();
        configuracion.setHoraApertura(LocalTime.of(7, 0));
        configuracion.setHoraCierre(LocalTime.of(21, 0));
        configuracion.setEstadoLocal("ABIERTO");
        configuracionRepository.save(configuracion);
    }

    private Producto producto(String nombre, String descripcion, String precio,
                              Categoria categoria, String alergenos, boolean disponible) {
        Producto producto = new Producto();
        producto.setNombre(nombre);
        producto.setDescripcion(descripcion);
        producto.setPrecio(new BigDecimal(precio));
        producto.setCategoria(categoria);
        producto.setAlergenos(alergenos);
        producto.setDisponible(disponible);
        return producto;
    }
}
