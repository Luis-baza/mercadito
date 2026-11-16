package com.uniremington.marketplace.controller.res;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.uniremington.marketplace.model.dao.TiendaDAO;
import com.uniremington.marketplace.model.vo.EstadoTienda;
import com.uniremington.marketplace.model.vo.TiendaVO;

class TiendaRestControllerTests {

    private final TiendaDAO tiendaDAO = mock(TiendaDAO.class);
    private TiendaRestController controller;

    @BeforeEach
    void setUp() {
        controller = new TiendaRestController(tiendaDAO);
    }

    @Test
    void crearAsignaEstadoPendienteYDevuelveLocation() {
        TiendaVO request = tienda(10L, 3L, "Tienda", EstadoTienda.APROBADA);
        TiendaVO creada = tienda(8L, 3L, "Tienda", EstadoTienda.PENDIENTE);
        when(tiendaDAO.crear(argThat(value -> value.id() == null
                && value.estado() == EstadoTienda.PENDIENTE))).thenReturn(8L);
        when(tiendaDAO.buscarPorId(8L)).thenReturn(Optional.of(creada));

        var response = controller.crear(request);

        assertEquals(201, response.getStatusCode().value());
        assertEquals("/api/tiendas/8", response.getHeaders().getLocation().toString());
        assertEquals(EstadoTienda.PENDIENTE, response.getBody().estado());
        verify(tiendaDAO).crear(argThat(value -> value.usuarioId().equals(3L)
                && value.estado() == EstadoTienda.PENDIENTE));
    }

    @Test
    void actualizarPreservaPropietarioYEstadoExistentes() {
        when(tiendaDAO.buscarPorId(4L)).thenReturn(Optional.of(
                tienda(4L, 2L, "Anterior", EstadoTienda.APROBADA)));
        when(tiendaDAO.actualizar(argThat(value -> value.id().equals(4L)
                && value.usuarioId().equals(2L)
                && value.estado() == EstadoTienda.APROBADA
                && value.nombre().equals("Nuevo")))).thenReturn(true);

        var response = controller.actualizar(4L, tienda(null, 99L, "Nuevo", EstadoTienda.RECHAZADA));

        assertEquals(204, response.getStatusCode().value());
        verify(tiendaDAO).actualizar(argThat(value -> value.usuarioId().equals(2L)
                && value.estado() == EstadoTienda.APROBADA));
    }

    @Test
    void actualizarYEliminarDevuelvenNotFoundParaRecursosInexistentes() {
        when(tiendaDAO.buscarPorId(4L)).thenReturn(Optional.empty());
        when(tiendaDAO.eliminar(4L)).thenReturn(false);

        assertEquals(404, controller.actualizar(4L, tienda(null, 2L, "Nuevo", null))
                .getStatusCode().value());
        assertEquals(404, controller.eliminar(4L).getStatusCode().value());
    }

    private TiendaVO tienda(Long id, Long usuarioId, String nombre, EstadoTienda estado) {
        return new TiendaVO(id, usuarioId, nombre, "Descripción", "300", "300", "contacto@test.com",
                estado, null);
    }
}
