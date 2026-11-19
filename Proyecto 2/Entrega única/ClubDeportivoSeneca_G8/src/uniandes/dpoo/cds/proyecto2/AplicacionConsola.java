package uniandes.dpoo.cds.proyecto2;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

/** Interfaz comun validada. Cada rol tiene su main; ninguna accion se permite sin autenticar. */
public final class AplicacionConsola {
    private AplicacionConsola() { }

    public static void iniciar(DatosProyecto2.Perfil perfil) {
        IntegracionProyecto1 integracion;
        DatosProyecto2 datos;
        try {
            integracion = IntegracionProyecto1.abrir();
            datos = integracion.datos();
        } catch (Exception e) {
            System.err.println("No se pudieron cargar los datos: " + e.getMessage());
            return;
        }
        try (Scanner scanner = new Scanner(System.in)) {
            Consola ui = new Consola(scanner, datos);
            System.out.println("\nCLUB DEPORTIVO SENECA / PROYECTO 2 - " + perfil);
            if(integracion.estaIntegrado()) System.out.println("Usando estado compartido con el Proyecto 1.");
            DatosProyecto2.Cuenta cuenta;
            if (perfil == DatosProyecto2.Perfil.VISITANTE && ui.entero("1. Ingresar  2. Crear cuenta: ") == 2) {
                String usuario = ui.texto("Nuevo usuario: ");
                String clave = ui.texto("Nueva contrasena (minimo 8 caracteres): ");
                GestorTorneos.Rol rol = GestorTorneos.Rol.valueOf(
                        ui.texto("Tipo ESTUDIANTE/EXTERNO: ").toUpperCase(Locale.ROOT));
                datos.registrarVisitante(usuario, clave, rol, ui.texto("Categoria o nivel: "));
                System.out.println("Cuenta registrada. Ya puedes iniciar sesion.");
            }
            String usuario = ui.texto("Usuario: ");
            String clave = ui.texto("Contrasena: ");
            cuenta = datos.autenticar(usuario, clave, perfil);
            System.out.println("Bienvenido/a: " + cuenta.login + " (" + cuenta.perfil + ")");
            boolean activo = true;
            while (activo) {
                ui.menu(perfil);
                int opcion = ui.entero("Opcion: ");
                if (opcion == 0) {
                    activo = false;
                } else {
                    try {
                        switch (perfil) {
                            case ADMINISTRADOR -> ui.administrar(opcion);
                            case SOCIO -> ui.socio(opcion, cuenta);
                            case EMPLEADO -> ui.empleado(opcion, cuenta);
                            case VISITANTE -> ui.visitante(opcion, cuenta);
                        }
                    } catch (Exception e) {
                        System.out.println("Operación rechazada: " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("No se pudo continuar: " + e.getMessage());
        } finally {
            try {
                integracion.guardar();
                System.out.println("Cambios guardados correctamente.");
            } catch (Exception e) {
                System.err.println("No se pudieron guardar los datos: " + e.getMessage());
            }
        }
    }

    private static final class Consola {
        private final Scanner entrada;
        private final DatosProyecto2 datos;
        private final GestorTorneos torneos;
        private final HistoriasOficiales historias;
        private Consola(Scanner scanner, DatosProyecto2 datos) {
            this.entrada = scanner;
            this.datos = datos;
            this.torneos = datos.torneos();
            this.historias = datos.historias();
        }
        private String texto(String mensaje) {
            while (true) {
                System.out.print(mensaje);
                String v = entrada.nextLine().trim();
                if (!v.isBlank()) return v;
                System.out.println("No puede estar vacio.");
            }
        }
        private int entero(String mensaje) {
            while (true) {
                try { return Integer.parseInt(texto(mensaje)); }
                catch (NumberFormatException e) { System.out.println("Introduce un numero entero."); }
            }
        }
        private double decimal(String mensaje) {
            while (true) {
                try {
                    double numero = Double.parseDouble(texto(mensaje).replace(',', '.'));
                    if (Double.isFinite(numero)) return numero;
                } catch (NumberFormatException ignored) { }
                System.out.println("Introduce un decimal valido.");
            }
        }
        private LocalDate fecha(String mensaje) {
            while (true) {
                try { return LocalDate.parse(texto(mensaje + " (AAAA-MM-DD): ")); }
                catch (Exception e) { System.out.println("Fecha invalida."); }
            }
        }
        private LocalTime hora(String mensaje) {
            while (true) {
                try { return LocalTime.parse(texto(mensaje + " (HH:MM): ")); }
                catch (Exception e) { System.out.println("Hora invalida."); }
            }
        }
        private boolean si(String mensaje) {
            while (true) {
                String dato = texto(mensaje + " (s/n): ").toLowerCase(Locale.ROOT);
                if (dato.equals("s")) return true;
                if (dato.equals("n")) return false;
                System.out.println("Responde s o n.");
            }
        }
        private void menu(DatosProyecto2.Perfil perfil) {
            System.out.println("\n--- " + perfil + " ---");
            if (perfil == DatosProyecto2.Perfil.ADMINISTRADOR) {
                System.out.println("1 Torneos | 2 Crear torneo | 3 Acreditar ranking | 4 Liberar cupos");
                System.out.println("5 Iniciar torneo | 6 Registrar encuentro | 7 Finalizar torneo");
                System.out.println("8 Inscribir estudiante/externo/equipo | 9 Trasladar producto");
                System.out.println("10 Aprobar snack | 11 Rechazar snack | 12 Consultar sugerencia");
                System.out.println("13 Definir precio | 14 Crear tienda / agregar producto");
                System.out.println("15 Asignar turno | 16 Ver compras | 17 Ver reservas | 18 Ver historial detallado");
                System.out.println("19 Consultar productos de una tienda");
            } else if (perfil == DatosProyecto2.Perfil.SOCIO) {
                System.out.println("1 Torneos | 2 Inscribirme | 3 Retirarme | 4 Reservar instalacion");
                System.out.println("5 Mis bonos | 6 Comprar en tienda | 7 Comprar en cafeteria");
                System.out.println("8 Mis compras | 9 Historial de un torneo");
            } else if (perfil == DatosProyecto2.Perfil.EMPLEADO) {
                System.out.println("1 Torneos | 2 Inscribirme | 3 Retirarme | 4 Sugerir snack");
                System.out.println("5 Historial de un torneo");
            } else {
                System.out.println("1 Torneos | 2 Inscribirme | 3 Retirarme | 4 Historial de un torneo");
            }
            System.out.println("0 Guardar y salir");
        }
        private void listar() {
            for (GestorTorneos.Torneo t : torneos.consultar()) {
                System.out.printf("%s | %s | %s | %s | %s | %d/%d cupos | ranking %d/%d | recaudo %.2f%n",
                    t.id, t.nombre, t.deporte, t.categoria, t.getEstado(), t.getOcupados(), t.cupos,
                    t.getPreferencialesOcupados(), t.getReservados(), t.getRecaudado());
            }
        }
        private void historial() throws Exception {
            GestorTorneos.Torneo t = torneos.buscar(texto("ID torneo: "));
            System.out.println("Torneo: " + t.nombre + " / Estado: " + t.getEstado());
            System.out.println("Tipo: " + t.tipo + " / Modalidad: " + t.formato + " / Fecha: " + t.fecha);
            System.out.println("Inscritos:");
            for (GestorTorneos.Inscripcion i : t.getInscripciones().values()) {
                System.out.println(" - " + i.codigo + " | participantes: " + nombres(i.miembros)
                            + " | preferencial: " + i.cupoPreferencial);
            }
            System.out.println("Encuentros:");
            for (GestorTorneos.Encuentro e : t.getEncuentros()) System.out.println(" - " + e);
            System.out.println("Posiciones (puntos): " + t.getPosiciones());
            System.out.println("Ganador: " + t.getGanador() + " | Recaudo: " + t.getRecaudado()
                    + " | Premio efectivo: " + t.getPremioPagado());
        }
        private String nombres(List<GestorTorneos.Participante> jugadores) {
            List<String> nombres = new ArrayList<>();
            for (GestorTorneos.Participante p : jugadores) nombres.add(p.nombre);
            return String.join(", ", nombres);
        }
        private void administrar(int opcion) throws Exception {
            switch (opcion) {
                case 1 -> listar();
                case 2 -> {
                    String nombre = texto("Nombre torneo: "), deporte = texto("Deporte/disciplina: ");
                    String categoria = texto("Categoria o nivel: ");
                    GestorTorneos.Tipo tipo = GestorTorneos.Tipo.valueOf(texto("RECREATIVO/COMPETITIVO: ").toUpperCase(Locale.ROOT));
                    GestorTorneos.Formato formato = GestorTorneos.Formato.valueOf(texto("EQUIPOS/INDIVIDUAL/DOBLES: ").toUpperCase(Locale.ROOT));
                    LocalDate fecha = fecha("Fecha del torneo");
                    int cupos = entero("Cantidad de cupos: ");
                    double tarifa = tipo == GestorTorneos.Tipo.COMPETITIVO ? decimal("Tarifa de inscripcion: ") : 0;
                    double premio = tipo == GestorTorneos.Tipo.COMPETITIVO ? decimal("Porcentaje del recaudo para premio (0..1): ") : 0;
                    double bono = tipo == GestorTorneos.Tipo.RECREATIVO ? decimal("Descuento del bono (0..1): ") : 0;
                    GestorTorneos.Torneo t = torneos.crear(nombre, deporte, categoria, tipo, formato,
                            fecha, cupos, tarifa, premio, bono);
                    System.out.println("Creado " + t.id + ". Ranking reservado: " + t.getReservados());
                }
                case 3 -> {
                    torneos.acreditarRanking(texto("ID torneo: "), texto("Codigo participante/equipo: "),
                            entero("Posicion de ranking acreditada: "));
                    System.out.println("Ranking acreditado.");
                }
                case 4 -> { torneos.liberarPreferenciales(texto("ID torneo: ")); System.out.println("Cupos liberados."); }
                case 5 -> { torneos.iniciar(texto("ID torneo: ")); System.out.println("Torneo en curso."); }
                case 6 -> {
                    String id = texto("ID torneo: "), a = texto("Inscripcion A: "), b = texto("Inscripcion B: ");
                    torneos.registrarEncuentro(id, a, b, entero("Puntos A: "), entero("Puntos B: "));
                    System.out.println("Encuentro registrado.");
                }
                case 7 -> {
                    String id = texto("ID torneo: "), ganador = texto("Codigo inscripcion ganadora: ");
                    torneos.finalizar(id, ganador);
                    System.out.println("Finalizado. Premio en efectivo: " + torneos.buscar(id).getPremioPagado());
                }
                case 8 -> inscribirAdministrador();
                case 9 -> {
                    historias.trasladarProducto(texto("Tienda origen: "),texto("Tienda destino: "),texto("Codigo producto: "));
                    System.out.println("Traslado registrado.");
                }
                case 10 -> { historias.aprobarSnack(texto("Codigo sugerencia: ")); System.out.println("Producto habilitado para venta."); }
                case 11 -> { historias.rechazarSnack(texto("Codigo sugerencia: ")); System.out.println("Sugerencia rechazada."); }
                case 12 -> {
                    HistoriasOficiales.Sugerencia s = historias.obtenerSugerencia(texto("Codigo sugerencia: "));
                    System.out.println(s.codigo + " / " + s.nombre + " / " + s.getEstado()
                            + " / alergenos " + s.alergenos + " / precio " + s.precio);
                }
                case 13 -> { historias.definirPrecio(texto("Codigo producto: "), decimal("Precio: ")); System.out.println("Precio actualizado."); }
                case 14 -> {
                    if (entero("1 Crear tienda; 2 Agregar producto a tienda: ") == 1)
                        historias.crearTienda(texto("Nombre tienda: "));
                    else historias.agregarProducto(texto("Tienda: "), texto("Codigo producto: "));
                }
                case 15 -> {
                    datos.asignarTurno(texto("Login del empleado: "), fecha("Fecha del turno"));
                    System.out.println("Turno asignado.");
                }
                case 16 -> mostrarCompras(null);
                case 17 -> {
                    for (HistoriasOficiales.Reserva r : historias.getReservas())
                        System.out.println(r.socio + " | " + r.instalacion + " | " + r.fecha + " " + r.inicio + "-" + r.fin);
                }
                case 18 -> historial();
                case 19 -> System.out.println(historias.productosDe(texto("Tienda: ")));
                default -> System.out.println("Opcion no reconocida.");
            }
        }
        private void inscribirAdministrador() throws Exception {
            String torneoId = texto("ID torneo: ");
            GestorTorneos.Torneo t = torneos.buscar(torneoId);
            String codigo = texto("Codigo inscripcion: ");
            List<GestorTorneos.Participante> miembros = new ArrayList<>();
            int total = t.formato == GestorTorneos.Formato.DOBLES ? 2 : 1;
            for (int n=1;n<=total;n++) {
                String id = texto("Codigo participante/equipo " + n + ": ");
                String nombre = texto("Nombre participante/equipo: ");
                GestorTorneos.Rol rol = GestorTorneos.Rol.valueOf(
                        texto("Rol ESTUDIANTE/EXTERNO/EQUIPO/SOCIO: ").toUpperCase(Locale.ROOT));
                String categoria = texto("Categoria o nivel: ");
                List<String> socios = new ArrayList<>();
                if (rol == GestorTorneos.Rol.EQUIPO) {
                    String lista = texto("Integrantes socios separados por coma, o NINGUNO: ");
                    if (!lista.equalsIgnoreCase("NINGUNO"))
                        for (String v : lista.split(",")) if (!v.isBlank()) socios.add(v.trim());
                }
                miembros.add(new GestorTorneos.Participante(id, nombre, categoria, rol,
                        torneos.tienePreferencia(torneoId,id), null, socios));
            }
            GestorTorneos.Inscripcion inscrito = torneos.inscribir(torneoId, codigo, miembros);
            System.out.println("Inscripcion completada; pago: " + inscrito.valorInscripcion);
        }
        private void socio(int opcion, DatosProyecto2.Cuenta cuenta) throws Exception {
            switch (opcion) {
                case 1 -> listar();
                case 2 -> inscribirCuenta(cuenta);
                case 3 -> retirar(cuenta);
                case 4 -> {
                    HistoriasOficiales.Reserva r = historias.reservar(cuenta.login,
                        texto("Instalacion (tenis-1, padel-1, padel-2): "), fecha("Fecha de reserva"),
                        hora("Hora inicial"), hora("Hora final"), entero("Cantidad de jugadores: "),
                        texto("Modalidad SENCILLOS/DOBLES: "));
                    System.out.println("Reserva confirmada: " + r.instalacion + " " + r.fecha);
                }
                case 5 -> {
                    for (GestorTorneos.Bono bono : torneos.consultarBonos(cuenta.login))
                        System.out.println(bono.codigo + " | descuento " + (bono.descuento*100) + "% | usado: " + bono.estaUsado());
                }
                case 6 -> comprar(cuenta, texto("Tienda: "));
                case 7 -> comprar(cuenta, "cafeteria");
                case 8 -> mostrarCompras(cuenta.login);
                case 9 -> historial();
                default -> System.out.println("Opcion no reconocida.");
            }
        }
        private void comprar(DatosProyecto2.Cuenta cuenta, String area) throws Exception {
            Map<String,Integer> productos = new LinkedHashMap<>();
            int cantidadTipos=entero("Cantidad de productos diferentes: ");
            if(cantidadTipos<1)throw new IllegalArgumentException("Compra vacia");
            for (int n=0;n<cantidadTipos;n++) productos.merge(texto("Codigo producto: "), entero("Unidades: "), Integer::sum);
            String bono=si("Desea aplicar bono de torneo")?texto("Codigo bono: "):null;
            boolean descuento=si("Esta compra tiene otro descuento");
            HistoriasOficiales.Compra compra = historias.comprar(cuenta.login, area, productos, bono, descuento, torneos);
            System.out.printf("Compra %s: subtotal %.2f, total %.2f. Bono: %s%n",
                compra.numero, compra.subtotal, compra.total, compra.bonoAplicado.isBlank()?"No":compra.bonoAplicado);
        }
        private void mostrarCompras(String login) {
            for(HistoriasOficiales.Compra c : historias.getCompras())
                if(login==null||c.socio.equalsIgnoreCase(login))
                    System.out.println(c.numero+" / "+c.socio+" / "+c.area+" / "+c.detalle+
                            " / subtotal "+c.subtotal+" / total "+c.total);
        }
        private void empleado(int opcion, DatosProyecto2.Cuenta cuenta) throws Exception {
            switch (opcion) {
                case 1 -> listar();
                case 2 -> inscribirCuenta(cuenta);
                case 3 -> retirar(cuenta);
                case 4 -> {
                    String codigo=texto("Codigo snack: "), nombre=texto("Nombre snack: ");
                    String declaracion=texto("Alergenos separados por coma o NINGUNO: ");
                    Set<String> alergenos=new LinkedHashSet<>();
                    for(String a:declaracion.split(","))if(!a.isBlank())alergenos.add(a.trim().toUpperCase(Locale.ROOT));
                    historias.proponerSnack(cuenta.login,codigo,nombre,alergenos,decimal("Precio propuesto: "));
                    System.out.println("Sugerencia pendiente enviada al administrador.");
                }
                case 5 -> historial();
                default -> System.out.println("Opcion no reconocida.");
            }
        }
        private void visitante(int opcion, DatosProyecto2.Cuenta cuenta) throws Exception {
            switch (opcion) {
                case 1 -> listar();
                case 2 -> inscribirCuenta(cuenta);
                case 3 -> retirar(cuenta);
                case 4 -> historial();
                default -> System.out.println("Opcion no reconocida.");
            }
        }
        private void retirar(DatosProyecto2.Cuenta cuenta) throws Exception {
            torneos.retirar(texto("ID torneo: "),cuenta.login);
            System.out.println("Retiro confirmado, el cupo se libero.");
        }
        private void inscribirCuenta(DatosProyecto2.Cuenta cuenta) throws Exception {
            String id=texto("ID torneo: ");
            GestorTorneos.Torneo t=torneos.buscar(id);
            List<GestorTorneos.Participante> participantes=new ArrayList<>();
            participantes.add(new GestorTorneos.Participante(cuenta.login,cuenta.login,cuenta.categoria,
                cuenta.rolTorneo,torneos.tienePreferencia(id,cuenta.login),datos.turnos(cuenta.login),null));
            if (t.formato == GestorTorneos.Formato.DOBLES) {
                String pareja = texto("Login o codigo de la pareja: ");
                participantes.add(new GestorTorneos.Participante(pareja,pareja,cuenta.categoria,
                    GestorTorneos.Rol.SOCIO,torneos.tienePreferencia(id,pareja),null,null));
            }
            GestorTorneos.Inscripcion i=torneos.inscribir(id,cuenta.login,participantes);
            System.out.printf("Inscripcion %s registrada. Valor a pagar: %.2f%n",i.codigo,i.valorInscripcion);
        }
    }
}
