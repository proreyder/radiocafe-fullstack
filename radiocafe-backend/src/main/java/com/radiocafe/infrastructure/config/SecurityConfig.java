package com.radiocafe.infrastructure.config;
import com.radiocafe.domain.ports.out.*;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.*;
import org.springframework.security.authentication.*;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.session.*;
import org.springframework.http.HttpMethod;
@Configuration @EnableMethodSecurity
public class SecurityConfig {
 @Bean PasswordEncoder passwordEncoder() {return new BCryptPasswordEncoder();}
 @Bean PasswordPort passwordPort(PasswordEncoder encoder) {return new PasswordPort(){public String codificar(String p){return encoder.encode(p);}public boolean coincide(String raw,String hash){return encoder.matches(raw,hash);}};}
 @Bean UserDetailsService userDetailsService(UserRepositoryPort users) {
  return correo -> {var u=users.buscarPorCorreo(correo).orElseThrow(()->new UsernameNotFoundException("Credenciales inválidas."));
   return new AuthenticatedUser(u);};
 }
 @Bean AuthenticationManager authenticationManager(UserDetailsService users,PasswordEncoder encoder) {
  var provider=new DaoAuthenticationProvider(users);provider.setPasswordEncoder(encoder);return new ProviderManager(provider);
 }
 @Bean SecurityContextRepository securityContextRepository() {return new TabSecurityContextRepository();}
 @Bean SessionAuthenticationStrategy sessionStrategy() {return new ChangeSessionIdAuthenticationStrategy();}
 @Bean SecurityFilterChain security(HttpSecurity http,SecurityContextRepository contexts,UserRepositoryPort users) throws Exception {
  return http.addFilterBefore(new TabSessionHeaderFilter(),org.springframework.security.web.context.SecurityContextHolderFilter.class)
   .addFilterBefore(new SessionAccountFilter(users,contexts),org.springframework.security.web.access.intercept.AuthorizationFilter.class).cors(c->{}).securityContext(c->c.securityContextRepository(contexts))
   .authorizeHttpRequests(a->a.requestMatchers(HttpMethod.OPTIONS,"/**").permitAll()
    .requestMatchers("/api/v1/auth/csrf","/api/v1/auth/login","/api/v1/auth/register","/error").permitAll()
    .requestMatchers(HttpMethod.POST,"/api/v1/guest/orders","/api/v1/commerce/quote").permitAll()
    .requestMatchers(HttpMethod.GET,"/api/v1/guest/orders/*","/api/v1/products/options","/api/v1/baristas","/api/v1/kappu/gallery","/api/v1/kappu/status","/api/v1/kappu/images/files/*","/api/v1/products","/api/v1/commerce/shop","/api/v1/commerce/campaigns","/api/v1/commerce/reviews","/api/v1/images/files/*","/v3/api-docs/**","/v3/api-docs.yaml","/swagger-ui/**","/swagger-ui.html","/api-explorer/**").permitAll().anyRequest().authenticated())
   .exceptionHandling(e->e.authenticationEntryPoint((req,res,ex)->{res.setStatus(401);res.setContentType("application/json;charset=UTF-8");res.getWriter().write("{\"message\":\"Inicia sesión para continuar.\"}");})
    .accessDeniedHandler((req,res,ex)->{res.setStatus(403);res.setContentType("application/json;charset=UTF-8");res.getWriter().write("{\"message\":\"Acceso denegado o sesión de formulario caducada.\"}");}))
   .logout(l->l.logoutUrl("/api/v1/auth/logout").invalidateHttpSession(false)
    .addLogoutHandler((req,res,auth)->{
     contexts.saveContext(org.springframework.security.core.context.SecurityContextHolder.createEmptyContext(),req,res);
     TabSecurityContextRepository.invalidateIfUnsharedLegacySession(req);
    }).logoutSuccessHandler((req,res,auth)->res.setStatus(204)))
   .build();
 }
}
