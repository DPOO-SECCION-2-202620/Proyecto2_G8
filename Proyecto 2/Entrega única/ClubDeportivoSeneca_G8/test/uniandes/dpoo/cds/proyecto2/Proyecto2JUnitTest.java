package uniandes.dpoo.cds.proyecto2;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.time.*;
import java.util.*;

/** Trazabilidad: pruebas para cada criterio de tres HU oficiales y nuevas reglas de torneos. */
class Proyecto2JUnitTest {
    @Test void huAdm01TrasladoCorrectoYRechazos() throws Exception {
        HistoriasOficiales h=new HistoriasOficiales();
        h.crearTienda("A");h.crearTienda("B");h.agregarProducto("A","P1");
        h.trasladarProducto("A","B","P1"); // CA-1
        assertFalse(h.tieneProducto("A","P1"));assertTrue(h.tieneProducto("B","P1"));
        Set<String> a=h.productosDe("A"), b=h.productosDe("B");
        assertThrows(HistoriasOficiales.ReglaException.class,()->h.trasladarProducto("A","B","P2")); // CA-2
        assertEquals(a,h.productosDe("A"));assertEquals(b,h.productosDe("B"));
        h.agregarProducto("A","P3");h.agregarProducto("B","P3");
        Set<String> previoA=h.productosDe("A"), previoB=h.productosDe("B");
        assertThrows(HistoriasOficiales.ReglaException.class,()->h.trasladarProducto("A","B","P3")); // CA-3
        assertEquals(previoA,h.productosDe("A"));assertEquals(previoB,h.productosDe("B"));
    }
    @Test void huAdm03SnackConAlergenosYRechazos() throws Exception {
        HistoriasOficiales h=new HistoriasOficiales();
        h.proponerSnack("empleado","s1","galleta",Set.of("GLUTEN"));
        h.aprobarSnack("s1"); // CA-1
        assertTrue(h.estaEnCatalogo("s1"));
        h.proponerSnack("empleado","s2","fruta",Set.of());
        assertThrows(HistoriasOficiales.ReglaException.class,()->h.aprobarSnack("s2")); // CA-2
        assertFalse(h.estaEnCatalogo("s2"));
        h.proponerSnack("empleado","s3","dulce",Set.of("NINGUNO"));
        h.rechazarSnack("s3"); // CA-3
        assertEquals(HistoriasOficiales.Sugerencia.Estado.RECHAZADA,h.obtenerSugerencia("s3").getEstado());
        assertFalse(h.estaEnCatalogo("s3"));
    }
    @Test void huSoc01ReservaYRechazos() throws Exception {
        HistoriasOficiales h=new HistoriasOficiales();
        LocalDate fecha=LocalDate.of(2030,4,1); LocalTime inicio=LocalTime.of(10,0),fin=LocalTime.of(11,0);
        h.registrarDisciplina("tenis",1);
        h.registrarInstalacion("T1","tenis",2);h.registrarInstalacion("T2","tenis",2);
        assertNotNull(h.reservar("socio1","T1",fecha,inicio,fin,2,"SENCILLOS")); // CA-1
        assertThrows(HistoriasOficiales.ReglaException.class,()->h.reservar("socio2","T2",fecha,inicio,fin,2,"SENCILLOS")); // CA-2
        h.registrarDisciplina("padel",2);h.registrarInstalacion("P1","padel",4);
        assertThrows(HistoriasOficiales.ReglaException.class,()->h.reservar("socio1","P1",fecha,inicio,fin,2,"SENCILLOS")); // CA-3
    }
    private static GestorTorneos.Participante participante(String id,GestorTorneos.Rol rol,boolean preferencial,Set<LocalDate> turnos) throws Exception {
        return new GestorTorneos.Participante(id,id,"Mayores",rol,preferencial,turnos,null);
    }
    @Test void torneosCuposPreferencialesYRetiro() throws Exception {
        GestorTorneos g=new GestorTorneos();
        var t=g.crear("Copa","tenis","Mayores",GestorTorneos.Tipo.RECREATIVO,GestorTorneos.Formato.INDIVIDUAL,
                LocalDate.of(2030,2,1),6,0,0,0.10);
        assertEquals(2,t.getReservados()); // ceil(20% de 6)
        for(int i=0;i<4;i++)g.inscribir(t.id,"REG"+i,List.of(participante("p"+i,GestorTorneos.Rol.EXTERNO,false,null)));
        assertThrows(GestorTorneos.ReglaException.class,()->g.inscribir(t.id,"REG4",List.of(participante("p4",GestorTorneos.Rol.EXTERNO,false,null))));
        g.acreditarRanking(t.id,"r1",1);
        g.inscribir(t.id,"PREF",List.of(participante("r1",GestorTorneos.Rol.SOCIO,true,null)));
        g.retirar(t.id,"PREF");assertEquals(4,t.getOcupados());
        g.liberarPreferenciales(t.id);
        g.inscribir(t.id,"REG4",List.of(participante("p4",GestorTorneos.Rol.EXTERNO,false,null)));
        assertEquals(5,t.getOcupados());
    }
    @Test void empleadosGratisSinPremioYTurnoImpide() throws Exception {
        GestorTorneos g=new GestorTorneos();LocalDate fecha=LocalDate.of(2030,1,3);
        var t=g.crear("Open","tenis","Mayores",GestorTorneos.Tipo.COMPETITIVO,GestorTorneos.Formato.INDIVIDUAL,
                fecha,4,100,0.60,0);
        assertThrows(GestorTorneos.ReglaException.class,()->g.inscribir(t.id,"ocupado",
                List.of(participante("e1",GestorTorneos.Rol.ENTRENADOR,false,Set.of(fecha)))));
        var empleado=g.inscribir(t.id,"libre",List.of(participante("e2",GestorTorneos.Rol.ENTRENADOR,false,null)));
        assertEquals(0,empleado.valorInscripcion);
        g.inscribir(t.id,"otro",List.of(participante("s1",GestorTorneos.Rol.SOCIO,false,null)));
        assertEquals(100,t.getRecaudado());g.iniciar(t.id);g.registrarEncuentro(t.id,"libre","otro",1,0);
        g.finalizar(t.id,"libre");assertEquals(0,t.getPremioPagado());
        assertEquals(GestorTorneos.Estado.FINALIZADO,t.getEstado());assertEquals(1,t.getEncuentros().size());
    }
    @Test void bonoNoAcumulableYDeUnSoloUso() throws Exception {
        GestorTorneos g=new GestorTorneos();
        var t=g.crear("Copa","tenis","Mayores",GestorTorneos.Tipo.RECREATIVO,GestorTorneos.Formato.INDIVIDUAL,
                LocalDate.of(2030,5,1),4,0,0,0.20);
        g.inscribir(t.id,"ana",List.of(participante("ana",GestorTorneos.Rol.SOCIO,false,null)));
        g.inscribir(t.id,"bob",List.of(participante("bob",GestorTorneos.Rol.SOCIO,false,null)));
        g.iniciar(t.id);g.finalizar(t.id,"ana");
        var bono=g.consultarBonos("ana").get(0);
        assertThrows(GestorTorneos.ReglaException.class,()->g.redimirBono("ana",bono.codigo,100,true));
        assertFalse(bono.estaUsado());assertEquals(80,g.redimirBono("ana",bono.codigo,100,false));
        assertThrows(GestorTorneos.ReglaException.class,()->g.redimirBono("ana",bono.codigo,100,false));
    }

    @Test void rankingDebeEstarAcreditadoPorAdministrador() throws Exception {
        GestorTorneos g = new GestorTorneos();
        var t = g.crear("Copa","tenis","Mayores",GestorTorneos.Tipo.RECREATIVO,
            GestorTorneos.Formato.INDIVIDUAL,LocalDate.now().plusDays(50),5,0,0,0.15);
        assertThrows(GestorTorneos.ReglaException.class,()->g.inscribir(t.id,"socio1",
            List.of(participante("socio1",GestorTorneos.Rol.SOCIO,true,null))));
        g.acreditarRanking(t.id,"socio1",1);
        assertTrue(g.tienePreferencia(t.id,"socio1"));
        g.inscribir(t.id,"socio1",List.of(participante("socio1",GestorTorneos.Rol.SOCIO,true,null)));
        assertEquals(1,t.getPreferencialesOcupados());
    }
    @Test void cuposSeLiberanAutomaticamenteAlCierreDeRanking() throws Exception {
        GestorTorneos g = new GestorTorneos();
        var t = g.crear("Copa","tenis","Mayores",GestorTorneos.Tipo.RECREATIVO,
            GestorTorneos.Formato.INDIVIDUAL,LocalDate.now().plusDays(1),5,0,0,0.15);
        assertTrue(torneosLiberados(g,t.id));
        for(int n=0;n<5;n++)g.inscribir(t.id,"r"+n,
            List.of(participante("r"+n,GestorTorneos.Rol.EXTERNO,false,null)));
        assertEquals(5,t.getOcupados());
    }
    private static boolean torneosLiberados(GestorTorneos g,String id) throws Exception {
        g.consultar();return g.buscar(id).estanLiberados();
    }
    @Test void doblesMixtosSoloPaganParticipantesNoEmpleados() throws Exception {
        GestorTorneos g = new GestorTorneos();
        var t = g.crear("Dobles","tenis","Mayores",GestorTorneos.Tipo.COMPETITIVO,
            GestorTorneos.Formato.DOBLES,LocalDate.now().plusDays(10),4,100,0.50,0);
        var doble = g.inscribir(t.id,"equipoA",List.of(
            participante("empleado",GestorTorneos.Rol.ENTRENADOR,false,null),
            participante("socio",GestorTorneos.Rol.SOCIO,false,null)));
        assertEquals(50.0,doble.valorInscripcion);
        g.inscribir(t.id,"equipoB",List.of(
            participante("e1",GestorTorneos.Rol.EXTERNO,false,null),
            participante("e2",GestorTorneos.Rol.EXTERNO,false,null)));
        assertEquals(150.0,t.getRecaudado());
        g.iniciar(t.id);g.finalizar(t.id,"equipoA");
        assertEquals(37.50,t.getPremioPagado());
    }
    @Test void compraTiendaUsaBonoEnLaCompraYSePersiste() throws Exception {
        DatosProyecto2 datos = DatosProyecto2.nuevoDemo();
        GestorTorneos g=datos.torneos();
        HistoriasOficiales h=datos.historias();
        var t=g.buscar("TOR-1");
        g.liberarPreferenciales(t.id);
        g.inscribir(t.id,"socio",List.of(participante("socio",GestorTorneos.Rol.SOCIO,false,null)));
        g.inscribir(t.id,"otro",List.of(participante("otro",GestorTorneos.Rol.SOCIO,false,null)));
        g.iniciar(t.id);g.finalizar(t.id,"socio");
        String bono=g.consultarBonos("socio").get(0).codigo;
        assertThrows(GestorTorneos.ReglaException.class,()->h.comprar("socio","tienda-central",
            Map.of("pelota",1),bono,true,g));
        assertFalse(g.consultarBonos("socio").get(0).estaUsado());
        HistoriasOficiales.Compra compra=h.comprar("socio","tienda-central",Map.of("pelota",2),bono,false,g);
        assertEquals(70000.0,compra.subtotal);
        assertEquals(59500.0,compra.total);
        assertEquals(1,h.getCompras().size());
        assertThrows(GestorTorneos.ReglaException.class,()->h.comprar("socio","tienda-central",Map.of("pelota",1),bono,false,g));
        java.nio.file.Path archivo=java.nio.file.Files.createTempFile("cds2",".ser");
        try{
            datos.guardar(archivo);
            DatosProyecto2 cargado=DatosProyecto2.cargar(archivo);
            assertEquals(1,cargado.historias().getCompras().size());
            assertTrue(cargado.torneos().consultarBonos("socio").get(0).estaUsado());
        }finally{java.nio.file.Files.deleteIfExists(archivo);}
    }
    @Test void visitantesSeRegistranSinPoderAtribuirseRolAdmin() throws Exception {
        DatosProyecto2 datos=DatosProyecto2.nuevoDemo();
        datos.registrarVisitante("nuevo","clave123456",GestorTorneos.Rol.EXTERNO,"Mayores");
        assertNotNull(datos.autenticar("nuevo","clave123456",DatosProyecto2.Perfil.VISITANTE));
        assertThrows(IllegalArgumentException.class,()->datos.registrarVisitante(
            "otro","clave123456",GestorTorneos.Rol.EQUIPO,"Mayores"));
        assertThrows(IllegalArgumentException.class,()->datos.agregarCuenta(
            "admin","nueva1234",DatosProyecto2.Perfil.ADMINISTRADOR,GestorTorneos.Rol.SOCIO,"Mayores"));
    }
    @Test void snackAprobadoPuedeComprarseEnCafeteria() throws Exception {
        HistoriasOficiales h=new HistoriasOficiales();
        h.proponerSnack("empleado","GAL","Galleta",Set.of("GLUTEN"),7500);
        assertThrows(HistoriasOficiales.ReglaException.class,()->h.comprar("socio","cafeteria",
            Map.of("GAL",2),null,false,new GestorTorneos()));
        h.aprobarSnack("GAL");
        var compra=h.comprar("socio","cafeteria",Map.of("GAL",2),null,false,new GestorTorneos());
        assertEquals(15000.0,compra.total);
    }

    @Test void huTor01ConfiguracionTiposYCostos() throws Exception {
        GestorTorneos g=new GestorTorneos();
        LocalDate fecha=LocalDate.now().plusDays(60);
        assertThrows(GestorTorneos.ReglaException.class,()->g.crear("Inválido","futbol","Sub-15",
            GestorTorneos.Tipo.RECREATIVO,GestorTorneos.Formato.EQUIPOS,fecha,4,10,0,0.10));
        assertThrows(GestorTorneos.ReglaException.class,()->g.crear("Inválido","futbol","Sub-15",
            GestorTorneos.Tipo.COMPETITIVO,GestorTorneos.Formato.EQUIPOS,fecha,4,0,0.50,0));
        var recreativo=g.crear("Amistoso","futbol","Sub-15",GestorTorneos.Tipo.RECREATIVO,
            GestorTorneos.Formato.EQUIPOS,fecha,4,0,0,0.15);
        var competitivo=g.crear("Liga","baloncesto","Mayores",GestorTorneos.Tipo.COMPETITIVO,
            GestorTorneos.Formato.EQUIPOS,fecha,6,100,0.50,0);
        assertEquals(GestorTorneos.Estado.INSCRIPCIONES,recreativo.getEstado());
        assertEquals(GestorTorneos.Estado.INSCRIPCIONES,competitivo.getEstado());
        assertEquals(2,g.consultar().size());
    }
    @Test void torneosDeConjuntoSoloAceptanEquiposYMultiplesTorneos() throws Exception {
        GestorTorneos g=new GestorTorneos();
        LocalDate fecha=LocalDate.now().plusDays(50);
        var t=g.crear("Copa futbol","futbol","Mayores",GestorTorneos.Tipo.RECREATIVO,
            GestorTorneos.Formato.EQUIPOS,fecha,4,0,0,0.10);
        assertThrows(GestorTorneos.ReglaException.class,()->g.inscribir(t.id,"soc1",
            List.of(participante("soc1",GestorTorneos.Rol.SOCIO,false,null))));
        var equipo=new GestorTorneos.Participante("equipoA","Equipo A","Mayores",
            GestorTorneos.Rol.EQUIPO,false,null,List.of("s1","s2"));
        g.inscribir(t.id,"equipoA",List.of(equipo));
        var segundo=g.crear("Segunda copa","futbol","Mayores",GestorTorneos.Tipo.RECREATIVO,
            GestorTorneos.Formato.EQUIPOS,fecha,4,0,0,0.10);
        g.inscribir(segundo.id,"equipoA",List.of(equipo));
        assertEquals(1,t.getOcupados());
        assertEquals(1,segundo.getOcupados());
    }
    @Test void huTor02RetiroYBloqueoDespuesDeIniciar() throws Exception {
        GestorTorneos g=new GestorTorneos();
        var t=g.crear("Copa","tenis","Mayores",GestorTorneos.Tipo.COMPETITIVO,
            GestorTorneos.Formato.INDIVIDUAL,LocalDate.now().plusDays(30),6,200,0.5,0);
        g.inscribir(t.id,"primera",List.of(participante("primera",GestorTorneos.Rol.SOCIO,false,null)));
        assertEquals(200.0,t.getRecaudado());
        g.retirar(t.id,"primera");
        assertEquals(0.0,t.getRecaudado());
        g.inscribir(t.id,"p1",List.of(participante("p1",GestorTorneos.Rol.SOCIO,false,null)));
        g.inscribir(t.id,"p2",List.of(participante("p2",GestorTorneos.Rol.SOCIO,false,null)));
        g.iniciar(t.id);
        assertThrows(GestorTorneos.ReglaException.class,()->g.retirar(t.id,"p1"));
        assertThrows(GestorTorneos.ReglaException.class,()->g.inscribir(t.id,"p3",
            List.of(participante("p3",GestorTorneos.Rol.SOCIO,false,null))));
        assertEquals(2,t.getOcupados());
    }
    @Test void huTor05HistorialPosicionesDisponibleDespuesDeFinalizar() throws Exception {
        GestorTorneos g=new GestorTorneos();
        var t=g.crear("Copa","tenis","Mayores",GestorTorneos.Tipo.RECREATIVO,
            GestorTorneos.Formato.INDIVIDUAL,LocalDate.now().plusDays(30),4,0,0,0.10);
        g.inscribir(t.id,"ana",List.of(participante("ana",GestorTorneos.Rol.SOCIO,false,null)));
        g.inscribir(t.id,"bea",List.of(participante("bea",GestorTorneos.Rol.SOCIO,false,null)));
        g.iniciar(t.id);
        g.registrarEncuentro(t.id,"ana","bea",3,1);
        assertEquals(Integer.valueOf(3),t.getPosiciones().get("ana"));
        g.finalizar(t.id,"ana");
        assertEquals(1,g.buscar(t.id).getEncuentros().size());
        assertEquals(GestorTorneos.Estado.FINALIZADO,g.buscar(t.id).getEstado());
        assertThrows(GestorTorneos.ReglaException.class,()->g.finalizar(t.id,"bea"));
    }
    @Test void huTor06EquipoGanadorRecibeBonosPorSocio() throws Exception {
        GestorTorneos g=new GestorTorneos();
        var t=g.crear("Final","futbol","Mayores",GestorTorneos.Tipo.RECREATIVO,
            GestorTorneos.Formato.EQUIPOS,LocalDate.now().plusDays(30),4,0,0,0.20);
        var e1=new GestorTorneos.Participante("A","Equipo A","Mayores",GestorTorneos.Rol.EQUIPO,
            false,null,List.of("ana","bea","carlos"));
        var e2=new GestorTorneos.Participante("B","Equipo B","Mayores",GestorTorneos.Rol.EQUIPO,
            false,null,List.of("diana"));
        g.inscribir(t.id,"A",List.of(e1));
        g.inscribir(t.id,"B",List.of(e2));
        g.iniciar(t.id);g.finalizar(t.id,"A");
        assertEquals(1,g.consultarBonos("ana").size());
        assertEquals(1,g.consultarBonos("bea").size());
        assertEquals(1,g.consultarBonos("carlos").size());
        assertEquals(0,g.consultarBonos("diana").size());
    }
}
