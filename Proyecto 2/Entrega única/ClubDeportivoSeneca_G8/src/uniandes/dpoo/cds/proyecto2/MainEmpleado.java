package uniandes.dpoo.cds.proyecto2;

/** Punto de entrada del perfil Empleado. Ejecutar como Java Application. */
public final class MainEmpleado {
    private MainEmpleado(){}
    public static void main(String[] args){
        AplicacionConsola.iniciar(DatosProyecto2.Perfil.EMPLEADO);
    }
}
