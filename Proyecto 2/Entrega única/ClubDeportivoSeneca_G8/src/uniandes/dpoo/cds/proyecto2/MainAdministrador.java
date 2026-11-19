package uniandes.dpoo.cds.proyecto2;

/** Punto de entrada del perfil Administrador. Ejecutar como Java Application. */
public final class MainAdministrador {
    private MainAdministrador(){}
    public static void main(String[] args){
        AplicacionConsola.iniciar(DatosProyecto2.Perfil.ADMINISTRADOR);
    }
}
