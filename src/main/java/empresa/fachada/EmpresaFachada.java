package empresa.fachada;

import empresa.modelo.CalculoNomina;
import empresa.modelo.Empleado;
import empresa.modelo.EmpleadoDAO;
import empresa.modelo.FiltroBusqueda;
import java.sql.SQLException;
import java.util.List;

/**
 * Patrón Facade (fachada): una única "ventanilla" para todo lo que el
 * controlador necesita del modelo. Con solo 4 métodos, el controlador no tiene
 * que saber nada del DAO, de la conexión a la base de datos ni de cómo se
 * calcula el sueldo.
 *
 * @author Meryb
 * @version 3.0
 */
public class EmpresaFachada {

    /** Acceso a la base de datos (el controlador no lo ve). */
    private final EmpleadoDAO dao = new EmpleadoDAO();

    /**
     * Devuelve todos los empleados, ordenados por nombre.
     *
     * @return lista de empleados
     * @throws SQLException si falla la base de datos
     */
    public List<Empleado> listar() throws SQLException {
        return dao.listar();
    }

    /**
     * Busca un empleado por su DNI exacto.
     *
     * @param dni DNI del empleado
     * @return el empleado, o null si no existe
     * @throws SQLException si falla la base de datos
     */
    public Empleado buscarPorDni(String dni) throws SQLException {
        return dao.buscarPorDni(dni);
    }

    /**
     * Busca empleados que cumplan los criterios del filtro.
     *
     * @param filtro criterios de búsqueda
     * @return empleados encontrados
     * @throws SQLException si falla la base de datos
     */
    public List<Empleado> buscar(FiltroBusqueda filtro) throws SQLException {
        return dao.buscar(filtro);
    }

    /**
     * Guarda un empleado. Antes calcula su sueldo con la categoría y los años
     * trabajados (el sueldo nunca lo escribe el usuario).
     *
     * @param empleado empleado con los datos nuevos
     * @return true si se guardó; false si no existe ningún empleado con ese DNI
     * @throws SQLException si falla la base de datos
     */
    public boolean modificar(Empleado empleado) throws SQLException {
        int sueldo = CalculoNomina.calcular(empleado.getCategoria(), empleado.getAnyos());
        empleado.setSueldo(sueldo);
        return dao.actualizar(empleado);
    }
}
