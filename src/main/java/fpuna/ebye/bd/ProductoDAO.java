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

}
