package com.uniremington.marketplace.controller.mvc;

import com.uniremington.marketplace.model.dao.CategoriaDAO;
import com.uniremington.marketplace.model.dao.FotoProductoDAO;
import com.uniremington.marketplace.model.dao.ProductoDAO;
import com.uniremington.marketplace.model.dao.TiendaDAO;
import com.uniremington.marketplace.model.vo.EstadoProducto;
import com.uniremington.marketplace.model.vo.EstadoTienda;
import com.uniremington.marketplace.model.vo.ProductoVO;
import com.uniremington.marketplace.model.vo.TiendaVO;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

@Controller
public class MarketplaceMvcController {

    private final ProductoDAO productoDAO;
    private final CategoriaDAO categoriaDAO;
    private final TiendaDAO tiendaDAO;
    private final FotoProductoDAO fotoProductoDAO;

    public MarketplaceMvcController(
            ProductoDAO productoDAO,
            CategoriaDAO categoriaDAO,
            TiendaDAO tiendaDAO,
            FotoProductoDAO fotoProductoDAO
    ) {
        this.productoDAO = productoDAO;
        this.categoriaDAO = categoriaDAO;
        this.tiendaDAO = tiendaDAO;
        this.fotoProductoDAO = fotoProductoDAO;
    }

    @GetMapping({"/", "/catalogo"})
    public String catalogo(
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) Long tiendaId,
            @RequestParam(required = false) BigDecimal precioMinimo,
            @RequestParam(required = false) BigDecimal precioMaximo,
            @RequestParam(defaultValue = "recientes") String ordenar,
            Model model
    ) {
        if (rangoInvalido(precioMinimo, precioMaximo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El rango de precios no es válido");
        }
        model.addAttribute("productos", productoDAO.listarCatalogo(
                texto,
                categoriaId,
                tiendaId,
                precioMinimo,
                precioMaximo,
                "precio".equalsIgnoreCase(ordenar)
        ));
        model.addAttribute("categorias", categoriaDAO.listar());
        model.addAttribute("texto", texto);
        model.addAttribute("categoriaId", categoriaId);
        model.addAttribute("tiendaId", tiendaId);
        model.addAttribute("precioMinimo", precioMinimo);
        model.addAttribute("precioMaximo", precioMaximo);
        model.addAttribute("ordenar", ordenar);
        return "catalogo";
    }

    @GetMapping("/productos/{id}")
    public String detalleProducto(@PathVariable Long id, Model model) {
        ProductoVO producto = productoDAO.buscarPorId(id)
                .filter(item -> item.estado() == EstadoProducto.PUBLICADO)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        TiendaVO tienda = tiendaDAO.buscarPorId(producto.tiendaId())
                .filter(item -> item.estado() == EstadoTienda.APROBADA)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        model.addAttribute("producto", producto);
        model.addAttribute("tienda", tienda);
        model.addAttribute("fotos", fotoProductoDAO.listarPorProducto(id));
        return "producto-detalle";
    }

    @GetMapping("/tiendas/{id}")
    public String perfilTienda(@PathVariable Long id, Model model) {
        TiendaVO tienda = tiendaDAO.buscarPorId(id)
                .filter(item -> item.estado() == EstadoTienda.APROBADA)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("tienda", tienda);
        model.addAttribute("productos", productoDAO.listarCatalogo(
                null, null, tienda.id(), null, null, false
        ));
        return "tienda";
    }

    private boolean rangoInvalido(BigDecimal minimo, BigDecimal maximo) {
        return (minimo != null && minimo.signum() < 0)
                || (maximo != null && maximo.signum() < 0)
                || (minimo != null && maximo != null && minimo.compareTo(maximo) > 0);
    }
}
