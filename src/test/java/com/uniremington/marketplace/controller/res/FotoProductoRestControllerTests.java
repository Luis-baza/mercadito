package com.uniremington.marketplace.controller.res;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.uniremington.marketplace.model.dao.FotoProductoDAO;
import com.uniremington.marketplace.model.vo.FotoProductoVO;

class FotoProductoRestControllerTests {

    private final FotoProductoDAO fotoDAO = mock(FotoProductoDAO.class);
    private FotoProductoRestController controller;

    @BeforeEach
    void setUp() {
        controller = new FotoProductoRestController(fotoDAO);
    }

    @Test
    void crearMarcaComoPrincipalSoloCuandoLaSolicitudLoIndica() {
        FotoProductoVO request = new FotoProductoVO(null, 4L, "https://img.test/foto.jpg", true);
        FotoProductoVO creada = new FotoProductoVO(8L, 4L, request.url(), true);
        when(fotoDAO.crear(new FotoProductoVO(null, 4L, request.url(), false))).thenReturn(8L);
        when(fotoDAO.listarPorProducto(4L)).thenReturn(List.of(creada));

        var response = controller.crear(4L, request);

        assertEquals(201, response.getStatusCode().value());
        assertEquals("/api/productos/4/fotos/8", response.getHeaders().getLocation().toString());
        verify(fotoDAO).marcarComoPrincipal(4L, 8L);
    }

    @Test
    void noActualizaNiEliminaFotosQueNoPertenecenAlProducto() {
        when(fotoDAO.listarPorProducto(4L)).thenReturn(List.of(new FotoProductoVO(9L, 4L, "url", false)));

        assertEquals(404, controller.actualizarUrl(4L, 10L, "nueva-url")
                .getStatusCode().value());
        assertEquals(404, controller.eliminar(4L, 10L).getStatusCode().value());
        assertEquals(404, controller.marcarPrincipal(4L, 10L).getStatusCode().value());
        verify(fotoDAO, never()).actualizarUrl(10L, "nueva-url");
        verify(fotoDAO, never()).eliminar(10L);
        verify(fotoDAO, never()).marcarComoPrincipal(4L, 10L);
    }

    @Test
    void actualizaUrlYMarcaFotoPropiaComoPrincipal() {
        when(fotoDAO.listarPorProducto(4L)).thenReturn(List.of(new FotoProductoVO(9L, 4L, "url", false)));
        when(fotoDAO.actualizarUrl(9L, "https://img.test/nueva.jpg")).thenReturn(true);

        assertEquals(204, controller.actualizarUrl(4L, 9L, "https://img.test/nueva.jpg")
                .getStatusCode().value());
        assertEquals(204, controller.marcarPrincipal(4L, 9L).getStatusCode().value());
        verify(fotoDAO).marcarComoPrincipal(4L, 9L);
    }
}
