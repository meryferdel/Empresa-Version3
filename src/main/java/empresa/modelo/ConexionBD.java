package empresa.modelo;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * <b>Patrón Singleton:</b> de esta clase solo puede existir UNA instancia en
 * toda la aplicación.
 * <p>
 * Cómo se consigue:
 * <ol>
 * <li>El constructor es <b>privado</b>: nadie puede hacer <code>new ConexionBD()</code>.</li>
 * <li>La única instancia se guarda en un atributo <b>estático</b> (<code>instancia</code>).</li>
 * <li>Para usarla se llama a {@link #getInstancia()}, que la crea la primera
 * vez y después devuelve siempre la misma.</li>
 * </ol>
 * Lo que se comparte es la configuración de la base de datos (se lee una sola
 * vez de <code>bd.properties</code>). Cada llamada a {@link #getConexion()}
 * abre una conexión nueva.
 *
 * @author Meryb
 * @version 3.0
 */
public class ConexionBD {

    /** La única instancia de la clase. Vale null hasta que alguien la pide. */
    private static ConexionBD instancia;

    /** Datos de conexión (url, usuario, clave) leídos de bd.properties. */
    private final Properties propiedades = new Properties();

    /**
     * Constructor PRIVADO: lee bd.properties y carga el driver de MariaDB.
     *
     * @throws SQLException si falta el fichero o el driver
     */
    private ConexionBD() throws SQLException {
        try (InputStream entrada = ConexionBD.class.getResourceAsStream("/bd.properties")) {
            if (entrada == null) {
                throw new SQLException("No se encuentra el fichero bd.properties");
            }
            propiedades.load(entrada);
            Class.forName("org.mariadb.jdbc.Driver");
        } catch (IOException | ClassNotFoundException ex) {
            throw new SQLException("No se puede preparar la conexión", ex);
        }
    }

    /**
     * Devuelve la única instancia; si todavía no existe, la crea.
     * synchronized evita que dos peticiones simultáneas creen
     * dos instancias a la vez. Imagina dos peticiones que llegan a la vez cuando todavía 
     * no existe la instancia. 
     * Sin synchronized, las dos verían null y crearían dos objetos, y eso rompería el Singleton. 
     * Con synchronized, la segunda espera, y cuando entra ya ve la instancia creada.
     *
     * @return la instancia única
     * @throws SQLException si no se puede crear
     */
    public static synchronized ConexionBD getInstancia() throws SQLException { 
        if (instancia == null) { // Por si todavía no existe ninguna instanciam que se cree.
            instancia = new ConexionBD();
        }
        return instancia;
    }

    /**
     * Abre una conexión nueva con la base de datos (quien la pide debe
     * cerrarla).
     *
     * @return conexión abierta
     * @throws SQLException si no se puede conectar
     */
    public Connection getConexion() throws SQLException {
        return DriverManager.getConnection(propiedades.getProperty("url"),
                propiedades.getProperty("usuario"), propiedades.getProperty("clave", ""));
    }
}
