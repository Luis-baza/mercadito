package com.uniremington.marketplace.controller.mvc;

import com.uniremington.marketplace.model.dao.CategoriaDAO;
import com.uniremington.marketplace.model.dao.FotoProductoDAO;
import com.uniremington.marketplace.model.dao.ProductoDAO;
import com.uniremington.marketplace.model.dao.TiendaDAO;
import com.uniremington.marketplace.model.dao.UsuarioDAO;
import com.uniremington.marketplace.model.vo.DisponibilidadProducto;
import com.uniremington.marketplace.model.vo.EstadoProducto;
import com.uniremington.marketplace.model.vo.FotoProductoVO;
import com.uniremington.marketplace.model.vo.ProductoVO;
import com.uniremington.marketplace.model.vo.TiendaVO;
import com.uniremington.marketplace.model.vo.UsuarioVO;
import com.uniremington.marketplace.security.MarketplaceAuthorizationService;
import com.uniremington.marketplace.storage.ProductoImageStorageService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.math.BigDecimal;

@Controller
@RequestMapping("/vendedor")
@PreAuthorize("hasRole('VENDEDOR')")
public class VendedorMvcController {

    private final UsuarioDAO usuarioDAO;
    private final TiendaDAO tiendaDAO;
    private final ProductoDAO productoDAO;
    private final CategoriaDAO categoriaDAO;
    private final FotoProductoDAO fotoProductoDAO;
    private final ProductoImageStorageService imageStorageService;
    private final MarketplaceAuthorizationService authorizationService;

    public VendedorMvcController(
            UsuarioDAO usuarioDAO,
            TiendaDAO tiendaDAO,
            ProductoDAO productoDAO,
            CategoriaDAO categoriaDAO,
            FotoProductoDAO fotoProductoDAO,
            ProductoImageStorageService imageStorageService,
            MarketplaceAuthorizationService authorizationService
    ) {
        this.usuarioDAO = usuarioDAO;
        this.tiendaDAO = tiendaDAO;
        this.productoDAO = productoDAO;
        this.categoriaDAO = categoriaDAO;
        this.fotoProductoDAO = fotoProductoDAO;
        this.imageStorageService = imageStorageService;
        this.authorizationService = authorizationService;
    }

    @GetMapping("/panel")
    public String panel(Authentication authentication, Model model) {
        UsuarioVO usuario = usuarioActual(authentication);
        TiendaVO tienda = tiendaDAO.buscarPorUsuarioId(usuario.id()).orElse(null);

        model.addAttribute("usuario", usuario);
        model.addAttribute("tienda", tienda);
        model.addAttribute("productos", tienda == null
                ? java.util.List.of()
                : productoDAO.listarPorTienda(tienda.id()));
        model.addAttribute("fotosPrincipales", tienda == null
                ? java.util.Map.of()
                : fotoProductoDAO.listarFotosPrincipalesPorTienda(tienda.id()));
        model.addAttribute("categorias", categoriaDAO.listar());
        model.addAttribute("nuevoProducto", tienda != null);
        return "vendedor/panel";
    }

    @PostMapping("/tienda")
    public String guardarTienda(
            Authentication authentication,
            @RequestParam String nombre,
            @RequestParam(required = false) String descripcion,
            @RequestParam(required = false) String telefono,
            @RequestParam(required = false) String whatsapp,
            @RequestParam(required = false) String correoContacto,
            RedirectAttributes redirectAttributes
    ) {
        UsuarioVO usuario = usuarioActual(authentication);
        if (nombre == null || nombre.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre de la tienda es obligatorio");
        }

        TiendaVO existente = tiendaDAO.buscarPorUsuarioId(usuario.id()).orElse(null);
        TiendaVO tienda = new TiendaVO(
                existente == null ? null : existente.id(),
                usuario.id(),
                nombre.trim(),
                descripcion,
                telefono,
                whatsapp,
                correoContacto,
                existente == null ? null : existente.estado(),
                existente == null ? null : existente.creadaEn()
        );

        if (existente == null) {
            tiendaDAO.crear(tienda);
            redirectAttributes.addFlashAttribute(
                    "mensaje",
                    "Tienda creada. Quedará visible cuando un administrador la apruebe."
            );
        } else {
            if (!tiendaDAO.actualizar(tienda)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró la tienda");
            }
            redirectAttributes.addFlashAttribute("mensaje", "Datos de la tienda actualizados.");
        }
        return "redirect:/vendedor/panel";
    }

    @PostMapping("/productos")
    @Transactional
    public String crearProducto(
            Authentication authentication,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam String nombre,
            @RequestParam(required = false) String descripcion,
            @RequestParam(required = false) MultipartFile foto,
            @RequestParam(required = false) String fotoUrl,
            @RequestParam BigDecimal precio,
            @RequestParam(defaultValue = "DISPONIBLE") DisponibilidadProducto disponibilidad,
            RedirectAttributes redirectAttributes
    ) {
        UsuarioVO usuario = usuarioActual(authentication);
        TiendaVO tienda = tiendaDAO.buscarPorUsuarioId(usuario.id())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Primero debes crear los datos de tu tienda"
                ));
        validarProducto(nombre, precio);
        Long productoId = productoDAO.crear(new ProductoVO(
                null,
                tienda.id(),
                categoriaId,
                nombre.trim(),
                descripcion,
                precio,
                disponibilidad,
                EstadoProducto.PENDIENTE,
                null,
                null
        ));
        guardarFoto(productoId, foto, fotoUrl, true);
        redirectAttributes.addFlashAttribute(
                "mensaje",
                "Producto enviado. Se publicará cuando sea aprobado por un administrador."
        );
        return "redirect:/vendedor/panel";
    }

    @PostMapping("/productos/{id}/editar")
    @Transactional
    public String editarProducto(
            @PathVariable Long id,
            Authentication authentication,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam String nombre,
            @RequestParam(required = false) String descripcion,
            @RequestParam(required = false) MultipartFile foto,
            @RequestParam(required = false) String fotoUrl,
            @RequestParam BigDecimal precio,
            @RequestParam DisponibilidadProducto disponibilidad,
            RedirectAttributes redirectAttributes
    ) {
        if (!authorizationService.canManageProduct(id, authentication)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        validarProducto(nombre, precio);
        ProductoVO existente = productoDAO.buscarPorId(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        productoDAO.actualizar(new ProductoVO(
                existente.id(),
                existente.tiendaId(),
                categoriaId,
                nombre.trim(),
                descripcion,
                precio,
                disponibilidad,
                EstadoProducto.PENDIENTE,
                existente.creadoEn(),
                existente.actualizadoEn()
        ));
        guardarFoto(id, foto, fotoUrl, true);
        redirectAttributes.addFlashAttribute("mensaje", "Producto actualizado.");
        return "redirect:/vendedor/panel";
    }

    @PostMapping("/productos/{id}/retirar")
    public String retirarProducto(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        if (!authorizationService.canManageProduct(id, authentication)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        productoDAO.actualizarEstado(id, EstadoProducto.RETIRADO);
        redirectAttributes.addFlashAttribute("mensaje", "Producto retirado del catálogo.");
        return "redirect:/vendedor/panel";
    }

    private UsuarioVO usuarioActual(Authentication authentication) {
        return usuarioDAO.buscarPorCorreo(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    private void validarProducto(String nombre, BigDecimal precio) {
        if (nombre == null || nombre.isBlank() || precio == null || precio.signum() < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El nombre es obligatorio y el precio no puede ser negativo"
            );
        }
    }

    private void guardarFoto(Long productoId, MultipartFile foto, String fotoUrl, boolean principal) {
        boolean archivoSeleccionado = foto != null && !foto.isEmpty();
        boolean urlSeleccionada = fotoUrl != null && !fotoUrl.isBlank();
        if (archivoSeleccionado && urlSeleccionada) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Selecciona un archivo o proporciona una URL, no ambos"
            );
        }
        if (!archivoSeleccionado && !urlSeleccionada) {
            return;
        }

        String url = urlSeleccionada ? fotoUrl.trim() : null;
        if (archivoSeleccionado) {
            try {
                url = imageStorageService.store(foto);
            } catch (IOException exception) {
                throw new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "No se pudo guardar la imagen seleccionada",
                        exception
                );
            }
        }

        try {
            fotoProductoDAO.crear(new FotoProductoVO(null, productoId, url, principal));
        } catch (RuntimeException exception) {
            if (archivoSeleccionado) {
                try {
                    imageStorageService.delete(url);
                } catch (IOException cleanupException) {
                    exception.addSuppressed(cleanupException);
                }
            }
            throw exception;
        }
    }
}
