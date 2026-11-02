package com.uniremington.marketplace.controller.res;

import com.uniremington.marketplace.model.dao.TiendaDAO;
import com.uniremington.marketplace.model.vo.EstadoTienda;
import com.uniremington.marketplace.model.vo.TiendaVO;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/tiendas")
public class TiendaRestController {

    private final TiendaDAO tiendaDAO;

    public TiendaRestController(TiendaDAO tiendaDAO) {
        this.tiendaDAO = tiendaDAO;
    }

    @GetMapping
    public List<TiendaVO> listar() {
        return tiendaDAO.listarAprobadas();
    }

    @GetMapping("/estado/{estado}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public List<TiendaVO> listarPorEstado(@PathVariable EstadoTienda estado) {
        return tiendaDAO.listarPorEstado(estado);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@marketplaceAuthorization.canViewStore(#p0, authentication)")
    public ResponseEntity<TiendaVO> buscarPorId(@PathVariable Long id) {
        return tiendaDAO.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/vendedor/{usuarioId}")
    @PreAuthorize("@marketplaceAuthorization.canManageUser(#p0, authentication)")
    public ResponseEntity<TiendaVO> buscarPorVendedor(@PathVariable Long usuarioId) {
        return tiendaDAO.buscarPorUsuarioId(usuarioId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("@marketplaceAuthorization.canCreateStore(#p0.usuarioId(), authentication)")
    public ResponseEntity<TiendaVO> crear(@RequestBody TiendaVO tienda) {
        Long id = tiendaDAO.crear(new TiendaVO(
                null,
                tienda.usuarioId(),
                tienda.nombre(),
                tienda.descripcion(),
                tienda.telefono(),
                tienda.whatsapp(),
                tienda.correoContacto(),
                EstadoTienda.PENDIENTE,
                null
        ));
        TiendaVO creada = tiendaDAO.buscarPorId(id).orElseThrow();
        return ResponseEntity.created(URI.create("/api/tiendas/" + id)).body(creada);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@marketplaceAuthorization.canManageStore(#p0, authentication)")
    public ResponseEntity<Void> actualizar(
            @PathVariable Long id,
            @RequestBody TiendaVO tienda
    ) {
        TiendaVO existente = tiendaDAO.buscarPorId(id).orElse(null);
        if (existente == null) {
            return ResponseEntity.notFound().build();
        }
        return tiendaDAO.actualizar(new TiendaVO(
                id,
                existente.usuarioId(),
                tienda.nombre(),
                tienda.descripcion(),
                tienda.telefono(),
                tienda.whatsapp(),
                tienda.correoContacto(),
                existente.estado(),
                existente.creadaEn()
        )) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> actualizarEstado(
            @PathVariable Long id,
            @RequestParam EstadoTienda estado
    ) {
        return tiendaDAO.actualizarEstado(id, estado)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@marketplaceAuthorization.canManageStore(#p0, authentication)")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        return tiendaDAO.eliminar(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}
