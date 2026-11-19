package uniandes.dpoo.cds.proyecto2;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

/** HU-ADM-01, HU-ADM-03, HU-SOC-01. Fachada de reglas para Proyecto 2.
 *  Se mantiene en un paquete adicional para no alterar las clases del Proyecto 1.
 */
public class HistoriasOficiales implements Serializable {
    private static final long serialVersionUID = 1L;

    public static class ReglaException extends Exception {
        public ReglaException(String mensaje) { super(mensaje); }
    }
    private final Map<String, Set<String>> inventarios = new LinkedHashMap<>();
    private final Map<String, Sugerencia> sugerencias = new LinkedHashMap<>();
    private final Set<String> catalogo = new LinkedHashSet<>();
    private final Map<String, Integer> cuposDisciplina = new LinkedHashMap<>();
    private final Map<String, Integer> capacidadInstalacion = new LinkedHashMap<>();
    private final Map<String, String> disciplinaInstalacion = new LinkedHashMap<>();
    private final List<Reserva> reservas = new ArrayList<>();
    private final Map<String, Double> precios = new LinkedHashMap<>();
    private final List<Compra> compras = new ArrayList<>();

    public static class Sugerencia implements Serializable {
        private static final long serialVersionUID = 1L;
        public enum Estado { PENDIENTE, APROBADA, RECHAZADA }
        public final String codigo, nombre, empleado;
        public final Set<String> alergenos;
        public final double precio;
        private Estado estado = Estado.PENDIENTE;
        private Sugerencia(String c, String n, String e, Set<String> a, double precio) {
            codigo=c; nombre=n; empleado=e;
            this.precio=precio;
            alergenos=Collections.unmodifiableSet(new LinkedHashSet<>(a));
        }
        public Estado getEstado() { return estado; }
    }
    public static class Reserva implements Serializable {
        private static final long serialVersionUID = 1L;
        public final String socio, instalacion, disciplina, modalidad;
        public final LocalDate fecha;
        public final LocalTime inicio, fin;
        public final int jugadores;
        private Reserva(String s,String i,String d,LocalDate f,LocalTime a,LocalTime b,int j,String m){
            socio=s; instalacion=i; disciplina=d; fecha=f; inicio=a; fin=b; jugadores=j; modalidad=m;
        }
    }
    /** Venta persistida con registro del bono usado y del total final. */
    public static final class Compra implements Serializable {
        private static final long serialVersionUID=1L;
        public final String numero, socio, area, bonoAplicado;
        public final Map<String,Integer> detalle;
        public final double subtotal,total;
        private Compra(String numero,String socio,String area,Map<String,Integer> detalle,
                       double subtotal,double total,String bono){
            this.numero=numero;this.socio=socio;this.area=area;this.detalle=Collections.unmodifiableMap(new LinkedHashMap<>(detalle));
            this.subtotal=subtotal;this.total=total;this.bonoAplicado=bono;
        }
    }
    private static String obligatorio(String valor, String campo) throws ReglaException {
        if (valor==null||valor.trim().isEmpty()) throw new ReglaException("Falta "+campo);
        return valor.trim();
    }
    private static String key(String valor) { return valor.trim().toLowerCase(Locale.ROOT); }

    // HU-ADM-01: el traslado es ATOMICO: se validan origen y destino antes de modificar.
    public void crearTienda(String tienda) throws ReglaException {
        String t=key(obligatorio(tienda,"tienda"));
        if(inventarios.containsKey(t)) throw new ReglaException("Tienda ya existe");
        inventarios.put(t,new LinkedHashSet<>());
    }
    public void agregarProducto(String tienda,String codigo) throws ReglaException {
        Set<String> items=obtenerTienda(tienda);
        items.add(key(obligatorio(codigo,"producto")));
    }
    private Set<String> obtenerTienda(String nombre) throws ReglaException {
        Set<String> items=inventarios.get(key(obligatorio(nombre,"tienda")));
        if(items==null)throw new ReglaException("Tienda no existe");
        return items;
    }
    public void trasladarProducto(String origen,String destino,String codigo) throws ReglaException {
        Set<String> salida=obtenerTienda(origen), llegada=obtenerTienda(destino);
        String producto=key(obligatorio(codigo,"producto"));
        if(salida==llegada)throw new ReglaException("Origen y destino iguales");
        if(!salida.contains(producto))throw new ReglaException("Producto ausente en origen");
        if(llegada.contains(producto))throw new ReglaException("Producto ya existe en destino");
        salida.remove(producto);
        llegada.add(producto);
    }
    public boolean tieneProducto(String tienda,String codigo) throws ReglaException {
        return obtenerTienda(tienda).contains(key(obligatorio(codigo,"producto")));
    }
    public Set<String> productosDe(String tienda) throws ReglaException {
        return Collections.unmodifiableSet(new LinkedHashSet<>(obtenerTienda(tienda)));
    }

    // HU-ADM-03: los alergenos se deben DECLARAR antes de aprobar; "NINGUNO" es explicito.
    public void proponerSnack(String empleado,String codigo,String nombre,Set<String> alergenos) throws ReglaException {
        proponerSnack(empleado,codigo,nombre,alergenos,10000.0);
    }
    public void proponerSnack(String empleado,String codigo,String nombre,Set<String> alergenos,double precio) throws ReglaException {
        String c=key(obligatorio(codigo,"codigo"));
        if(!Double.isFinite(precio)||precio<=0)throw new ReglaException("Precio del snack invalido");
        if(sugerencias.containsKey(c)||catalogo.contains(c))throw new ReglaException("Producto duplicado");
        sugerencias.put(c,new Sugerencia(c,obligatorio(nombre,"nombre"),obligatorio(empleado,"empleado"),
            alergenos==null?Collections.emptySet():alergenos,precio));
    }
    public void aprobarSnack(String codigo) throws ReglaException {
        Sugerencia s=obtenerSugerencia(codigo);
        if(s.estado!=Sugerencia.Estado.PENDIENTE)throw new ReglaException("Sugerencia ya resuelta");
        if(s.alergenos.isEmpty())throw new ReglaException("Debe declarar alergenos o NINGUNO");
        s.estado=Sugerencia.Estado.APROBADA;
        catalogo.add(s.codigo);
        precios.put(s.codigo,s.precio);
    }
    public void rechazarSnack(String codigo) throws ReglaException {
        Sugerencia s=obtenerSugerencia(codigo);
        if(s.estado!=Sugerencia.Estado.PENDIENTE)throw new ReglaException("Sugerencia ya resuelta");
        s.estado=Sugerencia.Estado.RECHAZADA;
    }
    public Sugerencia obtenerSugerencia(String codigo) throws ReglaException {
        Sugerencia s=sugerencias.get(key(obligatorio(codigo,"codigo")));
        if(s==null)throw new ReglaException("Sugerencia no existe");
        return s;
    }
    public boolean estaEnCatalogo(String codigo) { return codigo!=null&&catalogo.contains(key(codigo)); }

    // HU-SOC-01: bloques solapados y cupos simultaneos por disciplina.
    public void registrarDisciplina(String nombre, int maximoSimultaneo) throws ReglaException {
        String d=key(obligatorio(nombre,"disciplina"));
        if(maximoSimultaneo<1)throw new ReglaException("Maximo invalido");
        cuposDisciplina.put(d,maximoSimultaneo);
    }
    public void registrarInstalacion(String codigo,String disciplina,int capacidad) throws ReglaException {
        String i=key(obligatorio(codigo,"instalacion")), d=key(obligatorio(disciplina,"disciplina"));
        if(!cuposDisciplina.containsKey(d))throw new ReglaException("Disciplina sin registrar");
        if(capacidad<1||capacidadInstalacion.containsKey(i))throw new ReglaException("Instalacion invalida");
        capacidadInstalacion.put(i,capacidad);
        disciplinaInstalacion.put(i,d);
    }
    public Reserva reservar(String socio,String instalacion,LocalDate fecha,LocalTime inicio,
                           LocalTime fin,int jugadores,String modalidad) throws ReglaException {
        String s=obligatorio(socio,"socio"), i=key(obligatorio(instalacion,"instalacion"));
        String m=obligatorio(modalidad,"modalidad").toUpperCase(Locale.ROOT);
        String d=disciplinaInstalacion.get(i);
        if(d==null)throw new ReglaException("Instalacion inexistente");
        if(fecha==null||inicio==null||fin==null||!inicio.isBefore(fin))throw new ReglaException("Bloque invalido");
        if(!m.equals("SENCILLOS")&&!m.equals("DOBLES"))throw new ReglaException("Modalidad invalida");
        if(!d.equals("tenis")&&!d.equals("tenis de mesa")&&!d.equals("ping pong")&&!d.contains("padel"))
            throw new ReglaException("Disciplina no reconocida");
        if(d.contains("padel") && !m.equals("DOBLES"))
            throw new ReglaException("Padel requiere dobles");
        int requeridos=m.equals("SENCILLOS")?2:4;
        if(jugadores!=requeridos||jugadores>capacidadInstalacion.get(i))
            throw new ReglaException("Numero de jugadores incompatible con modalidad o capacidad");
        int simultaneas=0;
        Set<String> usadas=new HashSet<>();
        for(Reserva r:reservas){
            if(r.fecha.equals(fecha)&&r.disciplina.equals(d)&&inicio.isBefore(r.fin)&&r.inicio.isBefore(fin)){
                if(r.instalacion.equals(i))throw new ReglaException("Instalacion ocupada");
                usadas.add(r.instalacion);
            }
        }
        simultaneas=usadas.size();
        if(simultaneas>=cuposDisciplina.get(d))throw new ReglaException("Sin disponibilidad en la disciplina");
        Reserva r=new Reserva(s,i,d,fecha,inicio,fin,jugadores,m);
        reservas.add(r);
        return r;
    }
    public List<Reserva> getReservas(){return Collections.unmodifiableList(reservas);}

    // Venta real del modulo: registra una compra de tienda o cafeteria y redime el bono en la misma operacion.
    public void definirPrecio(String codigo,double precio) throws ReglaException {
        String c=key(obligatorio(codigo,"producto"));
        boolean disponible=catalogo.contains(c);
        if(!disponible)for(Set<String> productos:inventarios.values())if(productos.contains(c))disponible=true;
        if(!disponible)throw new ReglaException("Producto no disponible para la venta");
        if(!Double.isFinite(precio)||precio<=0)throw new ReglaException("Precio invalido");
        precios.put(c,precio);
    }
    public double getPrecio(String codigo) throws ReglaException {
        Double v=precios.get(key(obligatorio(codigo,"producto")));
        if(v==null)throw new ReglaException("Precio de venta no configurado");
        return v;
    }
    public List<Compra> getCompras(){return Collections.unmodifiableList(compras);}
    /** La compra conserva el registro y solo consume bono una vez validados todos los articulos. */
    public Compra comprar(String socio,String area,Map<String,Integer> cantidades,
                         String codigoBono,boolean otroDescuento,GestorTorneos torneos)
                         throws ReglaException,GestorTorneos.ReglaException {
        String comprador=obligatorio(socio,"comprador");
        String a=key(obligatorio(area,"tienda o cafeteria"));
        boolean cafeteria=a.equals("cafeteria");
        if(!cafeteria&&!inventarios.containsKey(a))throw new ReglaException("Tienda inexistente");
        if(cantidades==null||cantidades.isEmpty())throw new ReglaException("Compra sin productos");
        Map<String,Integer> detalle=new LinkedHashMap<>();
        double subtotal=0.0;
        for(Map.Entry<String,Integer> item:cantidades.entrySet()){
            String codigo=key(obligatorio(item.getKey(),"codigo de articulo"));
            Integer cantidad=item.getValue();
            if(cantidad==null||cantidad<=0)throw new ReglaException("Cantidad invalida");
            boolean disponible=cafeteria?catalogo.contains(codigo):inventarios.get(a).contains(codigo);
            if(!disponible)throw new ReglaException("Articulo no disponible en "+a+": "+codigo);
            double precio=getPrecio(codigo);
            subtotal+=precio*cantidad;
            if(!Double.isFinite(subtotal))throw new ReglaException("Subtotal demasiado grande");
            detalle.merge(codigo,cantidad,Integer::sum);
        }
        String bono=codigoBono==null?"":codigoBono.trim();
        double total=subtotal;
        if(!bono.isEmpty()){
            if(torneos==null)throw new ReglaException("Gestor de bonos no disponible");
            total=torneos.redimirBono(comprador,bono,subtotal,otroDescuento);
        }
        Compra resultado=new Compra("COMP-"+(compras.size()+1),comprador,a,detalle,
                    Math.round(subtotal*100.0)/100.0,total,bono);
        compras.add(resultado);
        return resultado;
    }

}
