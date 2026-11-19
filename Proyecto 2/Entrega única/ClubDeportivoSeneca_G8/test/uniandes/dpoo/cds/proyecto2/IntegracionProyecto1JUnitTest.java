package uniandes.dpoo.cds.proyecto2;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** Esta prueba se vuelve activa al incorporar el módulo al proyecto Eclipse original. */
class IntegracionProyecto1JUnitTest {
    @Test void originalYPunto2CompartenPersistencia() throws Exception {
        Class<?> main;
        try { main=Class.forName("uniandes.dpoo.cds.consola.Main"); }
        catch(ClassNotFoundException e) { return; } // En modo módulo aislado no se aplica.
        Path temp=Files.createTempFile("club-proyecto2-", ".dat");
        Files.deleteIfExists(temp);
        try {
            Object original=main.getMethod("cargarOCrear",Path.class).invoke(null,temp);
            Class<?> tipo=original.getClass();
            DatosProyecto2 p2=(DatosProyecto2)tipo.getMethod("getDatosProyecto2").invoke(original);
            var torneo=p2.torneos().crear("Copa persistente","tenis","Mayores",
                GestorTorneos.Tipo.RECREATIVO,GestorTorneos.Formato.INDIVIDUAL,
                LocalDate.now().plusDays(20),4,0,0,0.12);
            main.getMethod("guardar",tipo,Path.class).invoke(null,original,temp);
            Object cargado=main.getMethod("cargarOCrear",Path.class).invoke(null,temp);
            DatosProyecto2 p2Recargado=(DatosProyecto2)tipo.getMethod("getDatosProyecto2").invoke(cargado);
            assertEquals(torneo.nombre,p2Recargado.torneos().buscar(torneo.id).nombre);
            assertNotNull(tipo.getMethod("getUsuarios").invoke(cargado));
        } finally { Files.deleteIfExists(temp); }
    }
}
