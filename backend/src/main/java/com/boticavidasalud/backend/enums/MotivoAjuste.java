package com.boticavidasalud.backend.enums;

public enum MotivoAjuste {
    
    // --- MERMAS (Pérdidas reales) ---
    PRODUCTO_VENCIDO(TipoMovimiento.MERMA, "Producto Vencido"),
    PRODUCTO_DAÑADO(TipoMovimiento.MERMA, "Producto Dañado o Roto"),
    PERDIDA_ROBO(TipoMovimiento.MERMA, "Pérdida por Robo"),
    DEVOLUCION_PROVEEDOR(TipoMovimiento.MERMA, "Devolución a Proveedor"),
    USO_INTERNO_DONACION(TipoMovimiento.MERMA, "Uso Interno o Donación"),

    // --- AJUSTES (Correcciones) ---
    ERROR_CONTEO_FISICO(TipoMovimiento.AJUSTE, "Faltante/Sobrante en Conteo"),
    ERROR_DIGITACION(TipoMovimiento.AJUSTE, "Error de Digitación"),
    AJUSTE_EMPAQUE(TipoMovimiento.AJUSTE, "Ajuste por Apertura de Empaque");

    private final TipoMovimiento tipoMovimiento;
    private final String descripcion;

    MotivoAjuste(TipoMovimiento tipoMovimiento, String descripcion) {
        this.tipoMovimiento = tipoMovimiento;
        this.descripcion = descripcion;
    }

    public TipoMovimiento getTipoMovimiento() {
        return tipoMovimiento;
    }

    public String getDescripcion() {
        return descripcion;
    }
}


