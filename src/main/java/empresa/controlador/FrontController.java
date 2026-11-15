package empresa.controlador;

import empresa.fachada.EmpresaFachada;
import empresa.modelo.Empleado;
import empresa.modelo.FiltroBusqueda;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * <b>Patrón Front Controller (la C de MVC):</b> único servlet de la
 * aplicación. Todas las peticiones "/app/*" entran por aquí.
 * Según la ruta, llama al método que corresponde, que:
 * <ol>
 * <li>lee los parámetros de la petición,</li>
 * <li>pide los datos a la fachada EmpresaFachada,</li>
 * <li>deja los datos en la petición con setAttribute y</li>
 * <li>reenvía a la JSP (la V de MVC) que los muestra.</li>
 * </ol>
 * El controlador solo habla con la fachada: no sabe nada del DAO ni del
 * cálculo del sueldo.
 *
 * @author Meryb
 * @version 3.0
 */
@WebServlet(name = "FrontController", urlPatterns = {"/app/*"}) // Todo lo que empiece por /app/* entrará en el servlet
public class FrontController extends HttpServlet {

    /** Identificador de versión para la serialización. */
    private static final long serialVersionUID = 1L;

    /** Carpeta de las JSP (dentro de WEB-INF, así el navegador no las pide directamente). */
    private static final String CARPETA_JSP = "/WEB-INF/jsp/";

    /** Longitud máxima del nombre (la de la columna de la base de datos). */
    private static final int LONGITUD_MAXIMA_NOMBRE = 100;

    /** Única puerta de entrada al modelo (Facade). */
    private transient EmpresaFachada fachada; // Con transient lo que le estamos diciendo es que ese atributo no es necesario guardarlo, simplemente es como un mando a distancia que habla con el modelo o datos.

    /**
     * Crea el controlador (lo instancia Tomcat).
     */
    public FrontController() {
    }

    /**
     * Se ejecuta una vez al arrancar el servlet: crea la fachada.
     */
    @Override
    public void init() {
        fachada = new EmpresaFachada();
    }

    // ---------------------------------------------------------------
    // Entrada de las peticiones: elige el método según la ruta
    // ---------------------------------------------------------------

    /**
     * Atiende las peticiones GET. La ruta es lo que viene detrás de
     * <code>/app</code> (por ejemplo, <code>/listar</code>).
     *
     * @param peticion  petición recibida
     * @param respuesta respuesta que se enviará
     * @throws ServletException si falla el reenvío a la JSP
     * @throws IOException      si falla la salida
     */
    @Override
    protected void doGet(HttpServletRequest peticion, HttpServletResponse respuesta)
            throws ServletException, IOException {
        String ruta = peticion.getPathInfo();
        if (ruta == null) {
            ruta = "/";
        }
        try {
            switch (ruta) {
                case "/":
                    respuesta.sendRedirect(peticion.getContextPath() + "/app/inicio"); // peticion.getContextPath() devuelve el nombre de mi proyecto (en este caso Empresa)
                    break;
                case "/inicio":
                    mostrar(peticion, respuesta, "menu");
                    break;
                case "/listar":
                    listar(peticion, respuesta);
                    break;
                case "/formulario-salario":
                    mostrar(peticion, respuesta, "formularioSalario");
                    break;
                case "/salario":
                    salario(peticion, respuesta);
                    break;
                case "/formulario-busqueda":
                    mostrar(peticion, respuesta, "formularioBusqueda");
                    break;
                case "/buscar":
                    buscar(peticion, respuesta);
                    break;
                case "/modificar":
                    editar(peticion, respuesta);
                    break;
                default:
                    paginaNoEncontrada(peticion, respuesta);
            }
        } catch (SQLException ex) {
            errorBaseDatos(peticion, respuesta, ex);
        }
    }

    /**
     * Atiende las peticiones POST (solo se usa para guardar un empleado).
     *
     * @param peticion  petición recibida
     * @param respuesta respuesta que se enviará
     * @throws ServletException si falla el reenvío a la JSP
     * @throws IOException      si falla la salida
     */
    @Override
    protected void doPost(HttpServletRequest peticion, HttpServletResponse respuesta)
            throws ServletException, IOException {
        // Para leer bien las tildes y eñes que llegan del formulario
        peticion.setCharacterEncoding("UTF-8");
        String ruta = peticion.getPathInfo();
        try {
            if ("/modificar".equals(ruta)) {
                guardar(peticion, respuesta);
            } else {
                paginaNoEncontrada(peticion, respuesta);
            }
        } catch (SQLException ex) {
            errorBaseDatos(peticion, respuesta, ex);
        }
    }

    // ---------------------------------------------------------------
    // Una operación por método
    // ---------------------------------------------------------------

    /**
     * Opción 1 del menú: muestra todos los empleados.
     */
    private void listar(HttpServletRequest peticion, HttpServletResponse respuesta)
            throws ServletException, IOException, SQLException {
        peticion.setAttribute("empleados", fachada.listar());
        mostrar(peticion, respuesta, "listado");
    }

    /**
     * Opción 2 del menú: muestra el salario del empleado cuyo DNI llega del
     * formulario. Si el DNI está vacío, vuelve al formulario con un error.
     */
    private void salario(HttpServletRequest peticion, HttpServletResponse respuesta)
            throws ServletException, IOException, SQLException {
        String dni = texto(peticion.getParameter("dni"));
        if (dni == null) {
            peticion.setAttribute("error", "Escribe el DNI del empleado.");
            mostrar(peticion, respuesta, "formularioSalario");
            return;
        }
        dni = dni.toUpperCase();
        peticion.setAttribute("dniBuscado", dni);
        peticion.setAttribute("empleado", fachada.buscarPorDni(dni));
        mostrar(peticion, respuesta, "salario");
    }

    /**
     * Opción 3 del menú, paso 1: busca empleados por los criterios del
     * formulario. Si algún dato no es válido, vuelve al formulario con la
     * lista de errores.
     */
    private void buscar(HttpServletRequest peticion, HttpServletResponse respuesta)
            throws ServletException, IOException, SQLException {
        List<String> errores = new ArrayList<>();
        FiltroBusqueda filtro = new FiltroBusqueda();
        filtro.setDni(texto(peticion.getParameter("dni")));
        filtro.setNombre(texto(peticion.getParameter("nombre")));

        String sexo = texto(peticion.getParameter("sexo"));
        if (sexo != null && !sexo.equals("M") && !sexo.equals("F")) {
            errores.add("El sexo debe ser M o F.");
        } else {
            filtro.setSexo(sexo);
        }

        Integer categoria = leerEntero(peticion, "categoria", "Categoría", errores);
        if (categoria != null && (categoria < 1 || categoria > 10)) {
            errores.add("La categoría debe estar entre 1 y 10.");
        } else {
            filtro.setCategoria(categoria);
        }

        filtro.setAnyos(leerEntero(peticion, "anyos", "Años trabajados", errores));
        filtro.setSueldo(leerEntero(peticion, "sueldo", "Sueldo", errores));

        if (!errores.isEmpty()) {
            peticion.setAttribute("errores", errores);
            mostrar(peticion, respuesta, "formularioBusqueda"); // Si hay errores, vuelve al formulario para que filtre el usuario de nuevo.
            return;
        }
        peticion.setAttribute("empleados", fachada.buscar(filtro));
        peticion.setAttribute("sinCriterios", filtro.estaVacio());
        mostrar(peticion, respuesta, "resultadosBusqueda");
    }

    /**
     * Opción 3 del menú, paso 2 (GET): muestra el formulario de edición con
     * los datos actuales del empleado.
     */
    private void editar(HttpServletRequest peticion, HttpServletResponse respuesta)
            throws ServletException, IOException, SQLException {
        String dni = texto(peticion.getParameter("dni"));
        if (dni == null) {
            mostrarError(peticion, respuesta, HttpServletResponse.SC_BAD_REQUEST, // Error 400 por los datos enviados.
                    "Falta el DNI", "No se ha indicado qué empleado se quiere modificar.");
            return;
        }
        Empleado empleado = fachada.buscarPorDni(dni);
        if (empleado == null) {
            empleadoNoEncontrado(peticion, respuesta, dni);
            return;
        }
        peticion.setAttribute("empleado", empleado);
        mostrar(peticion, respuesta, "editar");
    }

    /**
     * Opción 3 del menú, paso 3 (POST): valida los datos del formulario y los
     * guarda. El DNI no se puede cambiar y el sueldo no se pide: la fachada lo
     * recalcula a partir de la categoría y los años trabajados.
     */
    private void guardar(HttpServletRequest peticion, HttpServletResponse respuesta)
            throws ServletException, IOException, SQLException {
        String dni = texto(peticion.getParameter("dni"));
        if (dni == null) {
            mostrarError(peticion, respuesta, HttpServletResponse.SC_BAD_REQUEST, // Error 400 por los datos enviados.
                    "Falta el DNI", "No se ha indicado qué empleado se quiere modificar.");
            return;
        }
        Empleado original = fachada.buscarPorDni(dni);
        if (original == null) {
            empleadoNoEncontrado(peticion, respuesta, dni);
            return;
        }

        // Se parte de los datos guardados y se cambia cada campo que sea válido
        List<String> errores = new ArrayList<>();
        Empleado editado = new Empleado(original.getDni(), original.getNombre(),
                original.getSexo(), original.getCategoria(), original.getAnyos(),
                original.getSueldo());

        String nombre = texto(peticion.getParameter("nombre"));
        if (nombre == null) {
            errores.add("El nombre no puede estar vacío.");
        } else if (nombre.length() > LONGITUD_MAXIMA_NOMBRE) {
            errores.add("El nombre no puede tener más de " + LONGITUD_MAXIMA_NOMBRE
                    + " caracteres.");
        } else {
            editado.setNombre(nombre);
        }

        String sexo = texto(peticion.getParameter("sexo"));
        if ("M".equals(sexo) || "F".equals(sexo)) {
            editado.setSexo(sexo);
        } else {
            errores.add("El sexo debe ser M o F.");
        }

        int erroresAntes = errores.size();
        Integer categoria = leerEntero(peticion, "categoria", "Categoría", errores);
        if (categoria == null) {
            if (errores.size() == erroresAntes) { // Si es null porque el usuario ha dejado categoría vacía y no se ha detectado ningún error nuevo, salta el mensaje.
                errores.add("La categoría es obligatoria.");
            }
        } else if (categoria < 1 || categoria > 10) {
            errores.add("La categoría debe estar entre 1 y 10.");
        } else {
            editado.setCategoria(categoria);
        }

        erroresAntes = errores.size();
        Integer anyos = leerEntero(peticion, "anyos", "Años trabajados", errores);
        if (anyos == null) {
            if (errores.size() == erroresAntes) {
                errores.add("Los años trabajados son obligatorios.");
            }
        } else {
            editado.setAnyos(anyos);
        }

        peticion.setAttribute("empleado", editado);
        if (!errores.isEmpty()) {
            peticion.setAttribute("errores", errores);
            mostrar(peticion, respuesta, "editar"); // Vuelve a editar para intentarlo de nuevo.
            return;
        }
        if (!fachada.modificar(editado)) {
            empleadoNoEncontrado(peticion, respuesta, dni);
            return;
        }
        peticion.setAttribute("mensajeOk", "Los datos se han guardado correctamente. "
                + "El sueldo se ha recalculado automáticamente.");
        mostrar(peticion, respuesta, "editar");
    }

    // ---------------------------------------------------------------
    // Utilidades: mostrar páginas y mostrar errores
    // ---------------------------------------------------------------

    /**
     * Reenvía a una JSP (el navegador no se entera y la URL no cambia).
     *
     * @param vista nombre de la JSP sin ruta ni extensión (por ejemplo "listado")
     */
    private void mostrar(HttpServletRequest peticion, HttpServletResponse respuesta,
            String vista) throws ServletException, IOException {
        peticion.getRequestDispatcher(CARPETA_JSP + vista + ".jsp") // getRequestDispatcher(ruta) te da un objeto que sabe llevar la petición a otro sitio (hacia la ruta).
                .forward(peticion, respuesta); // Es como un mensajero que lleva la petición a la hoja de respuesta a la ruta JSP que sea.
    }

    /**
     * Muestra la página de error con un código HTTP, un título y un mensaje.
     */
    private void mostrarError(HttpServletRequest peticion, HttpServletResponse respuesta,
            int estado, String titulo, String mensaje)
            throws ServletException, IOException {
        peticion.setAttribute("tituloError", titulo);
        peticion.setAttribute("mensajeError", mensaje);
        respuesta.setStatus(estado);
        mostrar(peticion, respuesta, "error");
    }

    /**
     * Error 404: la dirección no existe en la aplicación.
     */
    private void paginaNoEncontrada(HttpServletRequest peticion,
            HttpServletResponse respuesta) throws ServletException, IOException {
        mostrarError(peticion, respuesta, HttpServletResponse.SC_NOT_FOUND,
                "Página no encontrada",
                "La dirección que has escrito no existe en esta aplicación.");
    }

    /**
     * Error 404: no hay ningún empleado con ese DNI.
     */
    private void empleadoNoEncontrado(HttpServletRequest peticion,
            HttpServletResponse respuesta, String dni)
            throws ServletException, IOException {
        mostrarError(peticion, respuesta, HttpServletResponse.SC_NOT_FOUND,
                "Empleado no encontrado",
                "No existe ningún empleado con el DNI " + dni + ".");
    }

    /**
     * Error 500: falló el acceso a la base de datos.
     */
    private void errorBaseDatos(HttpServletRequest peticion, HttpServletResponse respuesta,
            SQLException ex) throws ServletException, IOException {
        log("Error de acceso a la base de datos", ex);
        mostrarError(peticion, respuesta, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Error de base de datos",
                "No se ha podido acceder a la base de datos. Comprueba que MariaDB está "
                + "arrancado y que los datos del fichero bd.properties son correctos.");
    }

    // ---------------------------------------------------------------
    // Utilidades para leer parámetros
    // ---------------------------------------------------------------

    /**
     * Quita los espacios de los extremos de un texto.
     *
     * @param valor texto recibido (puede ser null)
     * @return el texto sin espacios, o null si era nulo o estaba vacío
     */
    private static String texto(String valor) {
        if (valor == null) {
            return null;
        }
        String limpio = valor.trim();
        return limpio.isEmpty() ? null : limpio;
    }

    /**
     * Lee un parámetro que debe ser un número entero no negativo. Si no es
     * válido, anota el error en la lista.
     *
     * @param peticion  petición actual
     * @param parametro nombre del parámetro del formulario
     * @param etiqueta  nombre del campo que se usará en el mensaje de error
     * @param errores   lista donde se anotan los errores
     * @return el número, o null si el campo está vacío o no es válido
     */
    private static Integer leerEntero(HttpServletRequest peticion, String parametro,
            String etiqueta, List<String> errores) {
        String valor = texto(peticion.getParameter(parametro));
        if (valor == null) {
            return null;
        }
        try {
            int numero = Integer.parseInt(valor);
            if (numero < 0) {
                errores.add(etiqueta + ": no puede ser negativo.");
                return null;
            }
            return numero;
        } catch (NumberFormatException ex) {
            errores.add(etiqueta + ": debe ser un número entero.");
            return null;
        }
    }
}
