package uniandes.dpoo.cds.proyecto2;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.*;

/** Modulo adicional de torneos internos para Club Deportivo Seneca.
 *  Mantiene reglas de cupos, participacion, premios, historial y bonos.
 */
public class GestorTorneos implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Tipo { RECREATIVO, COMPETITIVO }
    public enum Formato { EQUIPOS, INDIVIDUAL, DOBLES }
    public enum Estado { INSCRIPCIONES, EN_CURSO, FINALIZADO }
    public enum Rol { SOCIO, ESTUDIANTE, EXTERNO, ENTRENADOR, FISIOTERAPEUTA, EQUIPO }

    public static class ReglaException extends Exception {
        public ReglaException(String m){super(m);}
    }
    private static String obligatorio(String v,String campo) throws ReglaException {
        if(v==null||v.isBlank())throw new ReglaException("Debe indicar "+campo);
        return v.trim();
    }
    public static final class Participante implements Serializable {
        private static final long serialVersionUID=1L;
        public final String codigo,nombre,categoria;
        public final Rol rol;
        public final boolean preferencial;
        public final Set<LocalDate> diasTurno;
        public final List<String> sociosIntegrantes;
        public Participante(String codigo,String nombre,String categoria,Rol rol,boolean preferencial,
                 Set<LocalDate> diasTurno,List<String> sociosIntegrantes) throws ReglaException {
            this.codigo=obligatorio(codigo,"codigo");
            this.nombre=obligatorio(nombre,"nombre");
            this.categoria=obligatorio(categoria,"categoria o nivel");
            if(rol==null)throw new ReglaException("Rol obligatorio");
            this.rol=rol;
            this.preferencial=preferencial;
            this.diasTurno=Collections.unmodifiableSet(new HashSet<>(diasTurno==null?Collections.emptySet():diasTurno));
            this.sociosIntegrantes=Collections.unmodifiableList(new ArrayList<>(sociosIntegrantes==null?Collections.emptyList():sociosIntegrantes));
        }
        public boolean esEmpleado(){return rol==Rol.ENTRENADOR||rol==Rol.FISIOTERAPEUTA;}
    }
    public static final class Inscripcion implements Serializable {
        private static final long serialVersionUID=1L;
        public final String codigo;
        public final List<Participante> miembros;
        public final boolean cupoPreferencial;
        public final double valorInscripcion;
        private Inscripcion(String codigo,List<Participante> miembros,boolean pref,double cobro){
            this.codigo=codigo;
            this.miembros=Collections.unmodifiableList(new ArrayList<>(miembros));
            this.cupoPreferencial=pref;
            this.valorInscripcion=cobro;
        }
    }
    public static final class Encuentro implements Serializable {
        private static final long serialVersionUID=1L;
        public final String local,visitante;
        public final int puntosLocal,puntosVisitante;
        private Encuentro(String a,String b,int pa,int pb){local=a;visitante=b;puntosLocal=pa;puntosVisitante=pb;}
        @Override public String toString(){return local+" "+puntosLocal+" - "+puntosVisitante+" "+visitante;}
    }
    public static final class Bono implements Serializable {
        private static final long serialVersionUID=1L;
        public final String codigo,socio;
        public final double descuento;
        private boolean usado;
        private Bono(String socio,double descuento){
            codigo=UUID.randomUUID().toString();this.socio=socio;this.descuento=descuento;
        }
        public boolean estaUsado(){return usado;}
        private double aplicar(double subtotal, boolean otroDescuento) throws ReglaException {
            if(usado)throw new ReglaException("Bono ya utilizado");
            if(otroDescuento)throw new ReglaException("El bono no se acumula con otros descuentos");
            if(!Double.isFinite(subtotal)||subtotal<=0)throw new ReglaException("Subtotal invalido");
            usado=true;
            return Math.round(subtotal*(1.0-descuento)*100.0)/100.0;
        }
    }
    public static final class Torneo implements Serializable {
        private static final long serialVersionUID=1L;
        public final String id,nombre,deporte,categoria;
        public final Tipo tipo;
        public final Formato formato;
        public final LocalDate fecha;
        public final int cupos;
        public final double tarifa,porcentajePremio,descuentoBono;
        private final Map<String,Inscripcion> inscripciones=new LinkedHashMap<>();
        private final List<Encuentro> encuentros=new ArrayList<>();
        private Estado estado=Estado.INSCRIPCIONES;
        private boolean liberados;
        private final Map<String,Integer> rankingAcreditado = new LinkedHashMap<>();
        private String ganador;
        private double recaudado,premioPagado;
        private Torneo(String id,String nombre,String deporte,String categoria,Tipo tipo,Formato formato,
                LocalDate fecha,int cupos,double tarifa,double porcentajePremio,double descuentoBono){
            this.id=id;this.nombre=nombre;this.deporte=deporte;this.categoria=categoria;this.tipo=tipo;
            this.formato=formato;this.fecha=fecha;this.cupos=cupos;this.tarifa=tarifa;
            this.porcentajePremio=porcentajePremio;this.descuentoBono=descuentoBono;
        }
        public Estado getEstado(){return estado;}
        public int getReservados(){return (cupos+4)/5;}
        public int getOcupados(){return inscripciones.size();}
        public int getCuposLibres(){return cupos-inscripciones.size();}
        public int getPreferencialesOcupados(){return (int)inscripciones.values().stream().filter(i->i.cupoPreferencial).count();}
        public boolean estanLiberados(){return liberados;}
        public Map<String,Integer> getRankingAcreditado(){return Collections.unmodifiableMap(rankingAcreditado);}
        public LocalDate getFechaLiberacionPreferencial(){return fecha.minusDays(1);}
        public Map<String,Inscripcion> getInscripciones(){return Collections.unmodifiableMap(inscripciones);}
        public List<Encuentro> getEncuentros(){return Collections.unmodifiableList(encuentros);}
        public String getGanador(){return ganador;}
        public double getRecaudado(){return recaudado;}
        public double getPremioPagado(){return premioPagado;}
        public Map<String,Integer> getPosiciones(){
            Map<String,Integer> puntos=new LinkedHashMap<>();
            for(String s:inscripciones.keySet())puntos.put(s,0);
            for(Encuentro e:encuentros){
                if(e.puntosLocal>e.puntosVisitante)puntos.merge(e.local,3,Integer::sum);
                else if(e.puntosLocal<e.puntosVisitante)puntos.merge(e.visitante,3,Integer::sum);
                else{puntos.merge(e.local,1,Integer::sum);puntos.merge(e.visitante,1,Integer::sum);}
            }
            return puntos.entrySet().stream().sorted(Map.Entry.<String,Integer>comparingByValue().reversed()
                    .thenComparing(Map.Entry.comparingByKey()))
                .collect(LinkedHashMap::new,(m,e)->m.put(e.getKey(),e.getValue()),LinkedHashMap::putAll);
        }
    }
    private final Map<String,Torneo> torneos=new LinkedHashMap<>();
    private final Map<String,List<Bono>> bonosPorSocio=new LinkedHashMap<>();
    private int consecutivo=1;
    public Torneo crear(String nombre,String deporte,String categoria,Tipo tipo,Formato formato,
             LocalDate fecha,int cupos,double tarifa,double porcentajePremio,double descuentoBono) throws ReglaException {
        obligatorio(nombre,"nombre");obligatorio(deporte,"deporte");obligatorio(categoria,"categoria");
        if(tipo==null||formato==null||fecha==null)throw new ReglaException("Datos incompletos");
        if(cupos<2)throw new ReglaException("El torneo requiere al menos dos cupos");
        if(fecha.isBefore(LocalDate.now()))throw new ReglaException("Fecha de torneo en el pasado");
        if(!Double.isFinite(tarifa)||tarifa<0||!Double.isFinite(porcentajePremio)||porcentajePremio<0||porcentajePremio>1||
           !Double.isFinite(descuentoBono)||descuentoBono<0||descuentoBono>1)
            throw new ReglaException("Montos o porcentajes invalidos");
        if(tipo==Tipo.RECREATIVO&&tarifa!=0)throw new ReglaException("El torneo recreativo es gratuito");
        if(tipo==Tipo.COMPETITIVO&&tarifa<=0)throw new ReglaException("El competitivo requiere tarifa");
        String id="TOR-"+(consecutivo++);
        Torneo t=new Torneo(id,nombre.trim(),deporte.trim(),categoria.trim(),tipo,formato,fecha,cupos,tarifa,
                porcentajePremio,descuentoBono);
        torneos.put(id,t);return t;
    }
    public Torneo buscar(String id) throws ReglaException {
        Torneo t=torneos.get(obligatorio(id,"id de torneo"));
        if(t==null)throw new ReglaException("Torneo no existe");
        return t;
    }
    public Collection<Torneo> consultar(){
        for (Torneo t:torneos.values()) actualizarLiberacion(t);
        return Collections.unmodifiableCollection(torneos.values());
    }
    private void actualizarLiberacion(Torneo t){
        if(t.estado==Estado.INSCRIPCIONES && !LocalDate.now().isBefore(t.getFechaLiberacionPreferencial()))
            t.liberados=true;
    }
    /** El ranking se acredita por el administrador, no por declaracion del jugador. */
    public void acreditarRanking(String id,String codigoParticipante,int posicion) throws ReglaException {
        Torneo t=buscar(id);
        if(t.estado!=Estado.INSCRIPCIONES)throw new ReglaException("Torneo ya iniciado");
        if(posicion<1)throw new ReglaException("Posicion de ranking invalida");
        t.rankingAcreditado.put(obligatorio(codigoParticipante,"participante").toLowerCase(Locale.ROOT),posicion);
    }
    public boolean tienePreferencia(String id,String codigoParticipante) throws ReglaException {
        Torneo t=buscar(id);actualizarLiberacion(t);
        Integer posicion=t.rankingAcreditado.get(obligatorio(codigoParticipante,"participante").toLowerCase(Locale.ROOT));
        return !t.liberados && posicion!=null && posicion<=t.getReservados();
    }

    public Inscripcion inscribir(String id,String codigo,List<Participante> miembros) throws ReglaException {
        Torneo t=buscar(id);
        String c=obligatorio(codigo,"codigo de inscripcion");
        actualizarLiberacion(t);
        if(t.estado!=Estado.INSCRIPCIONES)throw new ReglaException("Inscripciones cerradas");
        if(t.inscripciones.containsKey(c))throw new ReglaException("Inscripcion duplicada");
        if(t.getCuposLibres()==0)throw new ReglaException("Cupos agotados");
        if(miembros==null||miembros.size()!=(t.formato==Formato.DOBLES?2:1))
            throw new ReglaException("Cantidad de participantes incompatible con el formato");
        Set<String> ids=new HashSet<>();
        for(Participante p:miembros){
            if(p==null||!ids.add(p.codigo))throw new ReglaException("Participante nulo o repetido");
            if(!t.categoria.equalsIgnoreCase(p.categoria))throw new ReglaException("Categoria o nivel incompatible");
            if(p.preferencial && !tienePreferencia(id,p.codigo))
                throw new ReglaException("Ranking no acreditado o fuera de los cupos preferenciales");
            if(t.formato==Formato.EQUIPOS && p.rol!=Rol.EQUIPO)
                throw new ReglaException("Se debe inscribir un equipo completo");
            if(t.formato!=Formato.EQUIPOS && p.rol==Rol.EQUIPO)
                throw new ReglaException("Se deben inscribir jugadores");
            if(p.esEmpleado()){
                if(t.formato==Formato.EQUIPOS)throw new ReglaException("Empleados solo en individuales");
                if(p.diasTurno.contains(t.fecha))throw new ReglaException("Empleado de turno en fecha del torneo");
            }
            for(Inscripcion previa:t.inscripciones.values()){
                for(Participante existente:previa.miembros)
                    if(existente.codigo.equalsIgnoreCase(p.codigo))throw new ReglaException("Ya inscrito al torneo");
            }
        }
        boolean todosPreferenciales=miembros.stream().allMatch(p->p.preferencial &&
            t.rankingAcreditado.containsKey(p.codigo.toLowerCase(Locale.ROOT)));
        boolean preferencial=!t.liberados && todosPreferenciales && t.getPreferencialesOcupados()<t.getReservados();
        int ocupadosRegulares=t.getOcupados()-t.getPreferencialesOcupados();
        int maximaRegular=t.cupos-(t.liberados?0:t.getReservados());
        if(!preferencial&&ocupadosRegulares>=maximaRegular)
            throw new ReglaException("Solo quedan cupos reservados para ranking");
        long empleados=miembros.stream().filter(Participante::esEmpleado).count();
        // En dobles mixtos solo pagan los participantes que no sean empleados.
        double cobro=t.tipo==Tipo.RECREATIVO?0:
            Math.round(t.tarifa*(miembros.size()-empleados)/miembros.size()*100.0)/100.0;
        Inscripcion nueva=new Inscripcion(c,miembros,preferencial,cobro);
        t.inscripciones.put(c,nueva);
        t.recaudado+=cobro;
        return nueva;
    }
    public void retirar(String id,String codigo) throws ReglaException {
        Torneo t=buscar(id);
        if(t.estado!=Estado.INSCRIPCIONES)throw new ReglaException("No se puede retirar tras inicio");
        Inscripcion i=t.inscripciones.remove(obligatorio(codigo,"codigo"));
        if(i==null)throw new ReglaException("Inscripcion inexistente");
        t.recaudado-=i.valorInscripcion;
    }
    public void liberarPreferenciales(String id) throws ReglaException {
        Torneo t=buscar(id);
        if(t.estado!=Estado.INSCRIPCIONES)throw new ReglaException("El torneo ya inicio");
        t.liberados=true;
    }
    public void iniciar(String id) throws ReglaException {
        Torneo t=buscar(id);
        if(t.estado!=Estado.INSCRIPCIONES)throw new ReglaException("Estado invalido para iniciar");
        if(t.inscripciones.size()<2)throw new ReglaException("Se requieren al menos dos inscripciones");
        t.liberados=true;t.estado=Estado.EN_CURSO;
    }
    public void registrarEncuentro(String id,String a,String b,int pa,int pb) throws ReglaException {
        Torneo t=buscar(id);
        if(t.estado!=Estado.EN_CURSO)throw new ReglaException("El torneo no esta en curso");
        if(a==null||b==null||a.equals(b)||!t.inscripciones.containsKey(a)||!t.inscripciones.containsKey(b))
            throw new ReglaException("Inscripciones invalidas");
        if(pa<0||pb<0)throw new ReglaException("Marcador negativo");
        t.encuentros.add(new Encuentro(a,b,pa,pb));
    }
    public void finalizar(String id,String codigoGanador) throws ReglaException {
        Torneo t=buscar(id);
        if(t.estado!=Estado.EN_CURSO)throw new ReglaException("Torneo no iniciado o finalizado");
        Inscripcion ganador=t.inscripciones.get(obligatorio(codigoGanador,"ganador"));
        if(ganador==null)throw new ReglaException("Ganador no inscrito");
        t.ganador=codigoGanador;
        t.estado=Estado.FINALIZADO;
        if(t.tipo==Tipo.COMPETITIVO){
            long elegibles=ganador.miembros.stream().filter(p->!p.esEmpleado()).count();
            // Ningun empleado cobra su parte del premio en efectivo.
            t.premioPagado=Math.round(t.recaudado*t.porcentajePremio*
                elegibles/ganador.miembros.size()*100.0)/100.0;
        }else{
            for(Participante p:ganador.miembros){
                if(p.rol==Rol.SOCIO)agregarBono(p.codigo,t.descuentoBono);
                if(p.rol==Rol.EQUIPO)for(String socio:p.sociosIntegrantes)agregarBono(socio,t.descuentoBono);
            }
        }
    }
    private void agregarBono(String socio,double descuento){
        bonosPorSocio.computeIfAbsent(socio,k->new ArrayList<>()).add(new Bono(socio,descuento));
    }
    public List<Bono> consultarBonos(String socio){
        return Collections.unmodifiableList(bonosPorSocio.getOrDefault(socio,Collections.emptyList()));
    }
    public double redimirBono(String socio,String codigoBono,double subtotal,boolean otroDescuento) throws ReglaException {
        for(Bono b:consultarBonos(socio))if(b.codigo.equals(codigoBono))return b.aplicar(subtotal,otroDescuento);
        throw new ReglaException("Bono no pertenece al socio");
    }
}
