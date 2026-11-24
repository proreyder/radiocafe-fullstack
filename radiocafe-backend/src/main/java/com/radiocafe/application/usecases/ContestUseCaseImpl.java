package com.radiocafe.application.usecases;
import com.radiocafe.domain.model.*;import com.radiocafe.domain.ports.in.*;import com.radiocafe.domain.ports.out.*;
import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;import java.time.*;import java.util.*;
@Service @Transactional
public class ContestUseCaseImpl implements ContestUseCase {
 private final IllustrationRepositoryPort illustrations;private final ContestRepositoryPort contest;private final UserRepositoryPort users;private final NotificationUseCase notifications;private final Clock clock;
 public ContestUseCaseImpl(IllustrationRepositoryPort illustrations,ContestRepositoryPort contest,UserRepositoryPort users,NotificationUseCase notifications,Clock clock) {this.illustrations=illustrations;this.contest=contest;this.users=users;this.notifications=notifications;this.clock=clock;}
 private List<IlustracionKappu> ranking() {return illustrations.obtenerTodas().stream().sorted(Comparator.comparing(IlustracionKappu::getVotosCount,Comparator.nullsLast(Comparator.reverseOrder())).thenComparing(IlustracionKappu::getFechaSubida,Comparator.nullsLast(Comparator.naturalOrder())).thenComparing(IlustracionKappu::getIdIlustracion)).toList();}
 private void congelar() {
  var c=contest.estado();if(c.cerrado()||c.idGanador()!=null)return;
  var top=ranking().stream().limit(3).map(IlustracionKappu::getIdIlustracion).toList();
  for(var i:illustrations.obtenerTodas()) {i.setEsFinalista(top.contains(i.getIdIlustracion()));illustrations.guardar(i);if(Boolean.TRUE.equals(i.getEsFinalista())) notifications.enviar(i.getIdUsuario(),null,"KAPPU","¡Tu arte es finalista!","Tu ilustración está entre las finalistas del concurso de Kappu.","/catalogo?seccion=kappu");}
  contest.configurar(c.inicio(),c.fin(),true);
 }
 public ConcursoEstado estado() {
  contest.bloquear();var c=contest.estado();var now=LocalDateTime.now(clock);
  if(!c.cerrado()&&c.idGanador()==null&&c.fin()!=null&&!now.isBefore(c.fin())) {congelar();c=contest.estado();}
  String fase=c.idGanador()!=null?"GANADOR":c.cerrado()?"FINALISTAS":c.inicio()!=null&&now.isBefore(c.inicio())?"PROXIMAMENTE":"ABIERTO";
  var finalistas=ranking().stream().filter(i->Boolean.TRUE.equals(i.getEsFinalista())).map(IlustracionKappu::getIdIlustracion).toList();
  return new ConcursoEstado(c.inicio(),c.fin(),c.cerrado()||c.idGanador()!=null,c.idGanador(),fase,finalistas);
 }
 public ConcursoEstado configurar(LocalDateTime inicio,LocalDateTime fin) {
  contest.bloquear();var c=estado();if(c.cerrado()) throw BusinessException.conflicto("El concurso cerrado no puede reabrirse.");
  if(inicio==null||fin==null||!fin.isAfter(inicio)||!fin.isAfter(LocalDateTime.now(clock))) throw new IllegalArgumentException("Indica fechas válidas y un cierre futuro.");
  contest.configurar(inicio,fin,false);return estado();
 }
 public ConcursoEstado cerrar() {contest.bloquear();congelar();return estado();}
 public boolean yaVoto(Long usuario) {return contest.yaVoto(usuario);}
 public void verificarAbierto() {var c=estado();if(!c.fase().equals("ABIERTO")) throw BusinessException.conflicto("El concurso no acepta inscripciones ni likes en este momento.");}
 public List<IlustracionKappu> listar() {return ranking();}
 public IlustracionKappu participar(Long usuario,String titulo,String imagen) {return inscribir(usuario,titulo,imagen,users.buscarPorId(usuario).map(Usuario::nombre).orElse("Participante"),"");}
 public IlustracionKappu inscribir(Long usuario,String titulo,String imagen,String nombre,String descripcion) {
  contest.bloquear();verificarAbierto();var i=new IlustracionKappu();i.setIdUsuario(usuario);i.setTituloObra(titulo.strip());i.setImagenUrl(imagen);i.setNombreParticipante(nombre.strip());i.setDescripcion(descripcion.strip());
  var result=illustrations.guardar(i);notifications.enviarRol(Rol.ADMINISTRADOR,usuario,"KAPPU","Nueva ilustración de Kappu",nombre.strip()+" inscribió: "+titulo.strip()+".","/admin?seccion=kappu");return result;
 }
 public IlustracionKappu votar(Long usuario,Long id) {
  contest.bloquear();verificarAbierto();if(contest.yaVoto(usuario)) throw BusinessException.conflicto("Solo se permite un like por cuenta en este concurso.");
  var i=illustrations.buscarPorId(id).orElseThrow(()->BusinessException.noEncontrado("Ilustración no encontrada."));contest.votar(usuario,id);i.setVotosCount(i.getVotosCount()+1);return illustrations.guardar(i);
 }
}
