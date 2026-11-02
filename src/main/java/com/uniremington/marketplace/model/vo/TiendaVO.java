package com.uniremington.marketplace.model.vo;

import java.time.LocalDateTime;

public record TiendaVO(
        Long id,
        Long usuarioId,
        String nombre,
        String descripcion,
        String telefono,
        String whatsapp,
        String correoContacto,
        EstadoTienda estado,
        LocalDateTime creadaEn
) {
}
