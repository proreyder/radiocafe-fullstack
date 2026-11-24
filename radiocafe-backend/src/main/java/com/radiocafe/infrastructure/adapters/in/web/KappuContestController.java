package com.radiocafe.infrastructure.adapters.in.web;
import com.radiocafe.domain.model.*;
import com.radiocafe.domain.ports.in.*;
import com.google.zxing.*;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.http.*;
import java.io.ByteArrayOutputStream;
import java.util.List;
@io.swagger.v3.oas.annotations.tags.Tag(name="Concurso Kappu",description="Participación, un voto por cuenta, ganador único y canje único mediante QR.")
@RestController @RequestMapping("/api/v1/kappu")
public class KappuContestController {
 private final ContestUseCase contest;private final SelectWinnerUseCase winner;private final RedeemPrizeUseCase redeem;private final CurrentUser current;private final ProductImagesUseCase images;
 public KappuContestController(ContestUseCase contest,SelectWinnerUseCase winner,RedeemPrizeUseCase redeem,CurrentUser current,ProductImagesUseCase images) {this.contest=contest;this.winner=winner;this.redeem=redeem;this.current=current;this.images=images;}
 private ApiDtos.IllustrationView view(IlustracionKappu i,Authentication auth) {return ApiDtos.IllustrationView.of(i,current.staff(auth)||current.get(auth).idUsuario().equals(i.getIdUsuario()));}
 @GetMapping public List<ApiDtos.IllustrationView> list(Authentication auth) {return contest.listar().stream().map(i->view(i,auth)).toList();}
 public record Schedule(@jakarta.validation.constraints.NotNull java.time.LocalDateTime inicio,@jakarta.validation.constraints.NotNull java.time.LocalDateTime fin) {}
 public record Enrollment(@jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max=150) String tituloObra,@jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max=100) String nombreParticipante,@jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(min=5,max=2000) String descripcion,@jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max=2000) @jakarta.validation.constraints.Pattern(regexp="https://[^\\s]+|/api/v1/kappu/images/files/[a-f0-9-]{36}\\.(png|jpg)") String imagenUrl) {}
 public record Gallery(ConcursoEstado estado,List<ApiDtos.IllustrationView> ilustraciones) {}
 @GetMapping("/gallery") public Gallery gallery() {var state=contest.estado();return new Gallery(state,contest.listar().stream().map(i->ApiDtos.IllustrationView.of(i,false)).toList());}
 @GetMapping("/my-vote") public java.util.Map<String,Boolean> myVote(Authentication auth) {return java.util.Map.of("votoRegistrado",contest.yaVoto(current.get(auth).idUsuario()));}
 @GetMapping("/status") public ConcursoEstado status() {return contest.estado();}
 @PutMapping("/schedule") @PreAuthorize("hasRole('ADMINISTRADOR')") public ConcursoEstado schedule(@Valid @RequestBody Schedule input) {return contest.configurar(input.inicio(),input.fin());}
 @PostMapping("/close") @PreAuthorize("hasRole('ADMINISTRADOR')") public ConcursoEstado close() {return contest.cerrar();}
 @PostMapping("/entries") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasRole('CLIENTE')") public ApiDtos.IllustrationView enroll(@Valid @RequestBody Enrollment input,Authentication auth) {return view(contest.inscribir(current.get(auth).idUsuario(),input.tituloObra(),input.imagenUrl(),input.nombreParticipante(),input.descripcion()),auth);}
 @PostMapping(value="/images",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasRole('CLIENTE')") public ImagenProducto upload(@RequestPart("file") org.springframework.web.multipart.MultipartFile file) throws java.io.IOException {contest.verificarAbierto();return images.subirIlustracion(file.getBytes());}
 @GetMapping("/images/files/{nombre}") public ResponseEntity<byte[]> image(@PathVariable String nombre) {return ResponseEntity.ok().contentType(nombre.endsWith(".png")?MediaType.IMAGE_PNG:MediaType.IMAGE_JPEG).header("X-Content-Type-Options","nosniff").cacheControl(CacheControl.maxAge(java.time.Duration.ofDays(365)).cachePublic().immutable()).body(images.leerIlustracion(nombre));}
 @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasRole('CLIENTE')")
 public ApiDtos.IllustrationView participate(@Valid @RequestBody ApiDtos.IllustrationInput input,Authentication auth) {return view(contest.participar(current.get(auth).idUsuario(),input.tituloObra(),input.imagenUrl()),auth);}
 @PostMapping("/{id}/votes") @PreAuthorize("hasRole('CLIENTE')")
 public ApiDtos.IllustrationView vote(@PathVariable Long id,Authentication auth) {return view(contest.votar(current.get(auth).idUsuario(),id),auth);}
 @PostMapping("/select-winner/{id}") @PreAuthorize("hasRole('ADMINISTRADOR')")
 public ApiDtos.IllustrationView select(@PathVariable Long id,Authentication auth) {return view(winner.seleccionarGanador(id),auth);}
 @PostMapping("/redeem-prize") @PreAuthorize("hasAnyRole('ADMINISTRADOR','BARISTA')")
 public ApiDtos.IllustrationView redeem(@Valid @RequestBody ApiDtos.RedeemInput input,Authentication auth) {return view(redeem.canjearPremioPorCodigo(input.codigoPremio()),auth);}
 @GetMapping(value="/{id}/qr",produces=MediaType.IMAGE_PNG_VALUE)
 public ResponseEntity<byte[]> qr(@PathVariable Long id,Authentication auth) throws Exception {
  var i=contest.listar().stream().filter(x->x.getIdIlustracion().equals(id)).findFirst().orElseThrow(()->BusinessException.noEncontrado("Ilustración no encontrada."));
  if(!current.staff(auth)&&!current.get(auth).idUsuario().equals(i.getIdUsuario())) throw new org.springframework.security.access.AccessDeniedException("Este premio pertenece a otro participante.");
  if(!Boolean.TRUE.equals(i.getEsGanador())) throw BusinessException.conflicto("Esta ilustración no tiene premio.");
  var matrix=new QRCodeWriter().encode(i.getCodigoPremio(),BarcodeFormat.QR_CODE,280,280);var out=new ByteArrayOutputStream();MatrixToImageWriter.writeToStream(matrix,"PNG",out);
  return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(out.toByteArray());
 }
}
