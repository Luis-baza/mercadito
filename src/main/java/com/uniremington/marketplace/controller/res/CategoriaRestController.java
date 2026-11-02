package com.uniremington.marketplace.controller.res;

import com.uniremington.marketplace.model.dao.CategoriaDAO;
import com.uniremington.marketplace.model.vo.CategoriaVO;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/categorias")
public class CategoriaRestController {

    private final CategoriaDAO categoriaDAO;

    public CategoriaRestController(CategoriaDAO categoriaDAO) {
        this.categoriaDAO = categoriaDAO;
    }

    @GetMapping
    public List<CategoriaVO> listar() {
        return categoriaDAO.listar();
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoriaVO> buscarPorId(@PathVariable Long id) {
        return categoriaDAO.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<CategoriaVO> crear(@RequestBody CategoriaVO categoria) {
        Long id = categoriaDAO.crear(new CategoriaVO(null, categoria.nombre()));
        CategoriaVO creada = categoriaDAO.buscarPorId(id).orElseThrow();
        return ResponseEntity.created(URI.create("/api/categorias/" + id)).body(creada);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> actualizar(
            @PathVariable Long id,
            @RequestBody CategoriaVO categoria
    ) {
        return categoriaDAO.actualizar(new CategoriaVO(id, categoria.nombre()))
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        return categoriaDAO.eliminar(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}
