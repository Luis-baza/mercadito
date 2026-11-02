package com.uniremington.marketplace.model.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductoVO(
        Long id,
        Long tiendaId,
        Long categoriaId,
        String nombre,
        String descripcion,
        BigDecimal precio,
        DisponibilidadProducto disponibilidad,
        EstadoProducto estado,
        LocalDateTime creadoEn,
        LocalDateTime actualizadoEn
) {
}
