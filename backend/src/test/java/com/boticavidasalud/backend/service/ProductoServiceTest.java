package com.boticavidasalud.backend.service;

import com.boticavidasalud.backend.dto.ProductoRequestDTO;
import com.boticavidasalud.backend.dto.ProductoResponseDTO;
import com.boticavidasalud.backend.dto.PresentacionDTO;
import com.boticavidasalud.backend.entity.Categoria;
import com.boticavidasalud.backend.entity.Producto;
import com.boticavidasalud.backend.enums.TipoPresentacion;
import com.boticavidasalud.backend.exception.ResourceNotFoundException;
import com.boticavidasalud.backend.repository.CategoriaRepository;
import com.boticavidasalud.backend.repository.PresentacionRepository;
import com.boticavidasalud.backend.repository.ProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private CategoriaRepository categoriaRepository;

    @Mock
    private PresentacionRepository presentacionRepository;

    @InjectMocks
    private ProductoService productoService;

    private ProductoRequestDTO requestDTO;
    private Categoria categoria;

    @BeforeEach
    void setUp() {
        categoria = new Categoria();
        categoria.setId(1);
        categoria.setNombre("Medicamentos");

        requestDTO = new ProductoRequestDTO();
        requestDTO.setCategoriaId(1);
        requestDTO.setNombre("Paracetamol");
        requestDTO.setDescripcion("Analgésico");
        requestDTO.setRequiereReceta(false);
        requestDTO.setStockMinimo(BigDecimal.TEN);

        List<PresentacionDTO> presentaciones = new ArrayList<>();
        PresentacionDTO pres1 = new PresentacionDTO();
        pres1.setTipo(TipoPresentacion.UNIDAD);
        pres1.setFactorConversion(1);
        pres1.setPrecioVenta(new BigDecimal("1.50"));
        presentaciones.add(pres1);

        PresentacionDTO pres2 = new PresentacionDTO();
        pres2.setTipo(TipoPresentacion.BLISTER);
        pres2.setFactorConversion(10);
        pres2.setPrecioVenta(new BigDecimal("12.00"));
        presentaciones.add(pres2);

        requestDTO.setPresentaciones(presentaciones);
    }

    @Test
    void crearProducto_conCategoriaValida_deberiaGuardarProductoYPresentaciones() {
        when(categoriaRepository.findById(1)).thenReturn(Optional.of(categoria));
        when(productoRepository.save(any(Producto.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(presentacionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ProductoResponseDTO response = productoService.crearProducto(requestDTO);

        assertNotNull(response);
        assertEquals("Paracetamol", response.getNombre());
        assertEquals("Medicamentos", response.getCategoriaNombre());

        verify(productoRepository, times(1)).save(any(Producto.class));
        verify(presentacionRepository, times(2)).save(any());
    }

    @Test
    void crearProducto_conCategoriaInexistente_deberiaLanzarExcepcion() {
        when(categoriaRepository.findById(999)).thenReturn(Optional.empty());
        requestDTO.setCategoriaId(999);

        assertThrows(ResourceNotFoundException.class,
                () -> productoService.crearProducto(requestDTO));
        verify(productoRepository, never()).save(any(Producto.class));
        verify(presentacionRepository, never()).save(any());
    }

    @Test
    void obtenerProductoPorId_conIdValido_deberiaRetornarProducto() {
        Producto productoMock = new Producto();
        productoMock.setId(1);
        productoMock.setCategoria(categoria);
        productoMock.setNombre("Paracetamol");
        productoMock.setDescripcion("Analgésico");
        productoMock.setRequiereReceta(false);
        productoMock.setStockMinimo(BigDecimal.TEN);
        productoMock.setEstado((short) 1);
        productoMock.setPresentaciones(new ArrayList<>());

        when(productoRepository.findById(1)).thenReturn(Optional.of(productoMock));

        ProductoResponseDTO response = productoService.obtenerProductoPorId(1);

        assertNotNull(response);
        assertEquals("Paracetamol", response.getNombre());
        assertEquals("Medicamentos", response.getCategoriaNombre());
    }

    @Test
    void obtenerProductoPorId_conIdInvalido_deberiaLanzarExcepcion() {
        when(productoRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> productoService.obtenerProductoPorId(999));
    }
}

