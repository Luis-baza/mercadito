package com.uniremington.marketplace.controller.res;

import com.uniremington.marketplace.model.dao.ProductoDAO;
import com.uniremington.marketplace.security.MarketplaceAuthorizationService;
import com.uniremington.marketplace.model.vo.DisponibilidadProducto;
import com.uniremington.marketplace.model.vo.EstadoProducto;
import com.uniremington.marketplace.model.vo.ProductoVO;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/productos")
public class ProductoRestController {

    private final ProductoDAO productoDAO;
    private final MarketplaceAuthorizationService authorizationService;

    public ProductoRestController(
            ProductoDAO productoDAO,
            MarketplaceAuthorizationService authorizationService
    ) {
        this.productoDAO = productoDAO;
        this.authorizationService = authorizationService;
    }

    @GetMapping
    public List<ProductoVO> catalogo(
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) Long tiendaId,
            @RequestParam(required = false) BigDecimal precioMinimo,
            @RequestParam(required = false) BigDecimal precioMaximo,
            @RequestParam(defaultValue = "recientes") String ordenar
    ) {
        validarRangoPrecios(precioMinimo, precioMaximo);
        return productoDAO.listarCatalogo(
                texto,
                categoriaId,
                tiendaId,
                precioMinimo,
                precioMaximo,
                "precio".equalsIgnoreCase(ordenar)
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("@marketplaceAuthorization.canViewProduct(#p0, authentication)")
    public ResponseEntity<ProductoVO> buscarPorId(@PathVariable Long id) {
        return productoDAO.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/tienda/{tiendaId}")
    @PreAuthorize("@marketplaceAuthorization.canViewStore(#p0, authentication)")
    public List<ProductoVO> listarPorTienda(
            @PathVariable Long tiendaId,
            Authentication authentication
    ) {
        if (authorizationService.canManageStore(tiendaId, authentication)) {
            return productoDAO.listarPorTienda(tiendaId);
        }
        return productoDAO.listarCatalogo(null, null, tiendaId, null, null, false);
    }

    @GetMapping("/pendientes")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public List<ProductoVO> listarPendientesModeracion() {
        return productoDAO.listarPendientesModeracion();
    }

    @PostMapping
    @PreAuthorize("@marketplaceAuthorization.canManageProductsInStore(#p0.tiendaId(), authentication)")
    public ResponseEntity<ProductoVO> crear(@RequestBody ProductoVO producto) {
        Long id = productoDAO.crear(new ProductoVO(
                null,
                producto.tiendaId(),
                producto.categoriaId(),
                producto.nombre(),
                producto.descripcion(),
                producto.precio(),
                producto.disponibilidad(),
                EstadoProducto.PENDIENTE,
                null,
                null
        ));
        ProductoVO creado = productoDAO.buscarPorId(id).orElseThrow();
        return ResponseEntity.created(URI.create("/api/productos/" + id)).body(creado);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@marketplaceAuthorization.canManageProduct(#p0, authentication)")
    public ResponseEntity<Void> actualizar(
            @PathVariable Long id,
            @RequestBody ProductoVO producto,
            Authentication authentication
    ) {
        ProductoVO existente = productoDAO.buscarPorId(id).orElse(null);
        if (existente == null) {
            return ResponseEntity.notFound().build();
        }
        return productoDAO.actualizar(new ProductoVO(
                id,
                existente.tiendaId(),
                producto.categoriaId(),
                producto.nombre(),
                producto.descripcion(),
                producto.precio(),
                producto.disponibilidad(),
                authorizationService.isAdmin(authentication)
                        ? existente.estado()
                        : EstadoProducto.PENDIENTE,
                existente.creadoEn(),
                existente.actualizadoEn()
        )) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @PatchMapping("/{id}/disponibilidad")
    @PreAuthorize("@marketplaceAuthorization.canManageProduct(#p0, authentication)")
    public ResponseEntity<Void> actualizarDisponibilidad(
            @PathVariable Long id,
            @RequestParam DisponibilidadProducto disponibilidad
    ) {
        return productoDAO.actualizarDisponibilidad(id, disponibilidad)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> actualizarEstado(
            @PathVariable Long id,
            @RequestParam EstadoProducto estado
    ) {
        return productoDAO.actualizarEstado(id, estado)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@marketplaceAuthorization.canManageProduct(#p0, authentication)")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        return productoDAO.eliminar(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    private void validarRangoPrecios(BigDecimal minimo, BigDecimal maximo) {
        if (minimo != null && minimo.signum() < 0
                || maximo != null && maximo.signum() < 0
                || minimo != null && maximo != null && minimo.compareTo(maximo) > 0) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "El rango de precios no es válido"
            );
        }

    }
}
