package uniandes.dpoo.cds.proyecto2;

import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.time.LocalDate;
import java.util.*;

/** Persistencia predeterminada sin pedir nombre de archivo en la consola. */
public class DatosProyecto2 implements Serializable {
    private static final long serialVersionUID=1L;
    public enum Perfil { ADMINISTRADOR, SOCIO, EMPLEADO, VISITANTE }
    public static final class Cuenta implements Serializable {
        private static final long serialVersionUID=1L;
        public final String login;
        public final Perfil perfil;
        public final GestorTorneos.Rol rolTorneo;
        public final String categoria;
        private final byte[] sal,huella;
        public Cuenta(String login,String clave,Perfil perfil,GestorTorneos.Rol rolTorneo,String categoria){
            this.login=Objects.requireNonNull(login).trim();
            this.perfil=Objects.requireNonNull(perfil);this.rolTorneo=Objects.requireNonNull(rolTorneo);
            this.categoria=Objects.requireNonNull(categoria);
            sal=new byte[16];new SecureRandom().nextBytes(sal);
            huella=derivar(clave,sal);
        }
        public boolean validar(String clave){return clave!=null&&MessageDigest.isEqual(huella,derivar(clave,sal));}
    }
    private static byte[] derivar(String clave,byte[] sal){
        try {
            javax.crypto.spec.PBEKeySpec spec=new javax.crypto.spec.PBEKeySpec(clave.toCharArray(),sal,60000,256);
            try{return javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();}
            finally{spec.clearPassword();}
        }catch(java.security.GeneralSecurityException e){throw new IllegalStateException("Error de cifrado",e);}
    }
    private final Map<String,Cuenta> cuentas=new LinkedHashMap<>();
    private final GestorTorneos torneos=new GestorTorneos();
    private final HistoriasOficiales historias=new HistoriasOficiales();
    private final Map<String,Set<LocalDate>> turnosEmpleados=new LinkedHashMap<>();
    public GestorTorneos torneos(){return torneos;}
    public HistoriasOficiales historias(){return historias;}
    public Cuenta autenticar(String login,String clave,Perfil requerido) throws HistoriasOficiales.ReglaException {
        Cuenta c=cuentas.get(login==null?"":login.trim().toLowerCase(Locale.ROOT));
        if(c==null||!c.validar(clave)||c.perfil!=requerido)
            throw new HistoriasOficiales.ReglaException("Usuario, contrasena o rol incorrecto");
        return c;
    }
    public void agregarCuenta(String login,String clave,Perfil perfil,GestorTorneos.Rol rol,String categoria){
        if(login==null||login.isBlank()||clave==null||clave.length()<8)
            throw new IllegalArgumentException("Usuario o contrasena de menos de 8 caracteres");
        String key=login.trim().toLowerCase(Locale.ROOT);
        if(cuentas.containsKey(key))throw new IllegalArgumentException("La cuenta ya existe");
        cuentas.put(key,new Cuenta(login,clave,perfil,rol,categoria));
    }
    /** Los visitantes solo pueden crearse cuentas de estudiante o persona externa. */
    public Cuenta registrarVisitante(String login,String clave,GestorTorneos.Rol rol,String categoria){
        if(rol!=GestorTorneos.Rol.ESTUDIANTE&&rol!=GestorTorneos.Rol.EXTERNO)
            throw new IllegalArgumentException("El visitante debe ser estudiante o externo");
        if(categoria==null||categoria.isBlank())throw new IllegalArgumentException("Categoria obligatoria");
        agregarCuenta(login,clave,Perfil.VISITANTE,rol,categoria);
        return cuentas.get(login.trim().toLowerCase(Locale.ROOT));
    }
    public Collection<Cuenta> cuentasRegistradas(){return Collections.unmodifiableCollection(cuentas.values());}
    public Set<LocalDate> turnos(String login){return Collections.unmodifiableSet(turnosEmpleados.getOrDefault(login,Collections.emptySet()));}
    public void asignarTurno(String login,LocalDate dia){
        turnosEmpleados.computeIfAbsent(login,k->new HashSet<>()).add(dia);
    }
    public static DatosProyecto2 nuevoDemo(){
        DatosProyecto2 d=new DatosProyecto2();
        d.agregarCuenta("admin","admin1234",Perfil.ADMINISTRADOR,GestorTorneos.Rol.SOCIO,"Mayores");
        d.agregarCuenta("socio","socio1234",Perfil.SOCIO,GestorTorneos.Rol.SOCIO,"Mayores");
        d.agregarCuenta("empleado","empleado1234",Perfil.EMPLEADO,GestorTorneos.Rol.ENTRENADOR,"Mayores");
        try{
            d.historias.crearTienda("tienda-central");d.historias.crearTienda("tienda-norte");
            d.historias.agregarProducto("tienda-central","pelota");
            d.historias.definirPrecio("pelota",35000);
            d.historias.registrarDisciplina("padel",2);
            d.historias.registrarInstalacion("padel-1","padel",4);
            d.historias.registrarInstalacion("padel-2","padel",4);
            d.historias.registrarDisciplina("tenis",3);
            d.historias.registrarInstalacion("tenis-1","tenis",2);
            d.torneos.crear("Copa interna de tenis","tenis","Mayores",GestorTorneos.Tipo.RECREATIVO,
                GestorTorneos.Formato.INDIVIDUAL,LocalDate.now().plusDays(30),8,0,0,0.15);
        }catch(Exception e){throw new IllegalStateException("No se pudieron crear datos demo",e);}
        return d;
    }
    public static DatosProyecto2 cargar(Path ruta) throws IOException,ClassNotFoundException{
        if(!Files.exists(ruta))return nuevoDemo();
        try(ObjectInputStream o=new ObjectInputStream(Files.newInputStream(ruta))){
            Object v=o.readObject();
            if(!(v instanceof DatosProyecto2))throw new IOException("Formato de datos incompatible");
            return (DatosProyecto2)v;
        }
    }
    public void guardar(Path ruta) throws IOException {
        Path destino=ruta.toAbsolutePath().normalize();
        Files.createDirectories(destino.getParent());
        Path tmp=Files.createTempFile(destino.getParent(),"cds-",".tmp");
        try{
            try(ObjectOutputStream o=new ObjectOutputStream(Files.newOutputStream(tmp))){o.writeObject(this);}
            try{Files.move(tmp,destino,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
            catch(AtomicMoveNotSupportedException e){Files.move(tmp,destino,StandardCopyOption.REPLACE_EXISTING);}
        }finally{Files.deleteIfExists(tmp);}
    }
}
