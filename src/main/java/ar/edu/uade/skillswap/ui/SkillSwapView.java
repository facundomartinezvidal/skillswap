package ar.edu.uade.skillswap.ui;

import ar.edu.uade.skillswap.controllers.CalificacionController;
import ar.edu.uade.skillswap.controllers.ContratacionOperaciones;
import ar.edu.uade.skillswap.controllers.FranjaVista;
import ar.edu.uade.skillswap.controllers.PasoSiguienteVista;
import ar.edu.uade.skillswap.controllers.PublicacionOperaciones;

import ar.edu.uade.skillswap.model.Models;

import ar.edu.uade.skillswap.controllers.UiControllers;
import ar.edu.uade.skillswap.model.Models.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.List;
import java.util.function.Consumer;

import static ar.edu.uade.skillswap.ui.Theme.*;

public final class SkillSwapView extends JPanel {
    private static final String USUARIO_POR_DEFECTO="user-mateo";
    // El login simulado entrega el userId activo; la vista lo pasa a cada operación.
    private final String USER_ID;
    private final Models.Perfil perfilActivo;
    private final CalificacionController calificaciones;
    private final ContratacionOperaciones operaciones;
    private final PublicacionOperaciones publicacionOperaciones;
    private final List<String> usuarios;
    private final Consumer<String> cambiarUsuario;
    private JButton coins;
    private final UiControllers controllers;
    private final JPanel content=new JPanel(new BorderLayout());
    private final JLabel pageTitle=label("Inicio",H1,TEXT);
    private final JLabel badge=label("",new Font("SansSerif",Font.BOLD,10),Color.WHITE);

    public SkillSwapView(UiControllers controllers) {
        this(controllers, USUARIO_POR_DEFECTO);
    }

    public SkillSwapView(UiControllers controllers, String userId) {
        this(controllers, userId, null, null);
    }

    /**
     * Las operaciones del equipo llegan aparte porque {@link UiControllers} es de la cátedra. Pueden ser null:
     * en ese caso no se muestran los botones Calificar, Ver historial ni Volver a solicitar, y Solicitar mentoría
     * no pide franja, mensaje ni objetivo.
     */
    public SkillSwapView(UiControllers controllers, String userId, CalificacionController calificaciones,
                         ContratacionOperaciones operaciones) {
        this(controllers, userId, calificaciones, operaciones, null, List.of(), null);
    }

    /**
     * Login simulado: con {@code cambiarUsuario} la barra lateral muestra el botón Cambiar usuario, que ofrece
     * {@code usuarios} y le pasa el elegido a quien armó la vista (main), que la vuelve a crear con ese userId.
     */
    public SkillSwapView(UiControllers controllers, String userId, CalificacionController calificaciones,
                         ContratacionOperaciones operaciones, PublicacionOperaciones publicacionOperaciones,
                         List<String> usuarios, Consumer<String> cambiarUsuario) {
        this.controllers=controllers;
        this.publicacionOperaciones=publicacionOperaciones;
        this.usuarios=List.copyOf(usuarios);
        this.cambiarUsuario=cambiarUsuario;
        this.calificaciones=calificaciones;
        this.operaciones=operaciones;
        this.USER_ID=userId;
        this.perfilActivo=controllers.perfiles().verPerfil(userId); setLayout(new BorderLayout()); setBackground(BG);
        add(sidebar(),BorderLayout.WEST); add(shell(),BorderLayout.CENTER); showHome();
    }

    private JComponent sidebar() {
        JPanel side=new JPanel(); side.setBackground(NAVY); side.setPreferredSize(new Dimension(228,720)); side.setLayout(new BoxLayout(side,BoxLayout.Y_AXIS)); side.setBorder(new EmptyBorder(24,16,18,16));
        JLabel logo=label("SkillSwap",new Font("SansSerif",Font.BOLD,25),Color.WHITE); logo.setAlignmentX(LEFT_ALIGNMENT); side.add(logo);
        JLabel sub=label("Aprendé. Compartí. Crecé.",SMALL,new Color(177,200,216)); sub.setAlignmentX(LEFT_ALIGNMENT); side.add(sub); side.add(Box.createVerticalStrut(28));
        side.add(nav("⌂  Inicio",this::showHome)); side.add(nav("⌕  Explorar",this::showExplore)); side.add(nav("▣  Mis publicaciones",this::showListings)); side.add(nav("✓  Contrataciones",this::showContracts)); side.add(nav("◈  SkillCoins",this::showWallet)); side.add(nav("●  Notificaciones",this::showNotifications)); side.add(nav("♙  Mi perfil",this::showProfile)); side.add(Box.createVerticalGlue());
        if(cambiarUsuario!=null&&!usuarios.isEmpty()) side.add(nav("⇄  Cambiar usuario",this::elegirUsuario));
        JPanel user=new JPanel(new BorderLayout(10,0)); user.setOpaque(false); user.setBorder(new EmptyBorder(8,6,4,4)); user.setMaximumSize(new Dimension(Integer.MAX_VALUE,56)); user.setAlignmentX(LEFT_ALIGNMENT);
        JLabel avatar=avatar(iniciales(),36); user.add(avatar,BorderLayout.WEST); JPanel txt=new JPanel();txt.setOpaque(false);txt.setLayout(new BoxLayout(txt,BoxLayout.Y_AXIS));txt.add(label(perfilActivo.nombre(),H3,Color.WHITE));txt.add(label(perfilActivo.rol(),SMALL,new Color(177,200,216)));user.add(txt);side.add(user); return side;
    }

    /**
     * Muestra los usuarios de la demo con su rol en la demo y avisa el elegido; si es el mismo, no hace nada.
     * Todos los perfiles son PROFESIONAL: el rol que se muestra sale de si el usuario ofrece publicaciones
     * (proveedor) o solo contrata (cliente). Debajo del combo, la bio del usuario seleccionado.
     */
    private void elegirUsuario(){
        List<Models.Perfil> perfiles=usuarios.stream().map(id->controllers.perfiles().verPerfil(id)).toList();
        JComboBox<String> combo=new JComboBox<>(perfiles.stream().map(p->p.nombre()+"  ·  "+rolEnLaDemo(p.id())+"  ("+p.id()+")").toArray(String[]::new));
        JLabel bio=label("",SMALL,MUTED);
        combo.addActionListener(e->bio.setText(perfiles.get(combo.getSelectedIndex()).bio()));
        combo.setSelectedIndex(Math.max(0,usuarios.indexOf(USER_ID)));
        JPanel form=new JPanel(new GridLayout(0,1,4,5));form.add(new JLabel("Entrar como"));form.add(combo);form.add(bio);
        if(JOptionPane.showConfirmDialog(this,form,"Cambiar usuario",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)!=JOptionPane.OK_OPTION)return;
        String elegido=usuarios.get(combo.getSelectedIndex());
        if(!elegido.equals(USER_ID))cambiarUsuario.accept(elegido);
    }

    private String rolEnLaDemo(String userId){
        int propias=controllers.publicaciones().listarPropias(userId).size();
        return propias>0?"Profesional, ofrece "+propias+(propias==1?" publicación":" publicaciones"):"Cliente, contrata mentorías";
    }

    private JButton nav(String text,Runnable action){JButton b=new JButton(text);b.setMaximumSize(new Dimension(Integer.MAX_VALUE,44));b.setHorizontalAlignment(SwingConstants.LEFT);b.setForeground(new Color(224,234,241));b.setBackground(NAVY);b.setBorder(new EmptyBorder(10,12,10,12));b.setFocusPainted(false);b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));b.addActionListener(e->action.run());return b;}

    private JComponent shell(){JPanel shell=new JPanel(new BorderLayout());shell.setBackground(BG);JPanel top=new JPanel(new BorderLayout());top.setBackground(Color.WHITE);top.setBorder(new EmptyBorder(16,26,16,26));top.add(pageTitle,BorderLayout.WEST);JPanel actions=new JPanel(new FlowLayout(FlowLayout.RIGHT,10,0));actions.setOpaque(false);badge.setOpaque(true);badge.setBackground(DANGER);badge.setBorder(new EmptyBorder(5,8,5,8));actions.add(badge);coins=button("◈  "+controllers.wallet().consultarSaldo(USER_ID)+" SkillCoins",false);coins.addActionListener(e->showWallet());actions.add(coins);top.add(actions,BorderLayout.EAST);shell.add(top,BorderLayout.NORTH);content.setBackground(BG);shell.add(content);return shell;}

    private void page(String title,JComponent view){pageTitle.setText(title);long unread=controllers.notificaciones().listarNotificaciones(USER_ID).stream().filter(n->!n.leida()).count();badge.setText(unread+" NUEVAS");if(coins!=null)coins.setText("◈  "+controllers.wallet().consultarSaldo(USER_ID)+" SkillCoins");content.removeAll();JScrollPane sp=new JScrollPane(view);sp.setBorder(null);sp.getVerticalScrollBar().setUnitIncrement(18);sp.getViewport().setBackground(BG);content.add(sp);content.revalidate();content.repaint();}

    private JPanel base(){JPanel p=new JPanel();p.setBackground(BG);p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));p.setBorder(new EmptyBorder(24,26,28,26));return p;}
    private void addSection(JPanel p,String title,String subtitle){JLabel h=label(title,H2,TEXT);h.setAlignmentX(LEFT_ALIGNMENT);p.add(h);if(subtitle!=null){JLabel s=label(subtitle,BODY,MUTED);s.setAlignmentX(LEFT_ALIGNMENT);p.add(Box.createVerticalStrut(3));p.add(s);}p.add(Box.createVerticalStrut(14));}

    private void showHome(){JPanel p=base();JPanel hero=new Theme.RoundedPanel(22,NAVY_2);hero.setLayout(new BorderLayout(22,0));hero.setBorder(new EmptyBorder(24,26,24,26));hero.setMaximumSize(new Dimension(Integer.MAX_VALUE,155));JPanel words=new JPanel();words.setOpaque(false);words.setLayout(new BoxLayout(words,BoxLayout.Y_AXIS));words.add(label("Hola, "+perfilActivo.nombre().split(" ")[0]+" 👋",new Font("SansSerif",Font.BOLD,25),Color.WHITE));words.add(Box.createVerticalStrut(7));words.add(label("¿Qué habilidad querés desbloquear hoy?",BODY,new Color(213,228,238)));words.add(Box.createVerticalStrut(16));JTextField search=new JTextField();search.putClientProperty("JTextField.placeholderText","Buscar mentorías, servicios o habilidades...");search.setBorder(new EmptyBorder(11,13,11,13));search.addActionListener(e->showExplore(search.getText()));words.add(search);hero.add(words);JPanel stat=new JPanel();stat.setOpaque(false);stat.setLayout(new BoxLayout(stat,BoxLayout.Y_AXIS));stat.add(label("TU PROGRESO",SMALL,new Color(169,202,219)));stat.add(label(String.valueOf(perfilActivo.proyectosCompletados()),new Font("SansSerif",Font.BOLD,34),Color.WHITE));stat.add(label("proyectos completados",SMALL,new Color(213,228,238)));hero.add(stat,BorderLayout.EAST);hero.setAlignmentX(LEFT_ALIGNMENT);p.add(hero);p.add(Box.createVerticalStrut(26));addSection(p,"Recomendado para vos","Selecciones basadas en tus habilidades e intereses");p.add(cardGrid(controllers.busqueda().recomendar(USER_ID)));p.add(Box.createVerticalStrut(26));addSection(p,"Explorá por categoría",null);JPanel cats=new JPanel(new FlowLayout(FlowLayout.LEFT,10,0));cats.setOpaque(false);for(String c:controllers.busqueda().categorias())if(!c.equals("Todas")){JButton b=button(c,false);b.addActionListener(e->showExploreCategory(c));cats.add(b);}cats.setAlignmentX(LEFT_ALIGNMENT);p.add(cats);page("Inicio",p);}

    private void showExplore(){showExplore("");}
    private void showExplore(String initial){showExploreWith(initial,"Todas");}
    private void showExploreCategory(String category){showExploreWith("",category);}
    private void showExploreWith(String initial,String category){JPanel p=base();JPanel filters=card(new FlowLayout(FlowLayout.LEFT,10,0));filters.setAlignmentX(LEFT_ALIGNMENT);JTextField q=new JTextField(initial,24);q.setBorder(new EmptyBorder(10,12,10,12));JComboBox<String> cat=new JComboBox<>(controllers.busqueda().categorias().toArray(String[]::new));cat.setSelectedItem(category);JComboBox<String> order=new JComboBox<>(new String[]{"Relevancia","Mejor reputación","Menor precio","Más reciente"});JButton go=button("Buscar",true);filters.add(q);filters.add(cat);filters.add(order);filters.add(go);p.add(filters);p.add(Box.createVerticalStrut(22));JPanel results=new JPanel();results.setOpaque(false);results.setLayout(new BoxLayout(results,BoxLayout.Y_AXIS));Runnable refresh=()->{results.removeAll();List<Publicacion> list=controllers.busqueda().buscar(new FiltrosBusqueda(q.getText(),(String)cat.getSelectedItem(),null,0,(String)order.getSelectedItem()));addSection(results,list.size()+" resultados","Mentorías y servicios disponibles");results.add(cardGrid(list));results.revalidate();results.repaint();};go.addActionListener(e->refresh.run());q.addActionListener(e->refresh.run());p.add(results);refresh.run();page("Explorar",p);}

    private JPanel cardGrid(List<Publicacion> list){JPanel grid=new JPanel(new GridLayout(0,3,14,14));grid.setOpaque(false);grid.setAlignmentX(LEFT_ALIGNMENT);for(Publicacion x:list)grid.add(listingCard(x));return grid;}
    private JComponent listingCard(Publicacion x){JPanel c=card(new BorderLayout(0,12));JPanel top=new JPanel(new BorderLayout());top.setOpaque(false);top.add(pill(x.categoria(),TEAL),BorderLayout.WEST);top.add(label("★ "+x.reputacion(),H3,GOLD),BorderLayout.EAST);c.add(top,BorderLayout.NORTH);JPanel mid=new JPanel();mid.setOpaque(false);mid.setLayout(new BoxLayout(mid,BoxLayout.Y_AXIS));JLabel t=label("<html><div style='width:210px'>"+x.titulo()+"</div></html>",H3,TEXT);mid.add(t);mid.add(Box.createVerticalStrut(7));mid.add(label("Por "+x.proveedor(),SMALL,MUTED));mid.add(Box.createVerticalStrut(9));mid.add(label("<html><div style='width:215px;color:#677480'>"+x.descripcion()+"</div></html>",BODY,MUTED));mid.add(Box.createVerticalStrut(9));mid.add(label(x.modalidad()+"  ·  "+x.duracionMinutos()+" min",SMALL,MUTED));c.add(mid);JPanel bottom=new JPanel(new BorderLayout());bottom.setOpaque(false);bottom.add(label(x.precio()+" ◈",new Font("SansSerif",Font.BOLD,20),NAVY),BorderLayout.WEST);JButton detail=button("Ver detalle",true);detail.addActionListener(e->showDetail(x));bottom.add(detail,BorderLayout.EAST);c.add(bottom,BorderLayout.SOUTH);return c;}

    private void showDetail(Publicacion x){JPanel p=base();JButton back=button("← Volver a explorar",false);back.setAlignmentX(LEFT_ALIGNMENT);back.addActionListener(e->showExplore());p.add(back);p.add(Box.createVerticalStrut(18));JPanel c=card(new BorderLayout(28,0));c.setMaximumSize(new Dimension(Integer.MAX_VALUE,390));JPanel info=new JPanel();info.setOpaque(false);info.setLayout(new BoxLayout(info,BoxLayout.Y_AXIS));info.add(pill(x.categoria(),TEAL));info.add(Box.createVerticalStrut(14));info.add(label("<html><div style='width:540px'>"+x.titulo()+"</div></html>",H1,TEXT));info.add(Box.createVerticalStrut(10));info.add(label("★ "+x.reputacion()+"  ·  "+x.proveedor()+"  ·  "+x.modalidad(),H3,MUTED));info.add(Box.createVerticalStrut(22));info.add(label("Sobre esta propuesta",H2,TEXT));info.add(Box.createVerticalStrut(8));info.add(label("<html><div style='width:550px'>"+x.descripcion()+"</div></html>",BODY,MUTED));info.add(Box.createVerticalStrut(16));info.add(label("Duración: "+x.duracionMinutos()+" minutos",BODY,TEXT));c.add(info);JPanel buy=new Theme.RoundedPanel(16,new Color(239,247,246));buy.setLayout(new BoxLayout(buy,BoxLayout.Y_AXIS));buy.setBorder(new EmptyBorder(22,22,22,22));buy.add(label("INVERSIÓN",SMALL,MUTED));buy.add(label(x.precio()+" SkillCoins",new Font("SansSerif",Font.BOLD,25),NAVY));buy.add(Box.createVerticalStrut(12));buy.add(label("Saldo disponible: "+controllers.wallet().consultarSaldo(USER_ID),SMALL,MUTED));buy.add(Box.createVerticalStrut(20));JButton req=button("Solicitar mentoría",true);req.addActionListener(e->solicitar(x));buy.add(req);c.add(buy,BorderLayout.EAST);c.setAlignmentX(LEFT_ALIGNMENT);p.add(c);page("Detalle",p);}

    private void showListings(){JPanel p=base();JPanel title=new JPanel(new BorderLayout());title.setOpaque(false);title.add(label("Gestioná tus publicaciones",H2,TEXT));JButton create=button("+ Nueva publicación",true);create.addActionListener(e->createListing());title.add(create,BorderLayout.EAST);title.setMaximumSize(new Dimension(Integer.MAX_VALUE,45));title.setAlignmentX(LEFT_ALIGNMENT);p.add(title);p.add(Box.createVerticalStrut(15));List<Publicacion> own=controllers.publicaciones().listarPropias(USER_ID);if(own.isEmpty()){p.add(empty("Todavía no publicaste servicios","Creá tu primera propuesta para comenzar a compartir tu talento."));}else for(Publicacion x:own){JPanel row=card(new BorderLayout(12,0));row.setMaximumSize(new Dimension(Integer.MAX_VALUE,90));row.add(label(x.titulo(),H3,TEXT),BorderLayout.CENTER);row.add(pill(x.estado().name(),x.estado()==EstadoPublicacion.PUBLICADA?TEAL:MUTED),BorderLayout.WEST);JButton toggle=button(x.estado()==EstadoPublicacion.PUBLICADA?"Pausar":"Publicar",false);toggle.addActionListener(e->intentar(()->{controllers.publicaciones().cambiarEstado(USER_ID,x.id(),x.estado()==EstadoPublicacion.PUBLICADA?EstadoPublicacion.PAUSADA:EstadoPublicacion.PUBLICADA);showListings();}));JPanel acciones=new JPanel(new FlowLayout(FlowLayout.RIGHT,8,0));acciones.setOpaque(false);if(publicacionOperaciones!=null&&x.estado()==EstadoPublicacion.BORRADOR){JButton franjas=button("Franjas",false);franjas.addActionListener(e->editarFranjas(x));acciones.add(franjas);}acciones.add(toggle);row.add(acciones,BorderLayout.EAST);p.add(row);p.add(Box.createVerticalStrut(10));}page("Mis publicaciones",p);}

    private void createListing(){JTextField title=new JTextField(26);JTextArea desc=new JTextArea(4,26);desc.setLineWrap(true);desc.setWrapStyleWord(true);JComboBox<String> cat=new JComboBox<>(controllers.busqueda().categorias().stream().filter(c->!c.equals("Todas")).toArray(String[]::new));JPanel form=formulario();campo(form,"Título",title);campo(form,"Descripción",new JScrollPane(desc));campo(form,"Categoría",cat);JSpinner precio=campoPrecio();campo(form,"Precio (SkillCoins)",precio);JSpinner duracion=new JSpinner(new SpinnerNumberModel(60,15,480,15));JSpinner cupo=new JSpinner(new SpinnerNumberModel(1,1,50,1));DefaultListModel<LocalDateTime> franjas=new DefaultListModel<>();franjas.addElement(manianaEnPunto());if(publicacionOperaciones!=null){campo(form,"Duración (minutos)",duracion);campo(form,"Lugares por franja",cupo);campo(form,"Franjas",editorDeFranjas(franjas,duracion));}if(confirmarSinSuperposiciones(form,"Nueva publicación",franjas,duracion)&&!title.getText().isBlank()){intentar(()->{if(publicacionOperaciones!=null)publicacionOperaciones.crearBorrador(USER_ID,title.getText(),desc.getText(),(String)cat.getSelectedItem(),Modalidad.REMOTA,(Integer)duracion.getValue(),(Integer)precio.getValue(),(Integer)cupo.getValue(),java.util.Collections.list(franjas.elements()));else controllers.publicaciones().crearBorrador(USER_ID,title.getText(),desc.getText(),(String)cat.getSelectedItem(),Modalidad.REMOTA,60,(Integer)precio.getValue());message("Borrador creado","La publicación quedó lista para que tus alumnos conecten su flujo real.");showListings();});}}

    private void showContracts(){JPanel p=base();addSection(p,"Tus contrataciones","Avanzá el flujo para probar los estados del sistema");for(Contratacion x:controllers.contrataciones().listarContrataciones(USER_ID)){PasoSiguienteVista paso=operaciones==null?null:operaciones.pasoSiguiente(USER_ID,x.id());JPanel row=card(new BorderLayout(16,0));row.setMaximumSize(new Dimension(Integer.MAX_VALUE,paso==null?105:125));JPanel txt=new JPanel();txt.setOpaque(false);txt.setLayout(new BoxLayout(txt,BoxLayout.Y_AXIS));txt.add(label(x.titulo(),H3,TEXT));txt.add(Box.createVerticalStrut(5));txt.add(label(x.contraparte()+"  ·  "+x.fecha().format(DateTimeFormatter.ofPattern("dd/MM · HH:mm"))+(paso==null?"":"  ·  "+x.precio()+" SkillCoins  ·  Sos "+paso.rol().toLowerCase()),BODY,MUTED));if(paso!=null&&!paso.detalle().isBlank()){txt.add(Box.createVerticalStrut(4));txt.add(label(paso.detalle(),SMALL,TEXT));}row.add(txt,BorderLayout.CENTER);row.add(pill(x.estado().name(),x.estado()==EstadoContratacion.COMPLETADA?TEAL:NAVY_2),BorderLayout.WEST);JPanel actions=new JPanel(new FlowLayout(FlowLayout.RIGHT,8,0));actions.setOpaque(false);if(x.estado()==EstadoContratacion.COMPLETADA&&operaciones!=null){JButton receipt=button("Comprobante",false);receipt.addActionListener(e->intentar(()->verComprobante(x)));actions.add(receipt);}if(x.estado()==EstadoContratacion.COMPLETADA&&calificaciones!=null){JButton rate=button("Calificar",true);rate.addActionListener(e->calificar(x));actions.add(rate);}boolean terminada=x.estado()==EstadoContratacion.RECHAZADA||x.estado()==EstadoContratacion.CANCELADA;if(terminada&&operaciones!=null){JButton hist=button("Ver historial",false);hist.addActionListener(e->intentar(()->verHistorial(x)));actions.add(hist);}if(x.estado()==EstadoContratacion.RECHAZADA&&operaciones!=null&&operaciones.esCliente(USER_ID,x.id())){JButton again=button("Volver a solicitar",true);again.addActionListener(e->intentar(()->{operaciones.volverASolicitar(USER_ID,x.id());message("¡Solicitud enviada!","Se creó una solicitud nueva para la misma publicación.");showContracts();}));actions.add(again);}if(x.estado()!=EstadoContratacion.COMPLETADA&&!terminada){if(paso==null||paso.accion()!=null){JButton next=button(paso==null?"Avanzar estado":paso.accion(),true);next.addActionListener(e->intentar(()->{controllers.contrataciones().avanzar(USER_ID,x.id());showContracts();}));actions.add(next);}if(paso!=null)accionesExtra(x,paso,actions);JButton cancel=button("Cancelar",false);cancel.addActionListener(e->intentar(()->{controllers.contrataciones().cancelar(USER_ID,x.id());showContracts();}));actions.add(cancel);}row.add(actions,BorderLayout.EAST);p.add(row);p.add(Box.createVerticalStrut(11));}page("Contrataciones",p);}

    private void showWallet(){JPanel p=base();JPanel balance=new Theme.RoundedPanel(22,NAVY_2);balance.setLayout(new BorderLayout());balance.setBorder(new EmptyBorder(24,26,24,26));balance.setMaximumSize(new Dimension(Integer.MAX_VALUE,120));JPanel btxt=new JPanel();btxt.setOpaque(false);btxt.setLayout(new BoxLayout(btxt,BoxLayout.Y_AXIS));btxt.add(label("SALDO DISPONIBLE",SMALL,new Color(184,209,222)));btxt.add(label(controllers.wallet().consultarSaldo(USER_ID)+" SkillCoins",new Font("SansSerif",Font.BOLD,30),Color.WHITE));balance.add(btxt);JButton add=button("+ Acreditar demo",true);add.addActionListener(e->intentar(()->{controllers.wallet().acreditarDemo(USER_ID,500);showWallet();}));balance.add(add,BorderLayout.EAST);balance.setAlignmentX(LEFT_ALIGNMENT);p.add(balance);p.add(Box.createVerticalStrut(24));addSection(p,"Movimientos recientes",null);for(Movimiento m:controllers.wallet().listarMovimientos(USER_ID)){JPanel row=card(new BorderLayout());row.setMaximumSize(new Dimension(Integer.MAX_VALUE,72));JPanel t=new JPanel();t.setOpaque(false);t.setLayout(new BoxLayout(t,BoxLayout.Y_AXIS));t.add(label(m.concepto(),H3,TEXT));t.add(label(m.fecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy · HH:mm")),SMALL,MUTED));row.add(t);row.add(label((m.importe()>0?"+":"")+m.importe()+" ◈",H3,m.importe()>0?TEAL:DANGER),BorderLayout.EAST);p.add(row);p.add(Box.createVerticalStrut(8));}page("SkillCoins",p);}

    private void showNotifications(){JPanel p=base();JPanel head=new JPanel(new BorderLayout());head.setOpaque(false);head.add(label("Centro de notificaciones",H2,TEXT));JButton all=button("Marcar todas como leídas",false);all.addActionListener(e->intentar(()->{controllers.notificaciones().marcarTodasLeidas(USER_ID);showNotifications();}));head.add(all,BorderLayout.EAST);head.setMaximumSize(new Dimension(Integer.MAX_VALUE,42));head.setAlignmentX(LEFT_ALIGNMENT);p.add(head);p.add(Box.createVerticalStrut(15));for(Notificacion n:controllers.notificaciones().listarNotificaciones(USER_ID)){JPanel row=card(new BorderLayout(14,0));row.setMaximumSize(new Dimension(Integer.MAX_VALUE,90));JLabel dot=label(n.leida()?"○":"●",H2,n.leida()?BORDER:TEAL);row.add(dot,BorderLayout.WEST);JPanel t=new JPanel();t.setOpaque(false);t.setLayout(new BoxLayout(t,BoxLayout.Y_AXIS));t.add(label(n.titulo(),H3,TEXT));t.add(label(n.detalle(),BODY,MUTED));t.add(label(n.fecha().format(DateTimeFormatter.ofPattern("dd/MM · HH:mm")),SMALL,MUTED));row.add(t);if(!n.leida()){JButton read=button("Marcar leída",false);read.addActionListener(e->intentar(()->{controllers.notificaciones().marcarLeida(USER_ID,n.id());showNotifications();}));row.add(read,BorderLayout.EAST);}p.add(row);p.add(Box.createVerticalStrut(9));}page("Notificaciones",p);}

    private void showProfile(){Perfil x=controllers.perfiles().verPerfil(USER_ID);JPanel p=base();JPanel hero=card(new BorderLayout(20,0));hero.setMaximumSize(new Dimension(Integer.MAX_VALUE,180));hero.add(avatar(iniciales(),86),BorderLayout.WEST);JPanel data=new JPanel();data.setOpaque(false);data.setLayout(new BoxLayout(data,BoxLayout.Y_AXIS));data.add(label(x.nombre(),H1,TEXT));data.add(label(x.rol(),H3,TEAL_DARK));data.add(Box.createVerticalStrut(10));data.add(label("<html><div style='width:570px'>"+x.bio()+"</div></html>",BODY,MUTED));hero.add(data);JPanel stats=new JPanel(new GridLayout(3,1,0,4));stats.setOpaque(false);stats.add(label("★ "+x.reputacion()+" reputación",H3,GOLD));stats.add(label(x.resenas()+" reseñas",BODY,MUTED));stats.add(label(x.proyectosCompletados()+" proyectos",BODY,MUTED));hero.add(stats,BorderLayout.EAST);hero.setAlignmentX(LEFT_ALIGNMENT);p.add(hero);p.add(Box.createVerticalStrut(24));addSection(p,"Habilidades",null);JPanel tags=new JPanel(new FlowLayout(FlowLayout.LEFT,8,0));tags.setOpaque(false);for(String s:x.habilidades())tags.add(pill(s,NAVY_2));tags.setAlignmentX(LEFT_ALIGNMENT);p.add(tags);page("Mi perfil",p);}

    private JLabel pill(String text,Color color){JLabel l=label(text,new Font("SansSerif",Font.BOLD,10),Color.WHITE);l.setOpaque(true);l.setBackground(color);l.setBorder(new EmptyBorder(6,9,6,9));return l;}
    private JLabel avatar(String initials,int size){JLabel a=new JLabel(initials,SwingConstants.CENTER);a.setPreferredSize(new Dimension(size,size));a.setMinimumSize(new Dimension(size,size));a.setMaximumSize(new Dimension(size,size));a.setOpaque(true);a.setBackground(TEAL);a.setForeground(Color.WHITE);a.setFont(new Font("SansSerif",Font.BOLD,size/3));a.setBorder(BorderFactory.createLineBorder(new Color(255,255,255,100),2,true));return a;}
    private JComponent empty(String title,String subtitle){JPanel e=card(new GridBagLayout());e.setMaximumSize(new Dimension(Integer.MAX_VALUE,190));JPanel t=new JPanel();t.setOpaque(false);t.setLayout(new BoxLayout(t,BoxLayout.Y_AXIS));JLabel h=label(title,H2,TEXT);h.setAlignmentX(CENTER_ALIGNMENT);t.add(h);t.add(Box.createVerticalStrut(8));JLabel s=label(subtitle,BODY,MUTED);s.setAlignmentX(CENTER_ALIGNMENT);t.add(s);e.add(t);return e;}
    private void message(String title,String body){JOptionPane.showMessageDialog(this,body,title,JOptionPane.INFORMATION_MESSAGE);}

    private String iniciales() {
        StringBuilder iniciales = new StringBuilder();
        for (String parte : perfilActivo.nombre().split(" ")) {
            if (!parte.isBlank() && iniciales.length() < 2) {
                iniciales.append(Character.toUpperCase(parte.charAt(0)));
            }
        }
        return iniciales.toString();
    }

    /** Precio de la publicación nueva: de a 10 SkillCoins, arranca en 150 y no admite cero ni negativos. */
    private JSpinner campoPrecio() {
        JSpinner precio = new JSpinner(new SpinnerNumberModel(150, 1, 100_000, 10));
        ((JSpinner.DefaultEditor) precio.getEditor()).getTextField().setColumns(8);
        return precio;
    }

    /** Ejecuta una acción de un botón y, si el sistema la rechaza, muestra el motivo sobre la ventana. */
    private static final DateTimeFormatter FORMATO_FRANJA=DateTimeFormatter.ofPattern("EEE dd/MM · HH:mm",Locale.forLanguageTag("es-AR"));
    private static final DateTimeFormatter FORMATO_HORA=DateTimeFormatter.ofPattern("HH:mm");

    /**
     * La consigna pide que el cliente indique franja, mensaje y objetivo. Con las operaciones del equipo se
     * ofrecen solo las franjas que todavía se pueden pedir; sin ellas, queda el solicitar de la cátedra, que
     * elige la primera franja libre.
     */
    private void solicitar(Publicacion x){
        if(operaciones==null||publicacionOperaciones==null){
            intentar(()->{controllers.contrataciones().solicitar(USER_ID,x.id(),"Quiero profundizar y practicar");solicitudEnviada();});
            return;
        }
        intentar(()->elegirFranjaYSolicitar(x));
    }

    private void elegirFranjaYSolicitar(Publicacion x){
        List<FranjaVista> franjas=publicacionOperaciones.listarFranjas(USER_ID,x.id()).stream().filter(FranjaVista::disponible).toList();
        if(franjas.isEmpty()){message("Sin franjas disponibles","Esta publicación no tiene franjas con lugar. Probá más adelante.");return;}
        JComboBox<String> franja=new JComboBox<>(franjas.stream().map(this::textoFranja).toArray(String[]::new));
        JTextField objetivo=new JTextField();JTextArea mensaje=new JTextArea(3,26);mensaje.setLineWrap(true);mensaje.setWrapStyleWord(true);
        objetivo.setColumns(26);
        JPanel form=formulario();
        campo(form,"Franja horaria",franja);
        campo(form,"Objetivo",objetivo);
        campo(form,"Mensaje (opcional)",new JScrollPane(mensaje));
        if(JOptionPane.showConfirmDialog(this,form,"Solicitar: "+x.titulo(),JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)!=JOptionPane.OK_OPTION)return;
        String franjaId=franjas.get(franja.getSelectedIndex()).id();
        operaciones.solicitar(USER_ID,x.id(),franjaId,mensaje.getText(),objetivo.getText());solicitudEnviada();
    }

    private String textoFranja(FranjaVista f){
        return textoHorario(f)+"  ·  "+f.lugaresLibres()+(f.lugaresLibres()==1?" lugar":" lugares");
    }

    private static String textoHorario(FranjaVista f){return FORMATO_FRANJA.format(f.inicio())+"–"+FORMATO_HORA.format(f.fin());}

    /**
     * Las franjas las define el autor mientras la publicación está en borrador: muestra las cargadas y agrega
     * una nueva con la duración de la sesión. Superposiciones y demás reglas las valida el dominio.
     */
    private void editarFranjas(Publicacion x){
        intentar(()->{
            List<FranjaVista> franjas=publicacionOperaciones.listarFranjas(USER_ID,x.id());
            JPanel form=formulario();
            if(franjas.isEmpty())campo(form,"Cargadas",new JLabel("Todavía no cargaste franjas"));
            for(int i=0;i<franjas.size();i++)campo(form,i==0?"Cargadas":"",new JLabel(textoHorario(franjas.get(i))));
            LocalDateTime sugerida=franjas.isEmpty()?manianaEnPunto():franjas.get(franjas.size()-1).inicio().plusDays(1);
            JSpinner inicio=campoFechaHora(sugerida);
            campo(form,"Nueva franja",inicio);
            campo(form,"",new JLabel("Dura "+x.duracionMinutos()+" minutos, como la sesión"));
            if(JOptionPane.showConfirmDialog(this,form,"Franjas: "+x.titulo(),JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)!=JOptionPane.OK_OPTION)return;
            LocalDateTime desde=fechaHora(inicio);
            publicacionOperaciones.agregarFranja(USER_ID,x.id(),desde,desde.plusMinutes(x.duracionMinutos()));
            message("Franja agregada",FORMATO_FRANJA.format(desde)+"–"+FORMATO_HORA.format(desde.plusMinutes(x.duracionMinutos())));
            editarFranjas(x);
        });
    }

    /** Formulario de dos columnas (etiqueta y campo) donde cada fila toma la altura que necesita. */
    private static JPanel formulario(){JPanel form=new JPanel(new GridBagLayout());form.setBorder(new EmptyBorder(4,2,4,2));return form;}

    private static void campo(JPanel form,String etiqueta,JComponent valor){
        int fila=form.getComponentCount()/2;
        form.add(new JLabel(etiqueta),new GridBagConstraints(0,fila,1,1,0,0,GridBagConstraints.NORTHWEST,GridBagConstraints.NONE,new Insets(7,0,5,14),0,0));
        form.add(valor,new GridBagConstraints(1,fila,1,1,1,0,GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,new Insets(3,0,3,0),0,0));
    }

    /**
     * Las franjas del formulario de creación: un campo de fecha y hora con "+ Agregar", y la lista de las
     * agregadas con "Quitar". La siguiente se sugiere un día después de la última. Cada franja dura lo que
     * diga {@code duracion}; las reglas (como no superponerse) las valida el dominio al crear.
     */
    private JComponent editorDeFranjas(DefaultListModel<LocalDateTime> franjas,JSpinner duracion){
        JList<LocalDateTime> lista=new JList<>(franjas);lista.setVisibleRowCount(3);
        lista.setCellRenderer(new DefaultListCellRenderer(){
            @Override public Component getListCellRendererComponent(JList<?> l,Object valor,int i,boolean sel,boolean foco){
                LocalDateTime desde=(LocalDateTime)valor;
                return super.getListCellRendererComponent(l,textoHorario(desde,(Integer)duracion.getValue()),i,sel,foco);
            }
        });
        duracion.addChangeListener(e->lista.repaint());
        JSpinner nueva=campoFechaHora(manianaEnPunto().plusDays(1));
        JButton agregar=new JButton("+ Agregar");
        agregar.addActionListener(e->{
            LocalDateTime desde=fechaHora(nueva);int minutos=(Integer)duracion.getValue();
            for(int i=0;i<franjas.size();i++){
                if(seSuperponen(franjas.get(i),desde,minutos)){
                    JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor(agregar),"Se superpone con la franja del "+textoHorario(franjas.get(i),minutos)+".","Franja superpuesta",JOptionPane.WARNING_MESSAGE);
                    return;
                }
            }
            int lugar=0;while(lugar<franjas.size()&&franjas.get(lugar).isBefore(desde))lugar++;
            franjas.add(lugar,desde);
            nueva.setValue(Date.from(desde.plusDays(1).atZone(ZoneId.systemDefault()).toInstant()));
        });
        JButton quitar=new JButton("Quitar");
        quitar.addActionListener(e->{int i=lista.getSelectedIndex();if(i>=0)franjas.remove(i);});
        JPanel alta=new JPanel(new BorderLayout(6,0));alta.setOpaque(false);alta.add(nueva);alta.add(agregar,BorderLayout.EAST);
        JPanel cargadas=new JPanel(new BorderLayout(6,0));cargadas.setOpaque(false);cargadas.add(new JScrollPane(lista));
        JPanel botonQuitar=new JPanel(new BorderLayout());botonQuitar.setOpaque(false);botonQuitar.add(quitar,BorderLayout.NORTH);cargadas.add(botonQuitar,BorderLayout.EAST);
        quitar.setPreferredSize(agregar.getPreferredSize());
        JPanel editor=new JPanel(new BorderLayout(0,6));editor.setOpaque(false);editor.add(cargadas);editor.add(alta,BorderLayout.SOUTH);
        return editor;
    }

    /** Dos franjas de la misma duración se superponen si empiezan a menos de esa duración una de otra. */
    private static boolean seSuperponen(LocalDateTime una,LocalDateTime otra,int minutos){
        return Math.abs(java.time.Duration.between(una,otra).toMinutes())<minutos;
    }

    private static String textoHorario(LocalDateTime desde,int minutos){
        return FORMATO_FRANJA.format(desde)+"–"+FORMATO_HORA.format(desde.plusMinutes(minutos));
    }

    /**
     * Muestra el formulario hasta que se cancela o se confirma sin franjas superpuestas. Pueden quedar
     * superpuestas si se alarga la duración después de agregarlas: en ese caso avisa y el formulario vuelve
     * a abrirse con lo que ya estaba cargado.
     */
    private boolean confirmarSinSuperposiciones(JPanel form,String titulo,DefaultListModel<LocalDateTime> franjas,JSpinner duracion){
        while(true){
            if(JOptionPane.showConfirmDialog(this,form,titulo,JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)!=JOptionPane.OK_OPTION)return false;
            int minutos=(Integer)duracion.getValue();String conflicto=null;
            for(int i=1;i<franjas.size()&&conflicto==null;i++)
                if(seSuperponen(franjas.get(i-1),franjas.get(i),minutos))
                    conflicto=textoHorario(franjas.get(i-1),minutos)+" y "+textoHorario(franjas.get(i),minutos);
            if(conflicto==null)return true;
            JOptionPane.showMessageDialog(this,"Con "+minutos+" minutos de duración se superponen las franjas "+conflicto+". Quitá una o acortá la duración.","Franjas superpuestas",JOptionPane.WARNING_MESSAGE);
        }
    }

    private static LocalDateTime manianaEnPunto(){return LocalDateTime.now().truncatedTo(ChronoUnit.HOURS).plusDays(1);}

    private static JSpinner campoFechaHora(LocalDateTime inicial){
        JSpinner campo=new JSpinner(new SpinnerDateModel(Date.from(inicial.atZone(ZoneId.systemDefault()).toInstant()),null,null,Calendar.MINUTE));
        campo.setEditor(new JSpinner.DateEditor(campo,"dd/MM/yyyy HH:mm"));
        return campo;
    }

    private static LocalDateTime fechaHora(JSpinner campo){
        return LocalDateTime.ofInstant(((Date)campo.getValue()).toInstant(),ZoneId.systemDefault()).truncatedTo(ChronoUnit.MINUTES);
    }

    private void solicitudEnviada(){message("¡Solicitud enviada!", "Podés seguir su estado desde Contrataciones.");showContracts();}

    private void intentar(Runnable accion) {
        try {
            accion.run();
        } catch (RuntimeException error) {
            JOptionPane.showMessageDialog(this, error.getMessage(), "No se pudo completar la operación",
                    JOptionPane.WARNING_MESSAGE);
        }
    }

    /** Pide puntaje y comentario; un segundo intento sobre la misma contratación muestra el error del dominio. */
    private void calificar(Contratacion contratacion) {
        JComboBox<Integer> puntaje = new JComboBox<>(new Integer[]{5, 4, 3, 2, 1});
        JTextField comentario = new JTextField(24);
        JPanel form = new JPanel(new GridLayout(0, 1, 4, 5));
        form.add(new JLabel("Puntaje para " + contratacion.contraparte()));
        form.add(puntaje);
        form.add(new JLabel("Comentario (opcional)"));
        form.add(comentario);
        if (JOptionPane.showConfirmDialog(this, form, "Calificar", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) {
            return;
        }
        intentar(() -> {
            Perfil calificado = calificaciones.calificar(USER_ID, contratacion.id(), (Integer) puntaje.getSelectedItem(),
                    comentario.getText());
            message("¡Gracias por calificar!", calificado.nombre() + " ahora tiene ★ " + calificado.reputacion()
                    + " de reputación.");
            showContracts();
        });
    }

    /** RECHAZADA y CANCELADA son estados finales: solo se consulta el historial (regla 8). */
    /**
     * Botones que la UI de la cátedra no tenía: el proveedor rechaza una solicitud pendiente, y en ACEPTADA o
     * CONFIRMADA (los estados que admiten reprogramación según la consigna) una parte propone otra franja y la
     * otra acepta o rechaza el cambio. Quién puede y cuándo lo vuelve a validar el dominio.
     */
    private void accionesExtra(Contratacion x,PasoSiguienteVista paso,JPanel actions){
        if(x.estado()==EstadoContratacion.PENDIENTE&&"Proveedor".equals(paso.rol())){
            JButton reject=button("Rechazar",false);reject.addActionListener(e->rechazar(x));actions.add(reject);
        }
        if(x.estado()!=EstadoContratacion.ACEPTADA&&x.estado()!=EstadoContratacion.CONFIRMADA)return;
        if(paso.reprogramacionPropuesta()==null){
            JButton move=button("Reprogramar",false);move.addActionListener(e->intentar(()->reprogramar(x)));actions.add(move);
        }else if(!paso.propuestaPorMi()){
            JButton ok=button("Aceptar cambio",true);ok.addActionListener(e->intentar(()->{operaciones.aceptarReprogramacion(USER_ID,x.id());message("Sesión reprogramada","La sesión pasó al "+paso.reprogramacionPropuesta().format(DateTimeFormatter.ofPattern("dd/MM · HH:mm"))+".");showContracts();}));actions.add(ok);
            JButton no=button("Rechazar cambio",false);no.addActionListener(e->intentar(()->{operaciones.rechazarReprogramacion(USER_ID,x.id());showContracts();}));actions.add(no);
        }
    }

    private void rechazar(Contratacion x){
        String motivo=JOptionPane.showInputDialog(this,"¿Por qué rechazás la solicitud de "+x.contraparte()+"?","Rechazar solicitud",JOptionPane.PLAIN_MESSAGE);
        if(motivo==null)return;
        intentar(()->{operaciones.rechazar(USER_ID,x.id(),motivo);showContracts();});
    }

    private void reprogramar(Contratacion x){
        List<FranjaVista> franjas=operaciones.listarFranjasParaReprogramar(USER_ID,x.id());
        if(franjas.isEmpty()){message("Sin franjas disponibles","La publicación no tiene otra franja con lugar para mover la sesión.");return;}
        JComboBox<String> franja=new JComboBox<>(franjas.stream().map(this::textoFranja).toArray(String[]::new));
        JPanel form=formulario();campo(form,"Nueva franja",franja);campo(form,"",new JLabel(x.contraparte()+" tiene que aceptar el cambio."));
        if(JOptionPane.showConfirmDialog(this,form,"Reprogramar: "+x.titulo(),JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)!=JOptionPane.OK_OPTION)return;
        operaciones.proponerReprogramacion(USER_ID,x.id(),franjas.get(franja.getSelectedIndex()).id());
        message("Cambio propuesto","Le avisamos a "+x.contraparte()+" para que lo acepte o lo rechace.");
        showContracts();
    }

    private void verComprobante(Contratacion contratacion) {
        String lineas = String.join("\n", operaciones.verComprobante(USER_ID, contratacion.id()));
        JOptionPane.showMessageDialog(this, lineas, "Comprobante · " + contratacion.titulo(),
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void verHistorial(Contratacion contratacion) {
        String lineas = String.join("\n", operaciones.verHistorial(USER_ID, contratacion.id()));
        JOptionPane.showMessageDialog(this, lineas, "Historial · " + contratacion.titulo(),
                JOptionPane.INFORMATION_MESSAGE);
    }
}
