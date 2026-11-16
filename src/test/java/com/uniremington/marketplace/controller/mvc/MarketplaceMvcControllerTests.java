package com.uniremington.marketplace.controller.mvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.server.ResponseStatusException;

import com.uniremington.marketplace.model.dao.CategoriaDAO;
import com.uniremington.marketplace.model.dao.FotoProductoDAO;
import com.uniremington.marketplace.model.dao.ProductoDAO;
import com.uniremington.marketplace.model.dao.TiendaDAO;
import com.uniremington.marketplace.model.vo.CategoriaVO;
import com.uniremington.marketplace.model.vo.EstadoProducto;
import com.uniremington.marketplace.model.vo.EstadoTienda;
import com.uniremington.marketplace.model.vo.ProductoCatalogoVO;
import com.uniremington.marketplace.model.vo.ProductoVO;
import com.uniremington.marketplace.model.vo.TiendaVO;

class MarketplaceMvcControllerTests {

    private final ProductoDAO productoDAO = mock(ProductoDAO.class);
    private final CategoriaDAO categoriaDAO = mock(CategoriaDAO.class);
    private final TiendaDAO tiendaDAO = mock(TiendaDAO.class);
    private final FotoProductoDAO fotoDAO = mock(FotoProductoDAO.class);
    private MarketplaceMvcController controller;

    @BeforeEach
    void setUp() {
        controller = new MarketplaceMvcController(productoDAO, categoriaDAO, tiendaDAO, fotoDAO);
    }

    @Test
    void catalogoCargaProductosCategoriasYFiltrosEnModelo() {
        var model = new ExtendedModelMap();
        List<ProductoCatalogoVO> productos = List.of();
        when(productoDAO.listarCatalogoConFotos("mesa", 2L, null,
                new BigDecimal("10"), new BigDecimal("50"), true)).thenReturn(productos);
        when(categoriaDAO.listar()).thenReturn(List.of(new CategoriaVO(2L, "Hogar")));

        String view = controller.catalogo("mesa", 2L, null,
                new BigDecimal("10"), new BigDecimal("50"), "precio", model);

        assertEquals("catalogo", view);
        assertEquals(productos, model.getAttribute("productos"));
        assertEquals("mesa", model.getAttribute("texto"));
        assertEquals(List.of(new CategoriaVO(2L, "Hogar")), model.getAttribute("categorias"));
    }

    @Test
    void catalogoRechazaRangoInvalidoAntesDeConsultarServicios() {
        assertThrows(ResponseStatusException.class, () -> controller.catalogo(
                null, null, null, new BigDecimal("20"), new BigDecimal("10"), "recientes",
                new ExtendedModelMap()));
        org.mockito.Mockito.verifyNoInteractions(productoDAO, categoriaDAO);
    }

    @Test
    void detalleRequiereProductoPublicadoYTiendaAprobada() {
        ProductoVO producto = new ProductoVO(4L, 5L, 1L, "Mesa", "Madera", new BigDecimal("20"),
                null, EstadoProducto.PUBLICADO, null, null);
        TiendaVO tienda = new TiendaVO(5L, 1L, "Muebles", null, null, null, null,
                EstadoTienda.APROBADA, null);
        when(productoDAO.buscarPorId(4L)).thenReturn(Optional.of(producto));
        when(tiendaDAO.buscarPorId(5L)).thenReturn(Optional.of(tienda));
        when(fotoDAO.listarPorProducto(4L)).thenReturn(List.of());
        var model = new ExtendedModelMap();

        assertEquals("producto-detalle", controller.detalleProducto(4L, model));
        assertEquals(producto, model.getAttribute("producto"));
        assertEquals(tienda, model.getAttribute("tienda"));

        when(productoDAO.buscarPorId(9L)).thenReturn(Optional.empty());
        assertEquals(404, assertThrows(ResponseStatusException.class,
                () -> controller.detalleProducto(9L, new ExtendedModelMap())).getStatusCode().value());
    }

    @Test
    void perfilTiendaMuestraCatalogoDeTiendaAprobada() {
        TiendaVO tienda = new TiendaVO(3L, 1L, "Tienda", null, null, null, null,
                EstadoTienda.APROBADA, null);
        when(tiendaDAO.buscarPorId(3L)).thenReturn(Optional.of(tienda));

        assertEquals("tienda", controller.perfilTienda(3L, new ExtendedModelMap()));
        verify(productoDAO).listarCatalogoConFotos(null, null, 3L, null, null, false);
    }
}
