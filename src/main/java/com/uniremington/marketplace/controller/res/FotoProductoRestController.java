package com.uniremington.marketplace.controller.res;

import com.uniremington.marketplace.model.dao.FotoProductoDAO;
import com.uniremington.marketplace.model.vo.FotoProductoVO;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/productos/{productoId}/fotos")
public class FotoProductoRestController {

    private final FotoProductoDAO fotoProductoDAO;

    public FotoProductoRestController(FotoProductoDAO fotoProductoDAO) {
        this.fotoProductoDAO = fotoProductoDAO;
    }

    @GetMapping
    @PreAuthorize("@marketplaceAuthorization.canViewProduct(#p0, authentication)")
    public List<FotoProductoVO> listar(@PathVariable Long productoId) {
        return fotoProductoDAO.listarPorProducto(productoId);
    }

    @PostMapping
    @PreAuthorize("@marketplaceAuthorization.canManageProduct(#p0, authentication)")
    public ResponseEntity<FotoProductoVO> crear(
            @PathVariable Long productoId,
            @RequestBody FotoProductoVO foto
    ) {
        Long id = fotoProductoDAO.crear(new FotoProductoVO(
                null,
                productoId,
                foto.url(),
                false
        ));
        if (foto.principal()) {
            fotoProductoDAO.marcarComoPrincipal(productoId, id);
        }
        FotoProductoVO creada = fotoProductoDAO.listarPorProducto(productoId).stream()
                .filter(item -> item.id().equals(id))
                .findFirst()
                .orElseThrow();
        return ResponseEntity.created(
                URI.create("/api/productos/" + productoId + "/fotos/" + id)
        ).body(creada);
    }

    @PutMapping("/{fotoId}/principal")
    @PreAuthorize("@marketplaceAuthorization.canManageProduct(#p0, authentication)")
    public ResponseEntity<Void> marcarPrincipal(
            @PathVariable Long productoId,
            @PathVariable Long fotoId
    ) {
        boolean perteneceAlProducto = fotoProductoDAO.listarPorProducto(productoId).stream()
                .anyMatch(foto -> foto.id().equals(fotoId));
        if (!perteneceAlProducto) {
            return ResponseEntity.notFound().build();
        }
        fotoProductoDAO.marcarComoPrincipal(productoId, fotoId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{fotoId}")
    @PreAuthorize("@marketplaceAuthorization.canManageProduct(#p0, authentication)")
    public ResponseEntity<Void> actualizarUrl(
            @PathVariable Long productoId,
            @PathVariable Long fotoId,
            @RequestParam String url
    ) {
        boolean perteneceAlProducto = fotoProductoDAO.listarPorProducto(productoId).stream()
                .anyMatch(foto -> foto.id().equals(fotoId));
        if (!perteneceAlProducto) {
            return ResponseEntity.notFound().build();
        }
        return fotoProductoDAO.actualizarUrl(fotoId, url)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{fotoId}")
    @PreAuthorize("@marketplaceAuthorization.canManageProduct(#p0, authentication)")
    public ResponseEntity<Void> eliminar(
            @PathVariable Long productoId,
            @PathVariable Long fotoId
    ) {
        boolean perteneceAlProducto = fotoProductoDAO.listarPorProducto(productoId).stream()
                .anyMatch(foto -> foto.id().equals(fotoId));
        if (!perteneceAlProducto) {
            return ResponseEntity.notFound().build();
        }
        return fotoProductoDAO.eliminar(fotoId)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}
