package fpuna.ebye.entidad;

public class Producto {

	private Integer id;
	private String nombre;
	private Integer stock;
	private Long precio;

	public Producto() {
	}

	public Producto(Integer id, String nombre, Integer stock, Long precio) {
		this.id = id;
		this.nombre = nombre;
		this.stock = stock;
		this.precio = precio;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getNombre() {
		return nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	public Integer getStock() {
		return stock;
	}

	public void setStock(Integer stock) {
		this.stock = stock;
	}

	public Long getPrecio() {
		return precio;
	}

	public void setPrecio(Long precio) {
		this.precio = precio;
	}

}
