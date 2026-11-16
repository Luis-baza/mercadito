package com.uniremington.marketplace.controller.mvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import com.uniremington.marketplace.model.dao.CategoriaDAO;
import com.uniremington.marketplace.model.dao.FotoProductoDAO;
import com.uniremington.marketplace.model.dao.ProductoDAO;
import com.uniremington.marketplace.model.dao.TiendaDAO;
import com.uniremington.marketplace.model.dao.UsuarioDAO;
import com.uniremington.marketplace.model.vo.DisponibilidadProducto;
import com.uniremington.marketplace.model.vo.EstadoProducto;
import com.uniremington.marketplace.model.vo.RolUsuario;
import com.uniremington.marketplace.model.vo.TiendaVO;
import com.uniremington.marketplace.model.vo.UsuarioVO;
import com.uniremington.marketplace.security.MarketplaceAuthorizationService;
import com.uniremington.marketplace.storage.ProductoImageStorageService;

class VendedorMvcControllerTests {

    private final UsuarioDAO usuarioDAO = mock(UsuarioDAO.class);
    private final TiendaDAO tiendaDAO = mock(TiendaDAO.class);
    private final ProductoDAO productoDAO = mock(ProductoDAO.class);
    private final CategoriaDAO categoriaDAO = mock(CategoriaDAO.class);
    private final FotoProductoDAO fotoDAO = mock(FotoProductoDAO.class);
    private final ProductoImageStorageService imageStorage = mock(ProductoImageStorageService.class);
    private final MarketplaceAuthorizationService authorizationService =
            mock(MarketplaceAuthorizationService.class);
    private final TestingAuthenticationToken authentication =
            new TestingAuthenticationToken("seller@test.com", "password");
    private VendedorMvcController controller;

    @BeforeEach
    void setUp() {
        controller = new VendedorMvcController(usuarioDAO, tiendaDAO, productoDAO, categoriaDAO,
                fotoDAO, imageStorage, authorizationService);
        when(usuarioDAO.buscarPorCorreo("seller@test.com")).thenReturn(Optional.of(
                new UsuarioVO(2L, "Vendedor", "seller@test.com", "hash", RolUsuario.VENDEDOR, true, null)));
    }

    @Test
    void panelSinTiendaMuestraListaDeProductosVacia() {
        when(tiendaDAO.buscarPorUsuarioId(2L)).thenReturn(Optional.empty());
        var model = new org.springframework.ui.ExtendedModelMap();

        assertEquals("vendedor/panel", controller.panel(authentication, model));
        assertEquals(java.util.List.of(), model.getAttribute("productos"));
        assertEquals(false, model.getAttribute("nuevoProducto"));
    }

    @Test
    void guardarTiendaCreaNuevaTiendaParaUsuarioAutenticado() {
        when(tiendaDAO.buscarPorUsuarioId(2L)).thenReturn(Optional.empty());

        assertEquals("redirect:/vendedor/panel", controller.guardarTienda(authentication,
                " Mi tienda ", null, null, null, null, new RedirectAttributesModelMap()));
        verify(tiendaDAO).crear(org.mockito.ArgumentMatchers.argThat(store ->
                store.usuarioId().equals(2L) && store.nombre().equals("Mi tienda")));
    }

    @Test
    void crearProductoRequiereTiendaYValidaCamposAntesDeCrear() {
        when(tiendaDAO.buscarPorUsuarioId(2L)).thenReturn(Optional.empty());
        assertEquals(400, assertThrows(ResponseStatusException.class, () -> controller.crearProducto(
                authentication, null, "Producto", null, null, null, BigDecimal.TEN,
                DisponibilidadProducto.DISPONIBLE, new RedirectAttributesModelMap()))
                .getStatusCode().value());

        when(tiendaDAO.buscarPorUsuarioId(2L)).thenReturn(Optional.of(
                new TiendaVO(4L, 2L, "Tienda", null, null, null, null, null, null)));
        assertEquals(400, assertThrows(ResponseStatusException.class, () -> controller.crearProducto(
                authentication, null, " ", null, null, null, BigDecimal.TEN,
                DisponibilidadProducto.DISPONIBLE, new RedirectAttributesModelMap()))
                .getStatusCode().value());
        verify(productoDAO, never()).crear(any());
    }

    @Test
    void crearProductoGuardaProductoPendienteYFotoUrlOpcional() {
        when(tiendaDAO.buscarPorUsuarioId(2L)).thenReturn(Optional.of(
                new TiendaVO(4L, 2L, "Tienda", null, null, null, null, null, null)));
        when(productoDAO.crear(any())).thenReturn(11L);

        assertEquals("redirect:/vendedor/panel", controller.crearProducto(authentication,
                3L, " Mesa ", "Descripción", null, " https://img.test/mesa.jpg ",
                new BigDecimal("25"), DisponibilidadProducto.DISPONIBLE,
                new RedirectAttributesModelMap()));
        verify(productoDAO).crear(org.mockito.ArgumentMatchers.argThat(product ->
                product.estado() == EstadoProducto.PENDIENTE && product.nombre().equals("Mesa")));
        verify(fotoDAO).crear(org.mockito.ArgumentMatchers.argThat(photo ->
                photo.productoId().equals(11L) && photo.principal()
                        && photo.url().equals("https://img.test/mesa.jpg")));
    }

    @Test
    void editarProductoOcultaRecursosNoAutorizados() {
        when(authorizationService.canManageProduct(11L, authentication)).thenReturn(false);

        assertEquals(404, assertThrows(ResponseStatusException.class, () -> controller.editarProducto(
                11L, authentication, null, "Mesa", null, null, null, BigDecimal.TEN,
                DisponibilidadProducto.DISPONIBLE, new RedirectAttributesModelMap()))
                .getStatusCode().value());
        verify(productoDAO, never()).buscarPorId(11L);
    }
}
