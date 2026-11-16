package com.uniremington.marketplace.controller.mvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import com.uniremington.marketplace.model.dao.UsuarioDAO;
import com.uniremington.marketplace.model.vo.UsuarioVO;

class CuentaMvcControllerTests {

    private final UsuarioDAO usuarioDAO = mock(UsuarioDAO.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private CuentaMvcController controller;

    @BeforeEach
    void setUp() {
        controller = new CuentaMvcController(usuarioDAO, passwordEncoder);
    }

    @Test
    void registrarCreaCuentaConDatosNormalizados() {
        when(usuarioDAO.buscarPorCorreo("ana@test.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("hash");
        var redirect = new RedirectAttributesModelMap();

        String view = controller.registrar(" Ana ", " ana@test.com ", "password123",
                new ExtendedModelMap(), redirect);

        assertEquals("redirect:/login", view);
        assertEquals(true, redirect.getFlashAttributes().get("registroExitoso"));
        verify(usuarioDAO).crear(org.mockito.ArgumentMatchers.argThat(user ->
                user.nombre().equals("Ana") && user.correo().equals("ana@test.com")
                        && user.password().equals("hash")));
    }

    @Test
    void registrarNoCreaUsuariosConDatosInvalidosODuplicados() {
        var invalidModel = new ExtendedModelMap();
        assertEquals("registro", controller.registrar("Ana", "invalido", "corta",
                invalidModel, new RedirectAttributesModelMap()));
        assertEquals(true, invalidModel.containsAttribute("error"));

        when(usuarioDAO.buscarPorCorreo("ana@test.com")).thenReturn(Optional.of(
                new UsuarioVO(1L, "Ana", "ana@test.com", "hash", null, true, null)));
        var duplicateModel = new ExtendedModelMap();
        assertEquals("registro", controller.registrar("Ana", "ana@test.com", "password123",
                duplicateModel, new RedirectAttributesModelMap()));
        assertEquals(true, duplicateModel.containsAttribute("error"));
        org.mockito.Mockito.verify(usuarioDAO, org.mockito.Mockito.never()).crear(any(UsuarioVO.class));
    }

    @Test
    void dashboardRedirigeSegunRol() {
        var admin = new TestingAuthenticationToken("admin", "password",
                List.of(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR")));
        var seller = new TestingAuthenticationToken("seller", "password",
                List.of(new SimpleGrantedAuthority("ROLE_VENDEDOR")));

        assertEquals("redirect:/admin/panel", controller.dashboard(admin));
        assertEquals("redirect:/vendedor/panel", controller.dashboard(seller));
    }
}
