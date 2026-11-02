package com.uniremington.marketplace.controller.mvc;

import com.uniremington.marketplace.model.dao.ProductoDAO;
import com.uniremington.marketplace.model.dao.TiendaDAO;
import com.uniremington.marketplace.model.dao.UsuarioDAO;
import com.uniremington.marketplace.model.vo.EstadoProducto;
import com.uniremington.marketplace.model.vo.EstadoTienda;
import com.uniremington.marketplace.model.vo.RolUsuario;
import com.uniremington.marketplace.model.vo.UsuarioVO;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class AdministradorMvcController {

    private final UsuarioDAO usuarioDAO;
    private final TiendaDAO tiendaDAO;
    private final ProductoDAO productoDAO;

    public AdministradorMvcController(
            UsuarioDAO usuarioDAO,
            TiendaDAO tiendaDAO,
            ProductoDAO productoDAO
    ) {
        this.usuarioDAO = usuarioDAO;
        this.tiendaDAO = tiendaDAO;
        this.productoDAO = productoDAO;
    }

    @GetMapping("/panel")
    public String panel(Model model) {
        model.addAttribute("tiendasPendientes", tiendaDAO.listarPorEstado(EstadoTienda.PENDIENTE));
        model.addAttribute("productosPendientes", productoDAO.listarPendientesModeracion());
        model.addAttribute("usuarios", usuarioDAO.listar().stream()
                .map(usuario -> new UsuarioResumen(
                        usuario.id(),
                        usuario.nombre(),
                        usuario.correo(),
                        usuario.rol(),
                        usuario.activo()
                ))
                .toList());
        return "admin/panel";
    }

    @PostMapping("/tiendas/{id}/estado")
    public String actualizarEstadoTienda(
            @PathVariable Long id,
            @RequestParam EstadoTienda estado,
            RedirectAttributes redirectAttributes
    ) {
        if (estado == EstadoTienda.PENDIENTE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecciona un estado de revisión");
        }
        if (!tiendaDAO.actualizarEstado(id, estado)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró la tienda");
        }
        redirectAttributes.addFlashAttribute("mensaje", "Estado de tienda actualizado.");
        return "redirect:/admin/panel";
    }

    @PostMapping("/productos/{id}/estado")
    public String actualizarEstadoProducto(
            @PathVariable Long id,
            @RequestParam EstadoProducto estado,
            RedirectAttributes redirectAttributes
    ) {
        if (estado == EstadoProducto.PENDIENTE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecciona un estado de moderación");
        }
        if (!productoDAO.actualizarEstado(id, estado)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró el producto");
        }
        redirectAttributes.addFlashAttribute("mensaje", "Estado de producto actualizado.");
        return "redirect:/admin/panel";
    }

    @PostMapping("/usuarios/{id}/activo")
    public String actualizarEstadoUsuario(
            @PathVariable Long id,
            @RequestParam boolean activo,
            RedirectAttributes redirectAttributes
    ) {
        UsuarioVO usuario = usuarioDAO.buscarPorId(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró el usuario"));
        if (usuario.rol() == RolUsuario.ADMINISTRADOR && !activo) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "No puedes desactivar una cuenta de administrador desde este panel"
            );
        }
        usuarioDAO.actualizarEstado(id, activo);
        redirectAttributes.addFlashAttribute("mensaje", "Estado de cuenta actualizado.");
        return "redirect:/admin/panel";
    }

    public record UsuarioResumen(
            Long id,
            String nombre,
            String correo,
            RolUsuario rol,
            boolean activo
    ) {
    }
}
