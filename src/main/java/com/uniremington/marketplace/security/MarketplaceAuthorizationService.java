package com.uniremington.marketplace.security;

import com.uniremington.marketplace.model.dao.ProductoDAO;
import com.uniremington.marketplace.model.dao.TiendaDAO;
import com.uniremington.marketplace.model.dao.UsuarioDAO;
import com.uniremington.marketplace.model.vo.EstadoProducto;
import com.uniremington.marketplace.model.vo.EstadoTienda;
import com.uniremington.marketplace.model.vo.UsuarioVO;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("marketplaceAuthorization")
public class MarketplaceAuthorizationService {

    private final UsuarioDAO usuarioDAO;
    private final TiendaDAO tiendaDAO;
    private final ProductoDAO productoDAO;

    public MarketplaceAuthorizationService(
            UsuarioDAO usuarioDAO,
            TiendaDAO tiendaDAO,
            ProductoDAO productoDAO
    ) {
        this.usuarioDAO = usuarioDAO;
        this.tiendaDAO = tiendaDAO;
        this.productoDAO = productoDAO;
    }

    public boolean isAdmin(Authentication authentication) {
        return tieneRol(authentication, "ROLE_ADMINISTRADOR");
    }

    public boolean canCreateStore(Long userId, Authentication authentication) {
        return tieneRol(authentication, "ROLE_VENDEDOR")
                && usuarioAutenticado(authentication)
                .map(usuario -> usuario.id().equals(userId))
                .orElse(false);
    }

    public boolean canManageStore(Long storeId, Authentication authentication) {
        if (isAdmin(authentication)) {
            return true;
        }
        if (!tieneRol(authentication, "ROLE_VENDEDOR")) {
            return false;
        }
        return tiendaDAO.buscarPorId(storeId)
                .map(tienda -> esUsuarioActual(tienda.usuarioId(), authentication))
                .orElse(false);
    }

    public boolean canViewStore(Long storeId, Authentication authentication) {
        return tiendaDAO.buscarPorId(storeId)
                .map(tienda -> tienda.estado() == EstadoTienda.APROBADA
                        || canManageStore(storeId, authentication))
                .orElse(false);
    }

    public boolean canManageProduct(Long productId, Authentication authentication) {
        if (isAdmin(authentication)) {
            return true;
        }
        return productoDAO.buscarPorId(productId)
                .map(producto -> canManageStore(producto.tiendaId(), authentication))
                .orElse(false);
    }

    public boolean canManageProductsInStore(Long storeId, Authentication authentication) {
        return canManageStore(storeId, authentication);
    }

    public boolean canViewProduct(Long productId, Authentication authentication) {
        return productoDAO.buscarPorId(productId)
                .map(producto -> producto.estado() == EstadoProducto.PUBLICADO
                        && tiendaDAO.buscarPorId(producto.tiendaId())
                        .map(tienda -> tienda.estado() == EstadoTienda.APROBADA)
                        .orElse(false)
                        || canManageProduct(productId, authentication))
                .orElse(false);
    }

    public boolean canManageUser(Long userId, Authentication authentication) {
        if (isAdmin(authentication)) {
            return true;
        }
        return usuarioAutenticado(authentication)
                .map(usuario -> usuario.id().equals(userId))
                .orElse(false);
    }

    private boolean esUsuarioActual(Long userId, Authentication authentication) {
        return usuarioAutenticado(authentication)
                .map(usuario -> usuario.id().equals(userId))
                .orElse(false);
    }

    private java.util.Optional<UsuarioVO> usuarioAutenticado(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return java.util.Optional.empty();
        }
        return usuarioDAO.buscarPorCorreo(authentication.getName());
    }

    private boolean tieneRol(Authentication authentication, String rol) {
        return authentication != null
                && authentication.isAuthenticated()
                && authentication.getAuthorities().stream()
                .anyMatch(authority -> rol.equals(authority.getAuthority()));
    }
}
