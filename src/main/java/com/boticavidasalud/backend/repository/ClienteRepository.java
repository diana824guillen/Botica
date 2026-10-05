package com.boticavidasalud.backend.repository;

import com.boticavidasalud.backend.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ClienteRepository extends JpaRepository<Cliente, Integer> {

    @Procedure(procedureName = "sp_cliente_listar")
    List<Cliente> listarClientes();

    @Procedure(procedureName = "sp_cliente_obtener_por_documento")
    List<Cliente> obtenerPorDocumento(@Param("p_numero_documento") String numeroDocumento);

    @Procedure(procedureName = "sp_cliente_obtener_por_id")
    List<Cliente> obtenerPorIdProcedure(@Param("p_id_cliente") Integer id);
}


