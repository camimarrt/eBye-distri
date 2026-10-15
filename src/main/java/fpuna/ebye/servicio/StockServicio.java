package fpuna.ebye.servicio;

import java.sql.SQLException;
import java.util.List;

import fpuna.ebye.bd.ProductoDAO;
import fpuna.ebye.entidad.Producto;

public class StockServicio {

    private ProductoDAO productoDAO = new ProductoDAO();;

    public Producto reservarStock(Integer idProducto, Integer cantidad) {
    return this.productoDAO.consultarYReservar(idProducto, cantidad);
}

    // Devuelve el producto con ese id, o null si no existe.
    public Producto getProducto(int id) throws SQLException {
        List<Producto> lista = productoDAO.seleccionarPorId(id);

        if (lista.isEmpty()) {
            return null;
        }

        return lista.get(0);
    }
}
