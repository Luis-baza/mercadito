package com.uniremington.marketplace.controller.mvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import com.uniremington.marketplace.model.dao.ProductoDAO;
import com.uniremington.marketplace.model.dao.TiendaDAO;
import com.uniremington.marketplace.model.dao.UsuarioDAO;
import com.uniremington.marketplace.model.vo.EstadoProducto;
import com.uniremington.marketplace.model.vo.EstadoTienda;
import com.uniremington.marketplace.model.vo.RolUsuario;
import com.uniremington.marketplace.model.vo.UsuarioVO;

class AdministradorMvcControllerTests {

    private final UsuarioDAO usuarioDAO = mock(UsuarioDAO.class);
    private final TiendaDAO tiendaDAO = mock(TiendaDAO.class);
    private final ProductoDAO productoDAO = mock(ProductoDAO.class);
    private AdministradorMvcController controller;

    @BeforeEach
    void setUp() {
        controller = new AdministradorMvcController(usuarioDAO, tiendaDAO, productoDAO);
    }

    @Test
    void panelCargaPendientesYResumenDeUsuarios() {
        when(usuarioDAO.listar()).thenReturn(List.of(
                new UsuarioVO(1L, "Ana", "ana@test.com", "hash", RolUsuario.VENDEDOR, true, null)));
        var model = new ExtendedModelMap();

        assertEquals("admin/panel", controller.panel(model));
        assertEquals(1, ((List<?>) model.getAttribute("usuarios")).size());
        verify(tiendaDAO).listarPorEstado(EstadoTienda.PENDIENTE);
        verify(productoDAO).listarPendientesModeracion();
    }

    @Test
    void estadoPendienteSeRechazaYEstadoValidoSeGuarda() {
        assertEquals(400, assertThrows(ResponseStatusException.class,
                () -> controller.actualizarEstadoTienda(1L, EstadoTienda.PENDIENTE,
                        new RedirectAttributesModelMap())).getStatusCode().value());
        when(tiendaDAO.actualizarEstado(1L, EstadoTienda.APROBADA)).thenReturn(true);

        assertEquals("redirect:/admin/panel", controller.actualizarEstadoTienda(
                1L, EstadoTienda.APROBADA, new RedirectAttributesModelMap()));
        verify(tiendaDAO).actualizarEstado(1L, EstadoTienda.APROBADA);
    }

    @Test
    void noPermiteDesactivarAdministradorNiUsuarioInexistente() {
        when(usuarioDAO.buscarPorId(1L)).thenReturn(Optional.of(new UsuarioVO(
                1L, "Admin", "admin@test.com", "hash", RolUsuario.ADMINISTRADOR, true, null)));
        when(usuarioDAO.buscarPorId(2L)).thenReturn(Optional.empty());

        assertEquals(400, assertThrows(ResponseStatusException.class,
                () -> controller.actualizarEstadoUsuario(1L, false, new RedirectAttributesModelMap()))
                .getStatusCode().value());
        assertEquals(404, assertThrows(ResponseStatusException.class,
                () -> controller.actualizarEstadoUsuario(2L, true, new RedirectAttributesModelMap()))
                .getStatusCode().value());
    }

    @Test
    void permiteCambiarEstadoDeVendedor() {
        when(usuarioDAO.buscarPorId(3L)).thenReturn(Optional.of(new UsuarioVO(
                3L, "Vendedor", "v@test.com", "hash", RolUsuario.VENDEDOR, true, null)));

        assertEquals("redirect:/admin/panel", controller.actualizarEstadoUsuario(
                3L, false, new RedirectAttributesModelMap()));
        verify(usuarioDAO).actualizarEstado(3L, false);
    }

    @Test
    void noPermitePublicarProductoEnEstadoPendiente() {
        assertEquals(400, assertThrows(ResponseStatusException.class,
                () -> controller.actualizarEstadoProducto(8L, EstadoProducto.PENDIENTE,
                        new RedirectAttributesModelMap())).getStatusCode().value());
    }
}
