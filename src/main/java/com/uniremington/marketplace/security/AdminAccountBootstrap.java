package com.uniremington.marketplace.security;

import com.uniremington.marketplace.model.dao.UsuarioDAO;
import com.uniremington.marketplace.model.vo.RolUsuario;
import com.uniremington.marketplace.model.vo.UsuarioVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.logging.Logger;

@Component
@ConditionalOnProperty(name = "marketplace.security.bootstrap-admin.enabled", havingValue = "true")
public class AdminAccountBootstrap implements ApplicationRunner {

    private static final Logger LOGGER = Logger.getLogger(AdminAccountBootstrap.class.getName());

    private final UsuarioDAO usuarioDAO;
    private final PasswordEncoder passwordEncoder;
    private final String correo;
    private final String password;

    public AdminAccountBootstrap(
            UsuarioDAO usuarioDAO,
            PasswordEncoder passwordEncoder,
            @Value("${marketplace.security.bootstrap-admin.email:}") String correo,
            @Value("${marketplace.security.bootstrap-admin.password:}") String password) {
        this.usuarioDAO = usuarioDAO;
        this.passwordEncoder = passwordEncoder;
        this.correo = correo.trim();
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (correo.isBlank() || password == null || password.length() < 12) {
            throw new IllegalStateException(
                    "El bootstrap de administrador requiere MARKETPLACE_ADMIN_EMAIL y "
                            + "MARKETPLACE_ADMIN_PASSWORD (mínimo 12 caracteres).");
        }

        UsuarioVO usuarioExistente = usuarioDAO.buscarPorCorreo(correo).orElse(null);
        String passwordHash = passwordEncoder.encode(password);
        if (usuarioExistente == null) {
            usuarioDAO.crear(new UsuarioVO(
                    null,
                    "Administrador",
                    correo,
                    passwordHash,
                    RolUsuario.ADMINISTRADOR,
                    true,
                    null));
            LOGGER.info("La cuenta de administrador configurada fue creada.");
        } else {
            if (usuarioExistente.rol() != RolUsuario.ADMINISTRADOR) {
                throw new IllegalStateException(
                        "El correo configurado ya pertenece a un usuario que no es administrador.");
            }
            usuarioDAO.actualizarPasswordAdministrador(correo, passwordHash);
            LOGGER.info("La cuenta de administrador configurada fue activada y su contraseña actualizada.");
        }
        LOGGER.warning(
                "Desactiva MARKETPLACE_SECURITY_BOOTSTRAP_ADMIN_ENABLED después de recuperar el acceso.");
    }
}
