package com.uniremington.marketplace.controller.res;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.uniremington.marketplace.model.dao.CategoriaDAO;
import com.uniremington.marketplace.model.vo.CategoriaVO;

class CategoriaRestControllerTests {

    private final CategoriaDAO categoriaDAO = mock(CategoriaDAO.class);
    private CategoriaRestController controller;

    @BeforeEach
    void setUp() {
        controller = new CategoriaRestController(categoriaDAO);
    }

    @Test
    void listaCategorias() {
        List<CategoriaVO> categorias = List.of(new CategoriaVO(1L, "Hogar"));
        when(categoriaDAO.listar()).thenReturn(categorias);

        assertEquals(categorias, controller.listar());
    }

    @Test
    void buscaCategoriaYDevuelveNotFoundCuandoNoExiste() {
        when(categoriaDAO.buscarPorId(1L)).thenReturn(Optional.of(new CategoriaVO(1L, "Hogar")));
        when(categoriaDAO.buscarPorId(2L)).thenReturn(Optional.empty());

        assertEquals(200, controller.buscarPorId(1L).getStatusCode().value());
        assertEquals("Hogar", controller.buscarPorId(1L).getBody().nombre());
        assertEquals(404, controller.buscarPorId(2L).getStatusCode().value());
    }

    @Test
    void creaCategoriaConIdGenerado() {
        when(categoriaDAO.crear(new CategoriaVO(null, "Hogar"))).thenReturn(7L);
        when(categoriaDAO.buscarPorId(7L)).thenReturn(Optional.of(new CategoriaVO(7L, "Hogar")));

        var response = controller.crear(new CategoriaVO(null, "Hogar"));

        assertEquals(201, response.getStatusCode().value());
        assertEquals("/api/categorias/7", response.getHeaders().getLocation().toString());
        assertEquals(7L, response.getBody().id());
        verify(categoriaDAO).crear(new CategoriaVO(null, "Hogar"));
    }

    @Test
    void actualizarYEliminarReflejanSiExisteLaCategoria() {
        when(categoriaDAO.actualizar(new CategoriaVO(3L, "Tecnología"))).thenReturn(true, false);
        when(categoriaDAO.eliminar(3L)).thenReturn(true, false);

        assertEquals(204, controller.actualizar(3L, new CategoriaVO(null, "Tecnología"))
                .getStatusCode().value());
        assertEquals(404, controller.actualizar(3L, new CategoriaVO(null, "Tecnología"))
                .getStatusCode().value());
        assertEquals(204, controller.eliminar(3L).getStatusCode().value());
        assertEquals(404, controller.eliminar(3L).getStatusCode().value());
        assertFalse(controller.eliminar(3L).hasBody());
    }
}
