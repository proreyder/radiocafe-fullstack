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
 private final ContestUseCase contest;private final SelectWinnerUseCase winner;private final RedeemPrizeUseCase redeem;private final CurrentUser current;
 public KappuContestController(ContestUseCase contest,SelectWinnerUseCase winner,RedeemPrizeUseCase redeem,CurrentUser current) {this.contest=contest;this.winner=winner;this.redeem=redeem;this.current=current;}
 private ApiDtos.IllustrationView view(IlustracionKappu i,Authentication auth) {return ApiDtos.IllustrationView.of(i,current.staff(auth)||current.get(auth).idUsuario().equals(i.getIdUsuario()));}
 @GetMapping public List<ApiDtos.IllustrationView> list(Authentication auth) {return contest.listar().stream().map(i->view(i,auth)).toList();}
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
