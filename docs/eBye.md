# Sistema 1: Tienda Multi-comercio "eBye"

Integrantes: Camila Martinez, Letizia Tullo, Franco Espinola

## Servicios

### 1. Consultar existencia de productos

Devuelve el stock disponible de un producto. Lo consulta PedidosMasTarde.

**Transporte:** UDP (consulta rápida; se tolera la pérdida de un datagrama a cambio de baja latencia). Puerto por defecto: `6000`.

#### Entrada

`tipo`, `requestId`, `idproducto`

```json
{
  "tipo": "GET_STOCK",
  "requestId": "req-1",
  "idproducto": 305
}
```

#### Salida

`ok`, `requestId`, `stock_disponible`, `timestamp`

```json
{
  "ok": true,
  "requestId": "req-1",
  "stock_disponible": 15,
  "timestamp": "2026-10-10T15:30:00-03:00"
}
```

#### Errores

`ok`, `requestId`, `codigo`, `mensaje`

```json
{
  "ok": false,
  "requestId": "req-1",
  "codigo": "PRODUCTO_NO_ENCONTRADO",
  "mensaje": "No existe el producto 999"
}
```

| codigo | cuándo ocurre |
| --- | --- |
| `PRODUCTO_NO_ENCONTRADO` | el `idproducto` no existe en la tabla `producto` |
| `CAMPO_REQUERIDO` | falta `tipo`, `requestId` o `idproducto` (o `idproducto` no es un número) |
| `TIPO_NO_SOPORTADO` | el `tipo` no es `GET_STOCK` |
| `JSON_INVALIDO` | el datagrama no es un JSON válido |
| `ERROR_BD` | falló la consulta a PostgreSQL |

## Servicios pendientes (según la documentación del grupo)

- Servicio 2: Consultar datos del cliente (TCP)
- Servicio 3: Consultar pedido (TCP)
