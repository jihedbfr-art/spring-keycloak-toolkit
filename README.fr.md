# spring-keycloak-toolkit

[![JitPack](https://jitpack.io/v/jihedbfr-art/spring-keycloak-toolkit.svg)](https://jitpack.io/#jihedbfr-art/spring-keycloak-toolkit)
[![CI](https://github.com/jihedbfr-art/spring-keycloak-toolkit/actions/workflows/ci.yml/badge.svg)](https://github.com/jihedbfr-art/spring-keycloak-toolkit/actions)

[English version](./README.md)

Petite autoconfiguration Spring Boot pour les applications qui se trouvent derrière Keycloak en
tant que resource server. Elle corrige le piège dans lequel tombe presque toute intégration
Spring + Keycloak : le convertisseur JWT par défaut de Spring Security ne sait pas que Keycloak
place les rôles sous `realm_access.roles` et `resource_access.<clientId>.roles` au lieu d'un
claim `scope` plat. Sans ça, `hasRole(...)` et `@PreAuthorize` ne font silencieusement rien, car
le token ne produit jamais la moindre autorité `ROLE_*`.

Je suis tombé sur ce même câblage sur chaque backend sécurisé par Keycloak que j'ai construit ces
deux dernières années (realms, authenticators SPI custom, tout le lot), et j'ai copié-collé une
version de ce convertisseur dans chacun d'eux. C'est ce code, enfin extrait, testé et packagé pour
que j'arrête de le réécrire.

**Installation rapide :** ajoutez le dépôt JitPack, puis
`com.github.jihedbfr-art:spring-keycloak-toolkit:v0.1.0` comme dépendance — voir
[Installation](#installation) ci-dessous pour le bloc complet et l'alternative en build local.

## Ce que ça vous apporte

- `KeycloakRealmRoleConverter` — lit les rôles realm et/ou client depuis le JWT et les mappe en
  `SimpleGrantedAuthority` avec un préfixe configurable (par défaut `ROLE_`, conforme à la
  convention de Spring Security elle-même).
- Une autoconfiguration qui enregistre un `JwtAuthenticationConverter` câblé au convertisseur de
  rôles, pour que `spring-security-oauth2-resource-server` le prenne en compte sans configuration
  supplémentaire dans le cas courant.
- `ProblemDetailAuthenticationEntryPoint` / `ProblemDetailAccessDeniedHandler` — des corps JSON
  RFC 7807 pour les 401/403 au lieu de la réponse vide par défaut de Spring Security. Pas câblés
  automatiquement dans votre filter chain (chaque application découpe ses endpoints différemment),
  juste exposés comme des beans que vous branchez dans `exceptionHandling(...)`.

## Ce que ça ne fait délibérément pas

Ça ne configure pas votre `SecurityFilterChain`, ça ne touche pas aux matchers d'endpoints, et ça
ne présume d'aucune structure particulière de realm/client au-delà de « les rôles vivent où
Keycloak les place ». Câbler automatiquement une chaîne de sécurité pour vous impliquerait de
deviner vos endpoints publics, et se tromper silencieusement sur ce point est pire que d'écrire
quatre lignes de config vous-même.

## Installation

Via [JitPack](https://jitpack.io/#jihedbfr-art/spring-keycloak-toolkit) — ajoutez le repo
JitPack, puis récupérez le tag comme dépendance :

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

```xml
<dependency>
    <groupId>com.github.jihedbfr-art</groupId>
    <artifactId>spring-keycloak-toolkit</artifactId>
    <version>v0.1.0</version>
</dependency>
```

Remarque : JitPack déduit son `groupId:artifactId` du nom d'utilisateur/dépôt GitHub ci-dessus,
ce qui diffère des coordonnées Maven propres au projet (`io.github.jihedbfr-art:keycloak-toolkit-spring-boot-starter`)
utilisées une fois construit et installé localement, ou publié sur Maven Central. Les deux sont
correctes pour leur méthode d'installation respective — ne pas les mélanger.

Ou en build et install local :

```bash
git clone https://github.com/jihedbfr-art/spring-keycloak-toolkit.git
cd spring-keycloak-toolkit
mvn clean install
```

```xml
<dependency>
    <groupId>io.github.jihedbfr-art</groupId>
    <artifactId>keycloak-toolkit-spring-boot-starter</artifactId>
    <version>0.1.0</version>
</dependency>
```

## Utilisation

Pointez-le vers l'ID de votre client Keycloak et c'est réglé pour le mapping des rôles :

```yaml
jihedapps:
  keycloak-toolkit:
    resource-id: my-client       # le client Keycloak dont vous voulez les rôles resource_access
    resource-roles-enabled: true
    realm-roles-enabled: true    # activé par défaut
```

Si vous voulez aussi les corps d'erreur JSON :

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http,
        ProblemDetailAuthenticationEntryPoint entryPoint,
        ProblemDetailAccessDeniedHandler accessDeniedHandler) throws Exception {
    http
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/actuator/health").permitAll()
            .anyRequest().authenticated())
        .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
        .exceptionHandling(e -> e
            .authenticationEntryPoint(entryPoint)
            .accessDeniedHandler(accessDeniedHandler));
    return http.build();
}
```

## Référence de configuration

| Propriété | Défaut | Ce que ça fait |
|---|---|---|
| `jihedapps.keycloak-toolkit.realm-roles-enabled` | `true` | Lit `realm_access.roles` |
| `jihedapps.keycloak-toolkit.resource-roles-enabled` | `true` | Lit `resource_access.<resource-id>.roles` |
| `jihedapps.keycloak-toolkit.resource-id` | *(aucun)* | ID du client Keycloak pour les rôles resource ; ignoré si vide |
| `jihedapps.keycloak-toolkit.role-prefix` | `ROLE_` | Préfixe appliqué devant chaque rôle |
| `jihedapps.keycloak-toolkit.problem-details-enabled` | `true` | Enregistre les beans ProblemDetail entry point / handler |

## Compatibilité

Construit et testé avec Spring Boot 3.2.5 / Spring Security 6 / Java 17, ce que je fais tourner
en production. Les versions 3.x plus anciennes de Spring Boot fonctionneront probablement aussi
car l'API d'autoconfiguration utilisée ici est stable, mais je ne les ai pas testées.

## Roadmap

Ce que je veux ajouter une fois que ce projet aura vu un peu plus d'usage réel plutôt que de
deviner à l'avance :

- support multi-tenant (plus d'un `resource-id` à la fois)
- un module de fixtures de test (`JwtTestUtils` ou similaire) pour que les consommateurs n'aient
  pas à fabriquer des faux JWT à la main comme le font les tests de ce dépôt
- publication sur Maven Central — le profil `release` du `pom.xml` est prêt (signature +
  Central Portal), il ne manque plus qu'un compte Sonatype et une paire de clés GPG

## Licence

Apache License 2.0 — voir [LICENSE](LICENSE).
