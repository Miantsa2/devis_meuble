package com.atelier.devis.security;

import com.atelier.devis.domain.Repos.UserRepo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/** Authentification HTTP Basic sans état, utilisateurs en base (BCrypt). /api/admin/** réservé au rôle ADMIN. */
@Configuration
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    UserDetailsService userDetailsService(UserRepo users) {
        return username -> users.findByUsername(username)
                .map(u -> User.withUsername(u.getUsername()).password(u.getPasswordHash())
                        .authorities(new SimpleGrantedAuthority("ROLE_" + u.getRole())).build())
                .orElseThrow(() -> new UsernameNotFoundException(username));
    }

    @Bean
    SecurityFilterChain chain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
            .cors(Customizer.withDefaults())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/", "/error").permitAll()
                .requestMatchers("/actuator/health", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/**").authenticated()
                .anyRequest().denyAll())
            .httpBasic(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    CorsConfigurationSource cors(@Value("${app.cors-origins}") List<String> origins) {
        CorsConfiguration c = new CorsConfiguration();
        c.setAllowedOrigins(origins);
        c.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        c.setAllowedHeaders(List.of("*"));
        UrlBasedCorsConfigurationSource s = new UrlBasedCorsConfigurationSource();
        s.registerCorsConfiguration("/**", c);
        return s;
    }

    /**
     * Crée l'administrateur depuis l'environnement (aucun mot de passe par défaut dans le code).
     * Si l'utilisateur existe déjà, son mot de passe n'est remplacé que si APP_ADMIN_RESET=true
     * (utile après une saisie manuelle en base ou un changement de mot de passe).
     */
    @Bean
    CommandLineRunner bootstrapAdmin(UserRepo users, PasswordEncoder enc,
                                     @Value("${app.admin.username}") String user, @Value("${app.admin.password}") String pass,
                                     @Value("${app.admin.reset:false}") boolean reset) {
        return args -> {
            if (user.isBlank() || pass.isBlank()) {
                if (users.count() == 0)
                    System.err.println("[devis] Aucun utilisateur : définir APP_ADMIN_USERNAME et APP_ADMIN_PASSWORD pour créer l'administrateur.");
                return;
            }
            var existing = users.findByUsername(user);
            if (existing.isEmpty()) {
                var u = new com.atelier.devis.domain.Entities.UserEntity();
                u.setUsername(user); u.setPasswordHash(enc.encode(pass)); u.setRole("ADMIN");
                users.save(u);
                System.out.println("[devis] Administrateur '" + user + "' créé.");
            } else if (reset) {
                var u = existing.get();
                u.setPasswordHash(enc.encode(pass)); u.setRole("ADMIN");
                users.save(u);
                System.out.println("[devis] Mot de passe de '" + user + "' réinitialisé.");
            } else {
                System.out.println("[devis] Utilisateur '" + user + "' déjà présent (mot de passe inchangé ; APP_ADMIN_RESET=true pour le réinitialiser).");
            }
        };
    }
}
