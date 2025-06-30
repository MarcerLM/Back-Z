package com.ipn.mx.inventario.services.impl;

import com.ipn.mx.inventario.domain.entidades.Movimiento;
import com.ipn.mx.inventario.domain.entidades.OrdenCompra;
import com.ipn.mx.inventario.domain.entidades.Producto;
import com.ipn.mx.inventario.domain.entidades.Proveedor;
import com.ipn.mx.inventario.domain.repositorios.OrdenCompraRepository;
import com.ipn.mx.inventario.domain.repositorios.ProductoRepository;
import com.ipn.mx.inventario.domain.repositorios.ProveedorRepository;
import com.ipn.mx.inventario.dto.OrdenCompraDTO;
import com.ipn.mx.inventario.services.OrdenCompraService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrdenCompraServiceImpl implements OrdenCompraService {

    @Autowired
    private OrdenCompraRepository ordenCompraRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private ProveedorRepository proveedorRepository;

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;


    public OrdenCompra crearOrdenCompra(OrdenCompraDTO ordenCompraDTO) {
        Optional<Proveedor> proveedor = proveedorRepository.findById(ordenCompraDTO.getIdProveedor());
        Optional<Producto> producto = productoRepository.findById(ordenCompraDTO.getIdProducto());

        if (proveedor.isEmpty() || producto.isEmpty()) {
            throw new RuntimeException("Proveedor o producto no encontrado");
        }

        Producto productoExistente = productoRepository.findById(ordenCompraDTO.getIdProducto())
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        double precioUnitario = ordenCompraDTO.getTotal()/ordenCompraDTO.getCantidad();

        productoExistente.setStock(productoExistente.getStock() + ordenCompraDTO.getCantidad());

        OrdenCompra ordenCompra = OrdenCompra.builder()
                .proveedor(proveedor.get())
                .producto(producto.get())
                .cantidad(ordenCompraDTO.getCantidad())
                .precioUnitario(precioUnitario)
                .total(ordenCompraDTO.getTotal())
                .fechaEntrega(ordenCompraDTO.getFechaEntrega())
                .build();

        OrdenCompra ordenGuardada = ordenCompraRepository.save(ordenCompra);

        String proveedorEmail = proveedor.get().getEmail(); // Ajusta si tu entidad Proveedor tiene otro nombre de campo
        enviarCorreoProveedor(proveedorEmail, ordenGuardada);

        return ordenGuardada;
    }

    @Override
    public List<OrdenCompra> listarTodas() {
        return (List<OrdenCompra>) ordenCompraRepository.findAll();
    }

    private void enviarCorreoProveedor(String to, OrdenCompra orden) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject("Nueva Orden de Compra");
        message.setText("Se ha creado una nueva orden de compra con ID: " + orden.getIdCompra() +
                "\nProducto: " + orden.getProducto().getNombre() +
                "\nCantidad: " + orden.getCantidad() +
                "\nTotal: $" + orden.getTotal() +
                "\nFecha de entrega: " + orden.getFechaEntrega());
        System.out.println("Enviando correo a: " + to);
        mailSender.send(message);
    }

}
