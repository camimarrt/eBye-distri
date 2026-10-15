package fpuna.ebye.entidad;

public class Pedido {
    private Integer id;
    private Integer id_cliente;
    private String nombre_destinatario;
    private Double monto_total;
    private String estado;
    private String direccion;

    public Pedido(Integer id, Integer id_cliente, String nombre_destinatario, Double monto_total, String estado, String direccion) {
        this.id = id;
        this.id_cliente = id_cliente;
        this.nombre_destinatario = nombre_destinatario;
        this.monto_total = monto_total;
        this.estado = estado;
        this.direccion = direccion;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getIdCliente() {
        return id_cliente;
    }

    public void setIdCliente(Integer id_cliente) {
        this.id_cliente = id_cliente;
    }

    public String getNombreDestinatario() {
        return nombre_destinatario;
    }

    public void setNombreDestinatario(String nombre_destinatario) {
        this.nombre_destinatario = nombre_destinatario;
    }

    public Double getMontoTotal() {
        return monto_total;
    }

    public void setMontoTotal(Double monto_total) {
        this.monto_total = monto_total;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }
}