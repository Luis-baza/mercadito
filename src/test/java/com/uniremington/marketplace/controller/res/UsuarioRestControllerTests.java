package com.uniremington.marketplace.controller.res;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import com.uniremington.marketplace.model.dao.UsuarioDAO;
import com.uniremington.marketplace.model.vo.RolUsuario;
import com.uniremington.marketplace.model.vo.UsuarioVO;

class UsuarioRestControllerTests {

    private final UsuarioDAO usuarioDAO = mock(UsuarioDAO.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private UsuarioRestController controller;

    @BeforeEach
    void setUp() {
        controller = new UsuarioRestController(usuarioDAO, passwordEncoder);
    }

    @Test
    void crearNormalizaDatosCodificaPasswordYRespondeConRecursoCreado() {
        when(passwordEncoder.encode("password123")).thenReturn("hash");
        when(usuarioDAO.crear(any(UsuarioVO.class))).thenReturn(6L);
        when(usuarioDAO.buscarPorId(6L)).thenReturn(Optional.of(new UsuarioVO(
                6L, "Ana", "ana@test.com", "hash", RolUsuario.VENDEDOR, true, null)));

        var response = controller.crear(new UsuarioRestController.UsuarioRequest(
                " Ana ", " ana@test.com ", "password123"));

        assertEquals(201, response.getStatusCode().value());
        assertEquals("/api/usuarios/6", response.getHeaders().getLocation().toString());
        assertEquals("Ana", response.getBody().nombre());
        assertEquals("ana@test.com", response.getBody().correo());
        verify(usuarioDAO).crear(org.mockito.ArgumentMatchers.argThat(user ->
                user.password().equals("hash") && user.rol() == RolUsuario.VENDEDOR && user.activo()));
    }

    @Test
    void rechazaDatosInvalidosAntesDeLlamarAlDao() {
        assertThrows(ResponseStatusException.class, () -> controller.crear(
                new UsuarioRestController.UsuarioRequest("Ana", "correo-invalido", "12345678")));
        assertThrows(ResponseStatusException.class, () -> controller.crear(
                new UsuarioRestController.UsuarioRequest("Ana", "ana@test.com", "123")));
        org.mockito.Mockito.verifyNoInteractions(usuarioDAO, passwordEncoder);
    }

    @Test
    void actualizarConservaRolYEstadoDelUsuarioExistente() {
        when(passwordEncoder.encode("password123")).thenReturn("nuevo-hash");
        when(usuarioDAO.buscarPorId(2L)).thenReturn(Optional.of(new UsuarioVO(
                2L, "Nombre", "correo@test.com", "anterior", RolUsuario.ADMINISTRADOR, false, null)));
        when(usuarioDAO.actualizar(any(UsuarioVO.class))).thenReturn(true);

        var response = controller.actualizar(2L, new UsuarioRestController.UsuarioRequest(
                "Nuevo", "nuevo@test.com", "password123"));

        assertEquals(204, response.getStatusCode().value());
        verify(usuarioDAO).actualizar(org.mockito.ArgumentMatchers.argThat(user ->
                user.rol() == RolUsuario.ADMINISTRADOR && !user.activo()
                        && user.password().equals("nuevo-hash")));
    }

    @Test
    void buscarPorCorreoDevuelveNotFoundSiNoHayCoincidencia() {
        when(usuarioDAO.buscarPorCorreo("nadie@test.com")).thenReturn(Optional.empty());

        assertEquals(404, controller.buscarPorCorreo("nadie@test.com").getStatusCode().value());
    }
}
