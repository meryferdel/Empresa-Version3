package empresa.modelo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso a los datos de los empleados y sus nóminas en la base de datos
 * mediante JDBC (tablas <b>Empleados</b>(dni, nombre, sexo, categoria, anyos) y
 * <b>Nominas</b>(dni, sueldo)).
 * Todas las consultas usan sentencias con parámetros, de modo que lo que
 * escribe el usuario nunca se pega dentro del texto SQL.
 *
 * @author Meryb
 * @version 3.0
 */
public class EmpleadoDAO {

    /** Consulta base: cada empleado con su sueldo (si lo tiene). */
    private static final String CONSULTA_BASE
            = "SELECT e.dni, e.nombre, e.sexo, e.categoria, e.anyos, n.sueldo "
            + "FROM Empleados e LEFT JOIN Nominas n ON n.dni = e.dni";

    /** Orden de todos los listados. */
    private static final String ORDEN = " ORDER BY e.nombre, e.dni";

    /**
     * Crea el objeto de acceso a datos (lo crea empresa.fachada.EmpresaFachada).
     */
    public EmpleadoDAO() {
    }

    /**
     * Crea un Empleado con la fila actual de un resultado.
     *
     * @param rs resultado de la consulta base
     * @return empleado creado
     * @throws SQLException si falla la lectura de alguna columna
     */
    private Empleado crearEmpleado(ResultSet rs) throws SQLException {
        int sueldo = rs.getInt("sueldo");
        Integer sueldoGuardado = rs.wasNull() ? null : sueldo;
        return new Empleado(rs.getString("dni"), rs.getString("nombre"),
                rs.getString("sexo"), rs.getInt("categoria"), rs.getInt("anyos"),
                sueldoGuardado);
    }

    /**
     * Devuelve todos los empleados, ordenados por nombre.
     *
     * @return lista de empleados (vacía si no hay ninguno)
     * @throws SQLException si falla la consulta
     */
    public List<Empleado> listar() throws SQLException {
        List<Empleado> empleados = new ArrayList<>();
        try (Connection conexion = ConexionBD.getInstancia().getConexion();
                Statement sentencia = conexion.createStatement();
                ResultSet rs = sentencia.executeQuery(CONSULTA_BASE + ORDEN)) {
            while (rs.next()) {
                empleados.add(crearEmpleado(rs));
            }
        }
        return empleados;
    }

    /**
     * Busca un empleado por su DNI exacto.
     *
     * @param dni DNI del empleado
     * @return el empleado, o null si no existe
     * @throws SQLException si falla la consulta
     */
    public Empleado buscarPorDni(String dni) throws SQLException {
        try (Connection conexion = ConexionBD.getInstancia().getConexion();
                PreparedStatement ps = conexion.prepareStatement(CONSULTA_BASE + " WHERE e.dni = ?")) {
            ps.setString(1, dni);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? crearEmpleado(rs) : null;
            }
        }
    }

    /**
     * Busca empleados que cumplan todos los criterios rellenados del filtro.
     * El DNI y el nombre buscan texto contenido (sin distinguir mayúsculas);
     * el resto de campos exigen el valor exacto.
     *
     * @param filtro criterios de búsqueda
     * @return empleados encontrados, ordenados por nombre
     * @throws SQLException si falla la consulta
     */
    public List<Empleado> buscar(FiltroBusqueda filtro) throws SQLException {
        StringBuilder sql = new StringBuilder(CONSULTA_BASE).append(" WHERE 1 = 1"); // WHERE 1 = 1 es un truco. 1 = 1 siempre es verdadero, así que no filtra nada. Sirve para que después cada condición se pueda añadir con AND ... sin preocuparse de cuál va primero.
        List<Object> parametros = new ArrayList<>();
        if (filtro.getDni() != null) {
            sql.append(" AND e.dni LIKE ?");
            parametros.add("%" + filtro.getDni() + "%");
        }
        if (filtro.getNombre() != null) {
            sql.append(" AND e.nombre LIKE ?");
            parametros.add("%" + filtro.getNombre() + "%");
        }
        if (filtro.getSexo() != null) {
            sql.append(" AND e.sexo = ?");
            parametros.add(filtro.getSexo());
        }
        if (filtro.getCategoria() != null) {
            sql.append(" AND e.categoria = ?");
            parametros.add(filtro.getCategoria());
        }
        if (filtro.getAnyos() != null) {
            sql.append(" AND e.anyos = ?");
            parametros.add(filtro.getAnyos());
        }
        if (filtro.getSueldo() != null) {
            sql.append(" AND n.sueldo = ?");
            parametros.add(filtro.getSueldo());
        }
        sql.append(ORDEN);

        List<Empleado> empleados = new ArrayList<>();
        try (Connection conexion = ConexionBD.getInstancia().getConexion();
                PreparedStatement ps = conexion.prepareStatement(sql.toString())) {
            for (int i = 0; i < parametros.size(); i++) { // Recorre la lista y rellena los huecos en orden: el primer valor en el primer ?, el segundo en el segundo, y así.
                ps.setObject(i + 1, parametros.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    empleados.add(crearEmpleado(rs));
                }
            }
        }
        return empleados;
    }

    /**
     * Guarda los datos de un empleado (el DNI identifica al empleado y no se
     * cambia) y su sueldo en la tabla Nominas, en la misma transacción, de
     * modo que los datos y el sueldo nunca quedan descuadrados. El sueldo no
     * se calcula aquí: viene ya calculado por la capa de fachada.
     *
     * @param empleado empleado con los datos nuevos y su sueldo calculado
     * @return true si se modificó; false si no existe
     *         ningún empleado con ese DNI
     * @throws SQLException             si falla la actualización
     * @throws IllegalArgumentException si el empleado no trae sueldo
     */
    public boolean actualizar(Empleado empleado) throws SQLException {
        if (empleado.getSueldo() == null) {
            throw new IllegalArgumentException("El empleado debe traer el sueldo calculado");
        }
        int sueldo = empleado.getSueldo();
        String sqlEmpleado = "UPDATE Empleados SET nombre = ?, sexo = ?, categoria = ?, "
                + "anyos = ? WHERE dni = ?";
        String sqlNomina = "INSERT INTO Nominas (dni, sueldo) VALUES (?, ?) "
                + "ON DUPLICATE KEY UPDATE sueldo = VALUES(sueldo)";
        try (Connection conexion = ConexionBD.getInstancia().getConexion()) {
            conexion.setAutoCommit(false); // Se desactiva el guardado automático para que se guarden a la vez y al finalizar tanto la actualización del empleado como la nómina.
            try (PreparedStatement psEmpleado = conexion.prepareStatement(sqlEmpleado);
                    PreparedStatement psNomina = conexion.prepareStatement(sqlNomina)) {
                psEmpleado.setString(1, empleado.getNombre());
                psEmpleado.setString(2, empleado.getSexo());
                psEmpleado.setInt(3, empleado.getCategoria());
                psEmpleado.setInt(4, empleado.getAnyos());
                psEmpleado.setString(5, empleado.getDni());
                
                if (psEmpleado.executeUpdate() == 0) {
                    conexion.rollback(); // Se deshacen todos los cambios realizados y el método devuelve false.
                    return false;
                }
                
                psNomina.setString(1, empleado.getDni());
                psNomina.setInt(2, sueldo);
                psNomina.executeUpdate();
                
                conexion.commit(); // Como todo ha salido bien, se guarda el empleado y se guarda la nómina.
                
                return true;
            } catch (SQLException ex) {
                conexion.rollback(); // Si falla cualquier consulta: se deshacen todos los cambios (rollback) y se vuelve a lanzar la excepción.
                throw ex;
            }
        }
    }
}
