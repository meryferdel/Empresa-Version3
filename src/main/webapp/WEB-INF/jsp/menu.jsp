<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<c:set var="titulo" value="Menú principal"/>
<%@ include file="/WEB-INF/jsp/fragmentos/cabecera.jspf" %>

<p class="subtitulo">Elige una opción para consultar o modificar los datos de los empleados.</p>

<nav class="menu">
    <a class="tarjeta" href="${ctx}/app/listar">
        <span class="numero">1</span>
        <span class="titulo-tarjeta">Mostrar empleados</span>
        <span class="texto-tarjeta">Lista completa con nombre, DNI, sexo, categoría y años trabajados.</span>
    </a>
    <a class="tarjeta" href="${ctx}/app/formulario-salario">
        <span class="numero">2</span>
        <span class="titulo-tarjeta">Mostrar el salario de un empleado</span>
        <span class="texto-tarjeta">Consulta el sueldo guardado a partir del DNI.</span>
    </a>
    <a class="tarjeta" href="${ctx}/app/formulario-busqueda">
        <span class="numero">3</span>
        <span class="titulo-tarjeta">Modificar los datos de un empleado</span>
        <span class="texto-tarjeta">Busca por cualquier dato y edita el empleado. El sueldo se recalcula solo.</span>
    </a>
</nav>

<%@ include file="/WEB-INF/jsp/fragmentos/pie.jspf" %>
