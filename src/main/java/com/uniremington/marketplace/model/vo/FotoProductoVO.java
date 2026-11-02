package com.uniremington.marketplace.model.vo;

public record FotoProductoVO(
        Long id,
        Long productoId,
        String url,
        boolean principal
) {
}
