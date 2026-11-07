import { Injectable } from '@angular/core';
import { jsPDF } from 'jspdf';
import autoTable from 'jspdf-autotable';

import {
  VentaResponse,
  DetalleVentaResponse,
  PagoVentaResponse
} from '../../../core/models/venta.model';

type FormatoNota = 'A4' | 'TICKET';

@Injectable({
  providedIn: 'root'
})
export class NotaVentaPdfService {

  private readonly nombreBotica = 'BOTICA VIDA & SALUD';

  private readonly colorPrincipal: [number, number, number] = [13, 148, 136];
  private readonly colorOscuro: [number, number, number] = [30, 58, 82];
  private readonly colorGris: [number, number, number] = [100, 116, 139];
  private readonly colorLinea: [number, number, number] = [220, 226, 232];

  // ============================================================
  // DESCARGAR NOTA A4
  // ============================================================

  descargarA4(venta: VentaResponse): void {

    const documento = this.generarA4(venta);

    documento.save(this.obtenerNombreArchivo(venta, 'A4'));
  }

  // ============================================================
  // DESCARGAR TICKET 80 MM
  // ============================================================

  descargarTicket(venta: VentaResponse): void {

    const documento = this.generarTicket(venta);

    documento.save(this.obtenerNombreArchivo(venta, '80mm'));
  }

  // ============================================================
  // IMPRIMIR NOTA A4
  // ============================================================

  imprimirA4(venta: VentaResponse): void {

    this.imprimirDocumento(this.generarA4(venta));
  }

  // ============================================================
  // IMPRIMIR TICKET
  // ============================================================

  imprimirTicket(venta: VentaResponse): void {

    this.imprimirDocumento(this.generarTicket(venta));
  }

  // ============================================================
  // GENERAR DOCUMENTO A4
  // ============================================================

  private generarA4(venta: VentaResponse): jsPDF {

    const doc = new jsPDF({
      orientation: 'portrait',
      unit: 'mm',
      format: 'a4'
    });

    const ancho = doc.internal.pageSize.getWidth();
    const margen = 15;
    const anchoContenido = ancho - margen * 2;

    let y = 17;

    // ----------------------------------------------------------
    // ENCABEZADO
    // ----------------------------------------------------------

    doc.setTextColor(...this.colorPrincipal);
    doc.setFont('helvetica', 'bold');
    doc.setFontSize(21);

    doc.text(this.nombreBotica, margen, y);

    y += 9;

    doc.setTextColor(...this.colorOscuro);
    doc.setFontSize(15);

    doc.text('NOTA DE VENTA', margen, y);

    doc.setFontSize(10);
    doc.setFont('helvetica', 'normal');

    doc.text(
      `N.° ${venta.numeroNotaVenta}`,
      ancho - margen,
      y,
      { align: 'right' }
    );

    y += 7;

    doc.setDrawColor(...this.colorPrincipal);
    doc.setLineWidth(0.7);

    doc.line(margen, y, ancho - margen, y);

    y += 9;

    // ----------------------------------------------------------
    // INFORMACIÓN GENERAL
    // ----------------------------------------------------------

    doc.setFontSize(9.5);
    doc.setTextColor(...this.colorGris);

    doc.text('FECHA Y HORA', margen, y);
    doc.text('CAJERO', ancho / 2, y);

    y += 5;

    doc.setFont('helvetica', 'bold');
    doc.setFontSize(10);
    doc.setTextColor(...this.colorOscuro);

    doc.text(
      this.formatearFecha(venta.fechaVenta),
      margen,
      y
    );

    doc.text(
      this.textoSeguro(venta.cajeroNombre),
      ancho / 2,
      y
    );

    y += 9;

    doc.setFont('helvetica', 'normal');
    doc.setFontSize(9.5);
    doc.setTextColor(...this.colorGris);

    doc.text('CLIENTE', margen, y);
    doc.text('DOCUMENTO', ancho / 2, y);

    y += 5;

    doc.setFont('helvetica', 'bold');
    doc.setFontSize(10);
    doc.setTextColor(...this.colorOscuro);

    doc.text(
      this.textoSeguro(venta.clienteNombre || 'Sin cliente asociado'),
      margen,
      y,
      { maxWidth: ancho / 2 - margen - 5 }
    );

    doc.text(
      this.obtenerDocumento(venta),
      ancho / 2,
      y
    );

    y += 12;

    // ----------------------------------------------------------
    // DETALLE DE PRODUCTOS
    // ----------------------------------------------------------

    doc.setFont('helvetica', 'bold');
    doc.setFontSize(11);
    doc.setTextColor(...this.colorOscuro);

    doc.text('DETALLE DE PRODUCTOS', margen, y);

    y += 3;

    const filas = (venta.detalles || []).map(
      (detalle: DetalleVentaResponse) => [

        this.textoSeguro(
          `${detalle.productoNombre}\n${detalle.presentacionTipo}`
        ),

        this.formatearNumero(detalle.cantidad),

        String(detalle.factorConversion ?? '-'),

        this.textoSeguro(detalle.numeroLote || 'N/D'),

        this.formatearMoneda(detalle.precioUnitario),

        this.formatearMoneda(detalle.subtotal)
      ]
    );

    autoTable(doc, {

      startY: y,

      margin: {
        left: margen,
        right: margen,
        bottom: 24
      },

      head: [[
        'Producto / presentación',
        'Cant.',
        'Factor',
        'Lote',
        'Precio unitario',
        'Subtotal'
      ]],

      body: filas,

      theme: 'grid',

      styles: {
        font: 'helvetica',
        fontSize: 9,
        cellPadding: 2.5,
        textColor: this.colorOscuro,
        lineColor: this.colorLinea,
        lineWidth: 0.15,
        overflow: 'linebreak',
        valign: 'middle',
        halign: 'center'
      },

      headStyles: {
        fillColor: this.colorPrincipal,
        textColor: [255, 255, 255],
        fontStyle: 'bold',
        fontSize: 8.5,
        halign: 'center'
      },

      columnStyles: {
        0: { cellWidth: 63 },
        1: { cellWidth: 14 },
        2: { cellWidth: 14 },
        3: { cellWidth: 25 },
        4: { cellWidth: 30 },
        5: { cellWidth: 28 }
      },

      // Centrar todas las celdas excepto el contenido de la primera columna en las filas de productos.
      didParseCell: data => {

        if (
          data.section === 'body' &&
          data.column.index === 0
        ) {

          data.cell.styles.halign = 'left';

        } else {

          data.cell.styles.halign = 'center';

        }

      },

      didDrawPage: () => {

        doc.setDrawColor(...this.colorLinea);

      }

    });

    y = (doc as any).lastAutoTable.finalY + 12;

    // ----------------------------------------------------------
    // RESUMEN ECONÓMICO
    // ----------------------------------------------------------

    y = this.dibujarTotalesA4(
      doc,
      venta,
      y,
      ancho - margen
    );

    y += 10;

    // ----------------------------------------------------------
    // PAGOS
    // ----------------------------------------------------------

    if (y > 245) {
      doc.addPage();
      y = 20;
    }

    doc.setFont('helvetica', 'bold');
    doc.setFontSize(11);
    doc.setTextColor(...this.colorOscuro);

    doc.text('DETALLE DE PAGOS', margen, y);

    y += 3;

    const pagos = (venta.pagos || []).map(
      (pago: PagoVentaResponse) => [

        this.obtenerNombreMetodo(pago.metodoPago),

        this.formatearMoneda(pago.monto),

        pago.metodoPago === 'EFECTIVO'
          ? this.formatearMoneda(pago.montoRecibido)
          : '—',

        pago.metodoPago === 'EFECTIVO'
          ? this.formatearMoneda(pago.vuelto)
          : '—'

      ]
    );

    if (pagos.length > 0) {

      autoTable(doc, {

        startY: y,

        margin: {
          left: margen,
          right: margen,
          bottom: 24
        },

        head: [[
          'Método',
          'Importe aplicado',
          'Recibido',
          'Vuelto'
        ]],

        body: pagos,

        theme: 'grid',

        styles: {
          font: 'helvetica',
          fontSize: 9.5,
          cellPadding: 2.5,
          textColor: this.colorOscuro,
          lineColor: this.colorLinea,
          halign: 'center',
          valign: 'middle'
        },

        headStyles: {
          fillColor: this.colorOscuro,
          textColor: [255, 255, 255],
          fontStyle: 'bold',
          halign: 'center'
        }

      });

      y = (doc as any).lastAutoTable.finalY + 8;

    } else {

      doc.setFont('helvetica', 'normal');
      doc.setFontSize(9.5);
      doc.setTextColor(...this.colorGris);

      doc.text(
        'No se registraron pagos para esta venta.',
        margen,
        y + 6
      );

      y += 15;
    }

    // ----------------------------------------------------------
    // PIE DE DOCUMENTO
    // ----------------------------------------------------------

    this.dibujarPieA4(doc);

    return doc;
  }

  // ============================================================
  // GENERAR TICKET DE 80 MM
  // ============================================================

  private generarTicket(venta: VentaResponse): jsPDF {

    const detalles = venta.detalles || [];
    const pagos = venta.pagos || [];

    // Estimar la altura para evitar que el contenido
    // quede cortado cuando existan textos largos.
    const estimarLineas = (
      texto: string,
      caracteres: number
    ): number => {

      return Math.max(
        1,
        Math.ceil(texto.length / caracteres)
      );
    };

    // Espacio adicional para los datos generales que
    // puedan ocupar más de una línea.
    const datosGenerales = [
      this.formatearFecha(venta.fechaVenta),
      this.textoSeguro(venta.cajeroNombre),
      this.textoSeguro(venta.clienteNombre || 'Sin cliente'),
      this.obtenerDocumento(venta)
    ];

    const alturaExtraInformacion = datosGenerales.reduce(
      (total, texto) => {

        return total +
          Math.max(0, estimarLineas(texto, 30) - 1) * 4.5;

      },
      0
    );

    // Calcular la altura de cada producto considerando
    // el nombre y el número de lote.
    const alturaProductos = detalles.reduce(
      (total, detalle) => {

        const nombre = this.textoSeguro(
          detalle.productoNombre
        );

        const lote = this.textoSeguro(
          detalle.numeroLote || 'N/D'
        );

        const lineasNombre = estimarLineas(nombre, 38);
        const lineasLote = estimarLineas(lote, 28);

        return total +
          38 +
          Math.max(0, lineasNombre - 1) * 4.2 +
          Math.max(0, lineasLote - 1) * 4.5;

      },
      0
    );

    // Altura de los pagos según el método utilizado.
    const alturaPagos = pagos.reduce(
      (total, pago) => {

        return total +
          (pago.metodoPago === 'EFECTIVO' ? 23 : 13);

      },
      pagos.length === 0 ? 6 : 0
    );

    const alturaDocumento = Math.max(
      180,
      155 +
        alturaExtraInformacion +
        alturaProductos +
        alturaPagos
    );

    const doc = new jsPDF({
      orientation: 'portrait',
      unit: 'mm',
      format: [80, alturaDocumento]
    });

    const centro = 40;
    const margen = 5;
    const ancho = 70;

    let y = 10;

    // ----------------------------------------------------------
    // ENCABEZADO
    // ----------------------------------------------------------

    doc.setTextColor(...this.colorPrincipal);
    doc.setFont('helvetica', 'bold');
    doc.setFontSize(14);

    doc.text(this.nombreBotica, centro, y, {
      align: 'center',
      maxWidth: ancho
    });

    y += 9;

    doc.setTextColor(...this.colorOscuro);
    doc.setFontSize(12);

    doc.text('NOTA DE VENTA', centro, y, {
      align: 'center'
    });

    y += 7;

    this.lineaTicket(doc, y);

    y += 6;

    // ----------------------------------------------------------
    // INFORMACIÓN DE LA VENTA
    // ----------------------------------------------------------

    y = this.textoTicket(
      doc,
      'Número:',
      String(venta.numeroNotaVenta || '-'),
      y
    );

    y = this.textoTicket(
      doc,
      'Fecha:',
      this.formatearFecha(venta.fechaVenta),
      y
    );

    y = this.textoTicket(
      doc,
      'Cajero:',
      this.textoSeguro(venta.cajeroNombre),
      y
    );

    y = this.textoTicket(
      doc,
      'Cliente:',
      this.textoSeguro(venta.clienteNombre || 'Sin cliente'),
      y
    );

    y = this.textoTicket(
      doc,
      'Documento:',
      this.obtenerDocumento(venta),
      y
    );

    y += 1;

    this.lineaTicket(doc, y);

    y += 6;

    // ----------------------------------------------------------
    // PRODUCTOS
    // ----------------------------------------------------------

    doc.setFont('helvetica', 'bold');
    doc.setFontSize(10);
    doc.setTextColor(...this.colorOscuro);

    doc.text('PRODUCTOS', centro, y, {
      align: 'center'
    });

    y += 7;

    for (const detalle of detalles) {

      // NOMBRE DEL PRODUCTO
      const nombre = doc.splitTextToSize(
        this.textoSeguro(detalle.productoNombre),
        ancho
      );

      doc.setFont('helvetica', 'bold');
      doc.setFontSize(9.5);
      doc.setTextColor(...this.colorOscuro);

      doc.text(nombre, margen, y);

      y += nombre.length * 4.2 + 1;

      // PRESENTACIÓN Y CANTIDAD
      doc.setFont('helvetica', 'normal');
      doc.setFontSize(8.8);
      doc.setTextColor(...this.colorOscuro);

      doc.text(
        `Presentación: ${this.textoSeguro(detalle.presentacionTipo)}`,
        margen,
        y
      );

      doc.setFont('helvetica', 'bold');

      doc.text(
        `Cant.: ${this.formatearNumero(detalle.cantidad)}`,
        margen + ancho,
        y,
        { align: 'right' }
      );

      y += 5;

      // FACTOR Y CANTIDAD BASE
      doc.setFont('helvetica', 'normal');
      doc.setFontSize(8.5);

      doc.text(
        `Factor: ${detalle.factorConversion ?? '-'} | Base: ${this.formatearNumero(detalle.cantidadBase)}`,
        margen,
        y
      );

      y += 4.5;

      // NÚMERO DE LOTE
      y = this.textoTicket(
        doc,
        'Lote:',
        this.textoSeguro(detalle.numeroLote || 'N/D'),
        y
      );

      // PRECIO UNITARIO
      this.filaTicket(
        doc,
        'Precio unitario',
        this.formatearMoneda(detalle.precioUnitario),
        y
      );

      y += 5;

      // IMPORTE TOTAL DE LA LÍNEA
      // Se resalta para distinguirlo del precio unitario.
      doc.setFillColor(236, 253, 250);

      doc.roundedRect(
        margen,
        y - 3.5,
        ancho,
        7,
        1.2,
        1.2,
        'F'
      );

      this.filaTicket(
        doc,
        'Importe línea',
        this.formatearMoneda(detalle.subtotal),
        y,
        true
      );

      y += 7;

      this.lineaTicket(doc, y);

      y += 5;
    }

    // ----------------------------------------------------------
    // TOTALES
    // ----------------------------------------------------------

    this.filaTicket(
      doc,
      'Subtotal',
      this.formatearMoneda(venta.subtotal),
      y
    );

    y += 6;

    this.filaTicket(
      doc,
      'Descuento',
      this.formatearMoneda(venta.descuentoTotal),
      y
    );

    y += 7;

    doc.setFillColor(236, 253, 250);

    doc.roundedRect(
      margen,
      y - 4,
      ancho,
      9,
      1.5,
      1.5,
      'F'
    );

    this.filaTicket(
      doc,
      'TOTAL',
      this.formatearMoneda(venta.total),
      y,
      true
    );

    y += 10;

    this.lineaTicket(doc, y);

    y += 6;

    // ----------------------------------------------------------
    // PAGOS
    // ----------------------------------------------------------

    doc.setFont('helvetica', 'bold');
    doc.setFontSize(10);
    doc.setTextColor(...this.colorOscuro);

    doc.text('PAGOS', centro, y, {
      align: 'center'
    });

    y += 7;

    if (pagos.length === 0) {

      doc.setFont('helvetica', 'normal');
      doc.setFontSize(9);
      doc.setTextColor(...this.colorGris);

      doc.text(
        'No se registraron pagos.',
        margen,
        y
      );

      y += 6;

    } else {

      for (const pago of pagos) {

        doc.setFont('helvetica', 'bold');
        doc.setFontSize(9.5);
        doc.setTextColor(...this.colorOscuro);

        doc.text(
          this.obtenerNombreMetodo(pago.metodoPago),
          margen,
          y
        );

        y += 5;

        this.filaTicket(
          doc,
          'Importe aplicado',
          this.formatearMoneda(pago.monto),
          y
        );

        y += 5;

        if (pago.metodoPago === 'EFECTIVO') {

          this.filaTicket(
            doc,
            'Recibido',
            this.formatearMoneda(pago.montoRecibido),
            y
          );

          y += 5;

          this.filaTicket(
            doc,
            'Vuelto',
            this.formatearMoneda(pago.vuelto),
            y
          );

          y += 5;
        }

        y += 3;
      }
    }

    // ----------------------------------------------------------
    // PIE DEL TICKET
    // ----------------------------------------------------------

    this.lineaTicket(doc, y);

    y += 7;

    doc.setFont('helvetica', 'bold');
    doc.setFontSize(9);
    doc.setTextColor(...this.colorOscuro);

    doc.text(
      'Gracias por su preferencia',
      centro,
      y,
      { align: 'center' }
    );

    y += 6;

    doc.setFont('helvetica', 'normal');
    doc.setFontSize(7.5);
    doc.setTextColor(...this.colorGris);

    doc.text(
      'Documento interno de venta.',
      centro,
      y,
      { align: 'center' }
    );

    y += 4;

    doc.text(
      'No es un comprobante electrónico SUNAT.',
      centro,
      y,
      {
        align: 'center',
        maxWidth: ancho
      }
    );

    return doc;
  }

  // ============================================================
  // TOTALES A4
  // ============================================================

  private dibujarTotalesA4(
    doc: jsPDF,
    venta: VentaResponse,
    y: number,
    derecha: number
  ): number {

    const izquierda = derecha - 75;

    doc.setDrawColor(...this.colorLinea);
    doc.line(izquierda, y, derecha, y);

    y += 7;

    doc.setFont('helvetica', 'normal');
    doc.setFontSize(10);
    doc.setTextColor(...this.colorGris);

    doc.text('Subtotal', izquierda, y);

    doc.setTextColor(...this.colorOscuro);

    doc.text(
      this.formatearMoneda(venta.subtotal),
      derecha,
      y,
      { align: 'right' }
    );

    y += 7;

    doc.setTextColor(...this.colorGris);
    doc.text('Descuento', izquierda, y);

    doc.setTextColor(...this.colorOscuro);

    doc.text(
      this.formatearMoneda(venta.descuentoTotal),
      derecha,
      y,
      { align: 'right' }
    );

    y += 8;

    doc.setFillColor(236, 253, 250);
    doc.roundedRect(
      izquierda - 3,
      y - 5,
      78,
      13,
      2,
      2,
      'F'
    );

    doc.setFont('helvetica', 'bold');
    doc.setFontSize(12);
    doc.setTextColor(...this.colorPrincipal);

    doc.text('TOTAL', izquierda, y + 3);

    doc.text(
      this.formatearMoneda(venta.total),
      derecha,
      y + 3,
      { align: 'right' }
    );

    return y + 13;
  }

  // ============================================================
  // PIE DE PÁGINA A4
  // ============================================================

  private dibujarPieA4(doc: jsPDF): void {

    const totalPaginas = doc.getNumberOfPages();

    for (let pagina = 1; pagina <= totalPaginas; pagina++) {

      doc.setPage(pagina);

      const ancho = doc.internal.pageSize.getWidth();
      const alto = doc.internal.pageSize.getHeight();

      doc.setDrawColor(...this.colorLinea);

      doc.line(
        15,
        alto - 17,
        ancho - 15,
        alto - 17
      );

      doc.setFont('helvetica', 'normal');
      doc.setFontSize(8);
      doc.setTextColor(...this.colorGris);

      doc.text(
        'Documento interno de venta. No es un comprobante electrónico SUNAT.',
        15,
        alto - 11
      );

      doc.text(
        `Página ${pagina} de ${totalPaginas}`,
        ancho - 15,
        alto - 11,
        { align: 'right' }
      );
    }
  }

  // ============================================================
  // UTILIDADES DE TICKET
  // ============================================================

  private textoTicket(
    doc: jsPDF,
    etiqueta: string,
    valor: string,
    y: number
  ): number {

    doc.setFont('helvetica', 'bold');
    doc.setFontSize(8.5);
    doc.setTextColor(...this.colorOscuro);

    doc.text(etiqueta, 5, y);

    doc.setFont('helvetica', 'normal');

    const lineas = doc.splitTextToSize(
      valor,
      48
    );

    doc.text(lineas, 27, y);

    // Devuelve la siguiente posición vertical, considerando si el contenido ocupó varias líneas.
    return y + Math.max(5, lineas.length * 4.2);
  }

  private filaTicket(
    doc: jsPDF,
    etiqueta: string,
    valor: string,
    y: number,
    destacado: boolean = false
  ): void {

    doc.setFont(
      'helvetica',
      destacado ? 'bold' : 'normal'
    );

    doc.setFontSize(destacado ? 9.5 : 9);

    doc.setTextColor(
      ...(destacado ? this.colorPrincipal : this.colorGris)
    );

    doc.text(etiqueta, 5, y);

    doc.setFont('helvetica', 'bold');

    doc.setFontSize(destacado ? 9.5 : 9);

    doc.setTextColor(
      ...(destacado ? this.colorPrincipal : this.colorOscuro)
    );

    doc.text(valor, 75, y, {
      align: 'right'
    });
  }

  private lineaTicket(
    doc: jsPDF,
    y: number
  ): void {

    doc.setDrawColor(...this.colorLinea);
    doc.setLineWidth(0.25);

    doc.line(5, y, 75, y);
  }

  // ============================================================
  // IMPRESIÓN
  // ============================================================

  private imprimirDocumento(doc: jsPDF): void {

    const url = doc.output('bloburl');

    const ventana = window.open(url, '_blank');

    if (!ventana) {

      window.setTimeout(() => {
        URL.revokeObjectURL(url.href);
      }, 60_000);

      throw new Error(
        'El navegador bloqueó la ventana de impresión. Permite las ventanas emergentes e inténtalo nuevamente.'
      );
    }
  }

  // ============================================================
  // UTILIDADES GENERALES
  // ============================================================

  private obtenerNombreArchivo(
    venta: VentaResponse,
    formato: string
  ): string {

    const numero = String(
      venta.numeroNotaVenta || venta.id
    ).replace(/[^a-zA-Z0-9_-]/g, '_');

    return `Nota_Venta_${numero}_${formato}.pdf`;
  }

  private obtenerDocumento(venta: VentaResponse): string {

    if (!venta.clienteNumeroDocumento) {
      return 'No registrado';
    }

    return [
      venta.clienteTipoDocumento || '',
      venta.clienteNumeroDocumento
    ].filter(Boolean).join(' ');
  }

  private obtenerNombreMetodo(metodo: string): string {

    const nombres: Record<string, string> = {

      EFECTIVO: 'Efectivo',
      YAPE: 'Yape',
      PLIN: 'Plin',
      TARJETA: 'Tarjeta',
      TRANSFERENCIA: 'Transferencia'

    };

    return nombres[metodo] || metodo;
  }

  private formatearFecha(fecha: string): string {

    const valor = new Date(fecha);

    if (Number.isNaN(valor.getTime())) {
      return fecha || 'No disponible';
    }

    return new Intl.DateTimeFormat('es-PE', {
      dateStyle: 'short',
      timeStyle: 'short'
    }).format(valor);
  }

  private formatearMoneda(valor: number | null | undefined): string {

    const numero = Number(valor ?? 0);

    return `S/ ${numero.toFixed(2)}`;
  }

  private formatearNumero(valor: number | null | undefined): string {

    const numero = Number(valor ?? 0);

    return new Intl.NumberFormat('es-PE', {
      maximumFractionDigits: 2
    }).format(numero);
  }

  private textoSeguro(valor: string | null | undefined): string {

    return String(valor ?? '').trim() || 'No disponible';
  }

}


