package fpuna.ebye.bd;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import fpuna.ebye.entidad.Producto;

public class ProductoDAO {

    // Devuelve una lista con el producto buscado. Si el id no existe, la lista queda vacia.
    public List<Producto> seleccionarPorId(int id) throws SQLException {
        String query = "SELECT id, nombre, stock, precio FROM producto WHERE id = ?;";
        List<Producto> lista = new ArrayList<>();

        Connection conn = null;

        try {
            conn = Bd.connect();
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setInt(1, id);

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Producto p = new Producto();
                p.setId(rs.getInt(1));
                p.setNombre(rs.getString(2));
                p.setStock(rs.getInt(3));
                p.setPrecio(rs.getLong(4));

                lista.add(p);
            }

            return lista;
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    System.err.println(
                            "No se pudo cerrar la conexión a BD: "
                                    + e.getMessage());
                }
            }
        }
    }
    ///////////////////////////////////////////////
    public Producto consultarYReservar(Integer idProducto, Integer cantidadAReservar) {
    String sqlUpdate = "UPDATE producto SET cantidad = cantidad - ? WHERE id_producto = ? AND cantidad >= ?";
    String sqlSelect = "SELECT id_producto, nombre, precio, cantidad FROM producto WHERE id_producto = ?";
    
    Connection conn = null;
    PreparedStatement psUpdate = null;
    PreparedStatement psSelect = null;
    ResultSet rs = null;
    Producto producto = null;

    try {
        conn = Bd.connect();
        conn.setAutoCommit(false); // Iniciar transacción para asegurar atomicidad

        // 1. Descontar stock condicionado a que haya suficiente cantidad
        psUpdate = conn.prepareStatement(sqlUpdate);
        psUpdate.setInt(1, cantidadAReservar);
        psUpdate.setInt(2, idProducto);
        psUpdate.setInt(3, cantidadAReservar);

        int filasAfectadas = psUpdate.executeUpdate();

        if (filasAfectadas > 0) {
            // 2. Obtener datos actualizados del producto (nombre, precio, nuevo stock)
            psSelect = conn.prepareStatement(sqlSelect);
            psSelect.setInt(1, idProducto);
            rs = psSelect.executeQuery();

            if (rs.next()) {
                producto = new Producto();
                producto.setId(rs.getInt("id_producto"));
                producto.setNombre(rs.getString("nombre"));
                producto.setPrecio(rs.getLong("precio"));
                producto.setStock(rs.getInt("cantidad"));
            }
            conn.commit();
        } else {
            conn.rollback(); // No había stock suficiente o no existe el id
        }
    } catch (SQLException e) {
        if (conn != null) {
            try {
                conn.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
        e.printStackTrace();
    } finally {
        try {
            if (rs != null) rs.close();
            if (psUpdate != null) psUpdate.close();
            if (psSelect != null) psSelect.close();
            if (conn != null) {
                conn.setAutoCommit(true);
                conn.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    return producto;
}

}
