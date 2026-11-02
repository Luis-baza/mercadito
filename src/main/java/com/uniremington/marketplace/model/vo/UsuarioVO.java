package com.uniremington.marketplace.model.vo;

import java.time.LocalDateTime;

public record UsuarioVO(
        Long id,
        String nombre,
        String correo,
        String password,
        RolUsuario rol,
        boolean activo,
        LocalDateTime creadoEn
) {
}
