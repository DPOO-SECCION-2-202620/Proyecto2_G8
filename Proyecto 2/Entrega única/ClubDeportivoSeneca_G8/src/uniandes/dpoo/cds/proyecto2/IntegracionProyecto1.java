package uniandes.dpoo.cds.proyecto2;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Puente de persistencia con Proyecto 1. Si el código original comparte el mismo
 * proyecto Eclipse, ambos programas usan el MISMO SistemaClub y el MISMO archivo
 * datos/sistema-seneca.dat. En ejecución aislada usa datos/proyecto2.ser.
 *
 * Reflection permite que el módulo nuevo también se compile y se pruebe solo.
 */
public final class IntegracionProyecto1 {
    private static final Path ARCHIVO_ORIGINAL = Path.of("datos", "sistema-seneca.dat");
    private static final Path ARCHIVO_INDEPENDIENTE = Path.of("datos", "proyecto2.ser");
    private final Object sistemaOriginal;
    private final DatosProyecto2 datos;

    private IntegracionProyecto1(Object sistemaOriginal, DatosProyecto2 datos) {
        this.sistemaOriginal=sistemaOriginal;
        this.datos=datos;
    }
    public DatosProyecto2 datos() { return datos; }
    public boolean estaIntegrado() { return sistemaOriginal!=null; }

    public static IntegracionProyecto1 abrir() throws Exception {
        final Class<?> main;
        try {
            main=Class.forName("uniandes.dpoo.cds.consola.Main");
        } catch(ClassNotFoundException e) {
            return new IntegracionProyecto1(null, DatosProyecto2.cargar(ARCHIVO_INDEPENDIENTE));
        }
        try {
            Object sistema=main.getMethod("cargarOCrear",Path.class).invoke(null, ARCHIVO_ORIGINAL);
            if(!Files.exists(ARCHIVO_ORIGINAL)&&Files.exists(ARCHIVO_INDEPENDIENTE)) {
                // Migracion de una ejecucion aislada anterior del modulo nuevo.
                DatosProyecto2 anterior=DatosProyecto2.cargar(ARCHIVO_INDEPENDIENTE);
                sistema.getClass().getMethod("setDatosProyecto2",DatosProyecto2.class).invoke(sistema,anterior);
            }
            DatosProyecto2 estado=(DatosProyecto2)sistema.getClass().getMethod("getDatosProyecto2").invoke(sistema);
            if(estado==null)throw new IllegalStateException("El proyecto original no tiene estado del Proyecto 2");
            return new IntegracionProyecto1(sistema,estado);
        } catch(InvocationTargetException e) {
            Throwable causa=e.getCause();
            throw new IllegalStateException("No se pudo cargar el sistema original; comprueba los datos y la configuracion",causa);
        }
    }
    public void guardar() throws Exception {
        if(sistemaOriginal==null) {
            datos.guardar(ARCHIVO_INDEPENDIENTE);
        } else {
            Class<?> main=Class.forName("uniandes.dpoo.cds.consola.Main");
            main.getMethod("guardar",sistemaOriginal.getClass(),Path.class)
                .invoke(null,sistemaOriginal,ARCHIVO_ORIGINAL);
        }
    }
}
