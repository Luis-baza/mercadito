package com.uniremington.marketplace.controller.mvc;

import com.uniremington.marketplace.model.dao.UsuarioDAO;
import com.uniremington.marketplace.model.vo.RolUsuario;
import com.uniremington.marketplace.model.vo.UsuarioVO;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class CuentaMvcController {

    private final UsuarioDAO usuarioDAO;
    private final PasswordEncoder passwordEncoder;

    public CuentaMvcController(UsuarioDAO usuarioDAO, PasswordEncoder passwordEncoder) {
        this.usuarioDAO = usuarioDAO;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/registro")
    public String mostrarRegistro() {
        return "registro";
    }

    @PostMapping("/registro")
    public String registrar(
            @RequestParam String nombre,
            @RequestParam String correo,
            @RequestParam String password,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        String nombreNormalizado = nombre.trim();
        String correoNormalizado = correo.trim();
        if (nombreNormalizado.isBlank()
                || correoNormalizado.isBlank()
                || !correoNormalizado.contains("@")
                || password.length() < 8) {
            model.addAttribute("error", "Ingresa un nombre, correo válido y contraseña de al menos 8 caracteres.");
            return "registro";
        }
        if (usuarioDAO.buscarPorCorreo(correoNormalizado).isPresent()) {
            model.addAttribute("error", "Ya existe una cuenta con ese correo.");
            return "registro";
        }

        usuarioDAO.crear(new UsuarioVO(
                null,
                nombreNormalizado,
                correoNormalizado,
                passwordEncoder.encode(password),
                RolUsuario.VENDEDOR,
                true,
                null
        ));
        redirectAttributes.addFlashAttribute("registroExitoso", true);
        return "redirect:/login";
    }

    @GetMapping("/acceso-denegado")
    public String accesoDenegado() {
        return "acceso-denegado";
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication) {
        if (authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMINISTRADOR"))) {
            return "redirect:/admin/panel";
        }
        return "redirect:/vendedor/panel";
    }
}
