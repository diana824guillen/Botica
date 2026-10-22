package com.boticavidasalud.backend.enums;

public enum EstadoRegistro {

    INACTIVO((short) 0, "Inactivo"),
    ACTIVO((short) 1, "Activo"),
    ELIMINADO((short) 2, "Eliminado");

    private final Short valor;
    private final String descripcion;

    EstadoRegistro(Short valor, String descripcion) {
        this.valor = valor;
        this.descripcion = descripcion;
    }

    public Short getValor() {
        return valor;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public static EstadoRegistro fromValor(Short valor) {
        for (EstadoRegistro e : values()) {
            if (e.valor.equals(valor)) return e;
        }
        throw new IllegalArgumentException("Estado no válido: " + valor);
    }
}


