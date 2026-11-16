package com.uniremington.marketplace.controller.res;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;

import com.uniremington.marketplace.model.dao.ProductoDAO;
import com.uniremington.marketplace.model.vo.DisponibilidadProducto;
import com.uniremington.marketplace.model.vo.EstadoProducto;
import com.uniremington.marketplace.model.vo.ProductoVO;
import com.uniremington.marketplace.security.MarketplaceAuthorizationService;

class ProductoRestControllerTests {

    private final ProductoDAO productoDAO = mock(ProductoDAO.class);
    private final MarketplaceAuthorizationService authorizationService =
            mock(MarketplaceAuthorizationService.class);
    private ProductoRestController controller;

    @BeforeEach
    void setUp() {
        controller = new ProductoRestController(productoDAO, authorizationService);
    }

    @Test
    void catalogoValidaFiltrosYActivaOrdenPorPrecio() {
        controller.catalogo(" libro ", 2L, 3L, new BigDecimal("10"), new BigDecimal("20"), "PRECIO");

        verify(productoDAO).listarCatalogo(" libro ", 2L, 3L,
                new BigDecimal("10"), new BigDecimal("20"), true);
    }

    @Test
    void catalogoRechazaRangoDePreciosInvalidoSinConsultarDao() {
        assertThrows(ResponseStatusException.class, () -> controller.catalogo(
                null, null, null, new BigDecimal("20"), new BigDecimal("10"), "recientes"));
        assertThrows(ResponseStatusException.class, () -> controller.catalogo(
                null, null, null, new BigDecimal("-1"), null, "recientes"));
    }

    @Test
    void listarPorTiendaUsaInventarioPrivadoParaPropietarioYCatalogoPublicoParaOtros() {
        var authentication = new TestingAuthenticationToken("seller@test.com", "password");
        when(authorizationService.canManageStore(5L, authentication)).thenReturn(true, false);

        controller.listarPorTienda(5L, authentication);
        controller.listarPorTienda(5L, authentication);

        verify(productoDAO).listarPorTienda(5L);
        verify(productoDAO).listarCatalogo(null, null, 5L, null, null, false);
    }

    @Test
    void crearFuerzaEstadoPendiente() {
        ProductoVO request = producto(null, EstadoProducto.PUBLICADO);
        ProductoVO creado = producto(12L, EstadoProducto.PENDIENTE);
        when(productoDAO.crear(org.mockito.ArgumentMatchers.argThat(value ->
                value.estado() == EstadoProducto.PENDIENTE && value.id() == null))).thenReturn(12L);
        when(productoDAO.buscarPorId(12L)).thenReturn(Optional.of(creado));

        var response = controller.crear(request);

        assertEquals(201, response.getStatusCode().value());
        assertEquals("/api/productos/12", response.getHeaders().getLocation().toString());
        assertEquals(EstadoProducto.PENDIENTE, response.getBody().estado());
    }

    @Test
    void actualizarPreservaEstadoParaAdministradorYReiniciaPublicacionParaVendedor() {
        var authentication = new TestingAuthenticationToken("admin@test.com", "password");
        when(productoDAO.buscarPorId(9L)).thenReturn(Optional.of(producto(9L, EstadoProducto.PUBLICADO)));
        when(authorizationService.isAdmin(authentication)).thenReturn(true, false);
        when(productoDAO.actualizar(org.mockito.ArgumentMatchers.any(ProductoVO.class))).thenReturn(true);

        assertEquals(204, controller.actualizar(9L, producto(null, EstadoProducto.RECHAZADO), authentication)
                .getStatusCode().value());
        assertEquals(204, controller.actualizar(9L, producto(null, EstadoProducto.RECHAZADO), authentication)
                .getStatusCode().value());
        verify(productoDAO).actualizar(org.mockito.ArgumentMatchers.argThat(value ->
                value.estado() == EstadoProducto.PUBLICADO));
        verify(productoDAO).actualizar(org.mockito.ArgumentMatchers.argThat(value ->
                value.estado() == EstadoProducto.PENDIENTE));
    }

    @Test
    void buscarPorIdDevuelveNotFoundCuandoNoExiste() {
        when(productoDAO.buscarPorId(2L)).thenReturn(Optional.empty());

        assertEquals(404, controller.buscarPorId(2L).getStatusCode().value());
    }

    private ProductoVO producto(Long id, EstadoProducto estado) {
        return new ProductoVO(id, 5L, 2L, "Libro", "Descripción", new BigDecimal("15.00"),
                DisponibilidadProducto.DISPONIBLE, estado, null, null);
    }
}
