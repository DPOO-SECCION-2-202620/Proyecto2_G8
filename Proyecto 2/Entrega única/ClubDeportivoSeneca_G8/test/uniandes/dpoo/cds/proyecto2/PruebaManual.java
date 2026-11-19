package uniandes.dpoo.cds.proyecto2;

import java.time.*;
import java.util.*;

/** Validacion autocontenida sin librerias externas; NO reemplaza JUnit. */
public class PruebaManual {
    private static int realizadas=0;
    private static void comprobar(boolean cond,String msg){realizadas++;if(!cond)throw new AssertionError(msg);}
    private static void falla(Operacion op,String msg)throws Exception{realizadas++;
        try{op.ejecutar();throw new AssertionError("Se esperaba rechazo: "+msg);}
        catch(AssertionError a){throw a;}catch(Exception esperado){}
    }
    @FunctionalInterface interface Operacion {void ejecutar()throws Exception;}
    public static void main(String[] args)throws Exception{
        HistoriasOficiales h=new HistoriasOficiales();
        h.crearTienda("Norte");h.crearTienda("Sur");h.agregarProducto("Norte","pelota");
        h.trasladarProducto("Norte","Sur","pelota");
        comprobar(!h.tieneProducto("Norte","pelota")&&h.tieneProducto("Sur","pelota"),"Traslado CA1");
        falla(()->h.trasladarProducto("Norte","Sur","pelota"),"Traslado CA2");
        h.agregarProducto("Norte","pelota");
        falla(()->h.trasladarProducto("Norte","Sur","pelota"),"Traslado CA3");
        h.proponerSnack("emp","snack1","galleta",Set.of("GLUTEN"));h.aprobarSnack("snack1");
        comprobar(h.estaEnCatalogo("snack1"),"Snack CA1");
        h.proponerSnack("emp","snack2","fruta",Set.of());
        falla(()->h.aprobarSnack("snack2"),"Snack CA2");
        h.proponerSnack("emp","snack3","fruta",Set.of("NINGUNO"));h.rechazarSnack("snack3");
        comprobar(!h.estaEnCatalogo("snack3"),"Snack CA3");
        h.registrarDisciplina("tenis",1);h.registrarInstalacion("T1","tenis",2);h.registrarInstalacion("T2","tenis",2);
        LocalDate dia=LocalDate.of(2030,7,1);LocalTime a=LocalTime.of(10,0),b=LocalTime.of(11,0);
        h.reservar("socio","T1",dia,a,b,2,"SENCILLOS");comprobar(h.getReservas().size()==1,"Reserva CA1");
        falla(()->h.reservar("socio2","T2",dia,a,b,2,"SENCILLOS"),"Reserva CA2");
        h.registrarDisciplina("padel",1);h.registrarInstalacion("P1","padel",4);
        falla(()->h.reservar("socio","P1",dia,a,b,2,"SENCILLOS"),"Reserva CA3");
        GestorTorneos g=new GestorTorneos();
        var t=g.crear("Copa","tenis","Mayores",GestorTorneos.Tipo.RECREATIVO,GestorTorneos.Formato.INDIVIDUAL,dia,5,0,0,0.15);
        comprobar(t.getReservados()==1,"20% de cupos");
        var p1=new GestorTorneos.Participante("socio","socio","Mayores",GestorTorneos.Rol.SOCIO,false,null,null);
        var p2=new GestorTorneos.Participante("otra","otra","Mayores",GestorTorneos.Rol.EXTERNO,false,null,null);
        g.inscribir(t.id,"socio",List.of(p1));g.inscribir(t.id,"otra",List.of(p2));
        comprobar(t.getOcupados()==2,"Inscripcion");g.iniciar(t.id);g.registrarEncuentro(t.id,"socio","otra",2,1);
        g.finalizar(t.id,"socio");comprobar(t.getEncuentros().size()==1,"Historial");
        var bono=g.consultarBonos("socio").get(0);
        falla(()->g.redimirBono("socio",bono.codigo,100,true),"Bono no acumulable");
        comprobar(g.redimirBono("socio",bono.codigo,100,false)==85,"Bono redimido");
        falla(()->g.redimirBono("socio",bono.codigo,100,false),"Un solo uso");
        java.nio.file.Path temporal=java.nio.file.Files.createTempFile("proyecto2-",".dat");
        try{DatosProyecto2 d=DatosProyecto2.nuevoDemo();d.guardar(temporal);
            var recuperado=DatosProyecto2.cargar(temporal);comprobar(recuperado.torneos().consultar().size()==1,"Persistencia");
            comprobar(recuperado.autenticar("admin","admin1234",DatosProyecto2.Perfil.ADMINISTRADOR)!=null,"Autenticacion");
        }finally{java.nio.file.Files.deleteIfExists(temporal);}
        System.out.println("OK. Comprobaciones exitosas: "+realizadas);
    }
}
