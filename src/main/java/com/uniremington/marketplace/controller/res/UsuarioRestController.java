package com.uniremington.marketplace.controller.res;

import com.uniremington.marketplace.model.dao.UsuarioDAO;
import com.uniremington.marketplace.model.vo.RolUsuario;
import com.uniremington.marketplace.model.vo.UsuarioVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioRestController {

    private final UsuarioDAO usuarioDAO;
    private final PasswordEncoder passwordEncoder;

    public UsuarioRestController(UsuarioDAO usuarioDAO, PasswordEncoder passwordEncoder) {
        this.usuarioDAO = usuarioDAO;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public List<UsuarioResponse> listar() {
        return usuarioDAO.listar().stream().map(UsuarioResponse::desde).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<UsuarioResponse> buscarPorId(@PathVariable Long id) {
        return usuarioDAO.buscarPorId(id)
                .map(UsuarioResponse::desde)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/correo")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<UsuarioResponse> buscarPorCorreo(@RequestParam String valor) {
        return usuarioDAO.buscarPorCorreo(valor)
                .map(UsuarioResponse::desde)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<UsuarioResponse> crear(@RequestBody UsuarioRequest request) {
        validar(request);
        Long id = usuarioDAO.crear(request.aUsuario(
                null,
                RolUsuario.VENDEDOR,
                true,
                passwordEncoder.encode(request.password())
        ));
        UsuarioResponse response = usuarioDAO.buscarPorId(id)
                .map(UsuarioResponse::desde)
                .orElseThrow(() -> new IllegalStateException("No se encontró el usuario recién creado"));
        return ResponseEntity.created(URI.create("/api/usuarios/" + id)).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@marketplaceAuthorization.canManageUser(#p0, authentication)")
    public ResponseEntity<Void> actualizar(@PathVariable Long id, @RequestBody UsuarioRequest request) {
        validar(request);
        var usuarioExistente = usuarioDAO.buscarPorId(id);
        if (usuarioExistente.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        UsuarioVO usuario = request.aUsuario(
                id,
                usuarioExistente.get().rol(),
                usuarioExistente.get().activo(),
                passwordEncoder.encode(request.password())
        );
        return usuarioDAO.actualizar(usuario)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    @PatchMapping("/{id}/activo")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> actualizarEstado(
            @PathVariable Long id,
            @RequestParam boolean activo
    ) {
        return usuarioDAO.actualizarEstado(id, activo)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        return usuarioDAO.eliminar(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    private void validar(UsuarioRequest request) {
        if (request == null || request.nombre() == null || request.nombre().isBlank()
                || request.correo() == null || request.correo().isBlank()
                || !request.correo().contains("@")
                || request.password() == null || request.password().length() < 8) {
            throw new ResponseStatusException(
                    BAD_REQUEST,
                    "Nombre, correo válido y contraseña de al menos 8 caracteres son obligatorios"
            );
        }
    }

    public record UsuarioRequest(
            String nombre,
            String correo,
            String password
    ) {
        UsuarioVO aUsuario(Long id, RolUsuario rol, boolean activoAnterior, String passwordHash) {
            return new UsuarioVO(
                    id,
                    nombre.trim(),
                    correo.trim(),
                    passwordHash,
                    rol,
                    activoAnterior,
                    null
            );
        }
    }

    public record UsuarioResponse(
            Long id,
            String nombre,
            String correo,
            RolUsuario rol,
            boolean activo,
            java.time.LocalDateTime creadoEn
    ) {
        static UsuarioResponse desde(UsuarioVO usuario) {
            return new UsuarioResponse(
                    usuario.id(),
                    usuario.nombre(),
                    usuario.correo(),
                    usuario.rol(),
                    usuario.activo(),
                    usuario.creadoEn()
            );
        }
    }
}
