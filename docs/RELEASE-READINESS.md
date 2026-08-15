# Audit de Préparation à la Publication — `spring-keycloak-toolkit`

> **Document de référence — Phase A (Master Brief Maven Central)**  
> **Date de réalisation** : 14/08/2026  
> **Branche** : `chore/release-readiness-audit`  
> **Statut du build** : `BUILD SUCCESS` (Java 17 / Spring Boot 3.2.5)

---

## 1. Analyse du Code & Surface d'API

### 1.1 Architecture et Modules
Le projet est actuellement un projet Maven mono-module standard (`spring-keycloak-toolkit`) sans module d'exemple ni sous-modules.

### 1.2 Inventaire des Classes Publiques
Le code source est réparti en 3 packages :

1. **`com.jihedapps.keycloak.security`** :
   - `KeycloakRealmRoleConverter` (implémente `Converter<Jwt, Collection<GrantedAuthority>>`) :
     - Extrait les rôles de royaume depuis la revendication `realm_access.roles`.
     - Extrait les rôles clients depuis `resource_access.<clientId>.roles`.
     - Préfixe chaque rôle (par défaut `ROLE_`) et le convertit en majuscules (`Locale.ROOT`) pour correspondre aux conventions de Spring Security (`hasRole(...)`, `@PreAuthorize`).
     - Retourne un `Set` immuable de `SimpleGrantedAuthority`.

2. **`com.jihedapps.keycloak.error`** :
   - `ProblemDetailAuthenticationEntryPoint` (implémente `AuthenticationEntryPoint`) :
     - Écrit une réponse HTTP 401 Unauthorized structurée au format **RFC 7807** (`application/problem+json`) avec Jackson `ObjectMapper`.
   - `ProblemDetailAccessDeniedHandler` (implémente `AccessDeniedHandler`) :
     - Écrit une réponse HTTP 403 Forbidden structurée au format **RFC 7807** (`application/problem+json`).

3. **`com.jihedapps.keycloak.autoconfigure`** :
   - `KeycloakToolkitAutoConfiguration` :
     - Déclarée via `@AutoConfiguration`, conditionnée par `@ConditionalOnClass(Jwt.class)` et `@EnableConfigurationProperties(KeycloakToolkitProperties.class)`.
     - Enregistre `KeycloakRealmRoleConverter` (`@ConditionalOnMissingBean`).
     - Enregistre `JwtAuthenticationConverter` câblé au converter (`@ConditionalOnMissingBean`).
     - Enregistre `ProblemDetailAuthenticationEntryPoint` et `ProblemDetailAccessDeniedHandler` conditionnés par la propriété `problem-details-enabled` (`matchIfMissing = true`).
   - `KeycloakToolkitProperties` :
     - `@ConfigurationProperties(prefix = "jihedapps.keycloak-toolkit")`.
     - Propriétés exposées :
       - `realmRolesEnabled` (booléen, défaut: `true`)
       - `resourceRolesEnabled` (booléen, défaut: `true`)
       - `rolePrefix` (String, défaut: `"ROLE_"`)
       - `resourceId` (String, défaut: `null`)
       - `problemDetailsEnabled` (booléen, défaut: `true`)

### 1.3 Mécanisme d'Auto-configuration Spring Boot 3
- Fichier présent : `src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.
- Contenu : `com.jihedapps.keycloak.autoconfigure.KeycloakToolkitAutoConfiguration`.
- Le mécanisme est 100% conforme à Spring Boot 3.x (pas d'ancien `spring.factories`).

### 1.4 Versions Cibles
- **Java** : 17 (`<maven.compiler.source>17</maven.compiler.source>`).
- **Spring Boot** : 3.2.5 (BOM `spring-boot-dependencies:3.2.5` importé dans `dependencyManagement`).
- **Spring Security** : 6.2.4 (géré transitivement par Spring Boot 3.2.5).
- **Keycloak** : Aucun client Keycloak ou adaptateur propriétaire n'est embarqué. L'intégration repose exclusivement sur les spécifications OAuth2 Resource Server / JWT standard de Spring Security.

---

## 2. Analyse de la Qualité & Tests

### 2.1 Couverture et Typologie des Tests
Le projet comprend 4 classes de tests unitaires et de contexte (6 cas de test au total) :
- `KeycloakToolkitAutoConfigurationTest` (3 tests) :
  - Vérification de l'enregistrement des beans par défaut via `ApplicationContextRunner`.
  - Vérification du back-off des beans ProblemDetail lorsque désactivés via propriété.
  - Vérification du back-off lors de la définition d'un converter utilisateur personnalisé.
- `KeycloakRealmRoleConverterTest` (4 tests) :
  - Mapping des rôles de realm avec préfixe et majuscules.
  - Mapping des rôles de resource uniquement pour le `resourceId` configuré.
  - Comportement sur token sans revendications d'accès (retourne un ensemble vide, pas de NPE).
  - Combinaison des rôles de realm et de resource.
- `ProblemDetailAuthenticationEntryPointTest` (1 test) :
  - Vérification du code statut 401, du Content-Type `application/problem+json` et du corps JSON.
- `ProblemDetailAccessDeniedHandlerTest` (1 test) :
  - Vérification du code statut 403, du Content-Type `application/problem+json` et du corps JSON.

### 2.2 Lacunes de Test Identifiées
- **Aucun test d'intégration avec un vrai Keycloak** : Pas de Testcontainers (`org.testcontainers:keycloak`), aucun realm de test JSON versionné.
- **Aucun test multi-versions** : Pas de matrice de compatibilité testée en CI.

### 2.3 Javadoc
- La Javadoc actuelle est partielle :
  - Présente sur les classes publiques (`KeycloakRealmRoleConverter`, `KeycloakToolkitAutoConfiguration`, handlers d'erreur).
  - Absente sur les méthodes publiques, constructeurs, getters/setters de configuration.
  - Le build produit **18 avertissements Javadoc** (`warning: no comment`).

---

## 3. Analyse de la Configuration de Publication (`pom.xml`)

### 3.1 Coordonnées Actuelles
- `groupId` : `com.jihedapps` ⚠️ *(Incompatible avec le Sonatype Central Portal qui exige la vérification du namespace GitHub `io.github.jihedbfr-art`)*.
- `artifactId` : `spring-keycloak-toolkit`.
- `version` : `0.1.0`.
- `packaging` : `jar`.

### 3.2 Métadonnées POM
- `name`, `description`, `url` : Présents et corrects.
- `licenses` : Présent (`MIT License`) ⚠️ *(Le brief exige une licence Apache-2.0 pour la bibliothèque)*.
- `developers` : Présent mais contient une adresse e-mail personnelle (`jihedbenarfa2026@gmail.com`) ⚠️ *(À retirer impérativement selon §0.2)*.
- `scm` : Présent (`connection`, `developerConnection`, `url`).

### 3.3 Plugins de Build & Profil Release
- `maven-source-plugin` (3.3.1) : Configuré avec `jar-no-fork`.
- `maven-javadoc-plugin` (3.6.3) : Configuré avec `jar`.
- Profil `release` :
  - `maven-gpg-plugin` (3.2.4) : Présent.
  - `central-publishing-maven-plugin` (0.7.0) : Présent avec `publishingServerId` = `central` et `autoPublish` = `false`.

---

## 4. Sortie Brute du Build (`mvn clean verify`)

```text
[INFO] Scanning for projects...
[INFO] 
[INFO] ----------------< com.jihedapps:spring-keycloak-toolkit >----------------
[INFO] Building spring-keycloak-toolkit 0.1.0
[INFO]   from pom.xml
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- clean:3.2.0:clean (default-clean) @ spring-keycloak-toolkit ---
[INFO] 
[INFO] --- resources:3.3.1:resources (default-resources) @ spring-keycloak-toolkit ---
[INFO] Copying 1 resource from src\main\resources to target\classes
[INFO] 
[INFO] --- compiler:3.13.0:compile (default-compile) @ spring-keycloak-toolkit ---
[INFO] Recompiling the module because of changed source code.
[INFO] Compiling 5 source files with javac [debug target 17] to target\classes
[INFO] 
[INFO] --- resources:3.3.1:testResources (default-testResources) @ spring-keycloak-toolkit ---
[INFO] skip non existing resourceDirectory E:\jihedapps\GitHub\spring-keycloak-toolkit\src\test\resources
[INFO] 
[INFO] --- compiler:3.13.0:testCompile (default-testCompile) @ spring-keycloak-toolkit ---
[INFO] Recompiling the module because of changed dependency.
[INFO] Compiling 4 source files with javac [debug target 17] to target\test-classes
[INFO] 
[INFO] --- surefire:3.2.5:test (default-test) @ spring-keycloak-toolkit ---
[INFO] Using auto detected provider: org.apache.maven.surefire.junitplatform.JUnitPlatformProvider
[INFO] 
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.jihedapps.keycloak.autoconfigure.KeycloakToolkitAutoConfigurationTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 2.108 s -- in com.jihedapps.keycloak.autoconfigure.KeycloakToolkitAutoConfigurationTest
[INFO] Running com.jihedapps.keycloak.error.ProblemDetailAccessDeniedHandlerTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.218 s -- in com.jihedapps.keycloak.error.ProblemDetailAccessDeniedHandlerTest
[INFO] Running com.jihedapps.keycloak.error.ProblemDetailAuthenticationEntryPointTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.013 s -- in com.jihedapps.keycloak.error.ProblemDetailAuthenticationEntryPointTest
[INFO] Running com.jihedapps.keycloak.security.KeycloakRealmRoleConverterTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.046 s -- in com.jihedapps.keycloak.security.KeycloakRealmRoleConverterTest
[INFO] Results:
[INFO] 
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] --- jar:3.4.1:jar (default-jar) @ spring-keycloak-toolkit ---
[INFO] Building jar: E:\jihedapps\GitHub\spring-keycloak-toolkit\target\spring-keycloak-toolkit-0.1.0.jar
[INFO] 
[INFO] --- source:3.3.1:jar-no-fork (attach-sources) @ spring-keycloak-toolkit ---
[INFO] Building jar: E:\jihedapps\GitHub\spring-keycloak-toolkit\target\spring-keycloak-toolkit-0.1.0-sources.jar
[INFO] 
[INFO] --- javadoc:3.6.3:jar (attach-javadocs) @ spring-keycloak-toolkit ---
[INFO] No previous run data found, generating javadoc.
[WARNING] Javadoc Warnings
[WARNING] .../KeycloakRealmRoleConverter.java:36: warning: no comment
[WARNING] .../KeycloakToolkitAutoConfiguration.java:40: warning: no comment
[WARNING] .../KeycloakToolkitProperties.java:6: warning: no comment
[WARNING] (18 warnings total)
[INFO] Building jar: E:\jihedapps\GitHub\spring-keycloak-toolkit\target\spring-keycloak-toolkit-0.1.0-javadoc.jar
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  22.955 s
[INFO] Finished at: 2026-08-14T23:16:09+01:00
[INFO] ------------------------------------------------------------------------
```

---

## 5. Verdict

### Réponse à la question clé : *Le projet est-il publiable en l'état, ou faut-il d'abord écrire le cœur fonctionnel ?*

#### **Verdict : Le cœur fonctionnel existe, est sain et compile, mais le projet N'EST PAS publiable en l'état sans les ajustements structurels des Phases B, C et D.**

1. **Le cœur fonctionnel n'est PAS un squelette vide** :
   - Le convertisseur de rôles (`KeycloakRealmRoleConverter`) résout un problème technique réel et concret (mapping `realm_access` et `resource_access` vers `ROLE_*`).
   - Les gestionnaires d'erreurs RFC 7807 (`ProblemDetailAuthenticationEntryPoint`, `ProblemDetailAccessDeniedHandler`) fonctionnent et respectent les standards Spring Security 6 / Spring Boot 3.
   - L'auto-configuration Boot 3 est correctement implémentée avec `AutoConfiguration.imports` et des conditions de back-off précises.
   - Les tests unitaires et de contexte passent à 100%.

2. **Éléments bloquants avant publication sur Maven Central (immuabilité)** :
   - **Coordonnées et package namespace** : Le `groupId` `com.jihedapps` et les packages `com.jihedapps.*` doivent être migrés vers `io.github.jihedbfr-art.*` pour permettre la vérification automatisée par Sonatype Central Portal.
   - **Préfixe de configuration** : `jihedapps.keycloak-toolkit.*` doit être harmonisé vers un préfixe stable et définitif (ex: `jihedailabs.keycloak.*`).
   - **Métadonnées de configuration IDE** : `spring-boot-configuration-processor` est présent en dépendance optionnelle mais aucune métadonnée JSON n'est validée dans le livrable jar.
   - **Javadoc** : 18 warnings doivent être résolus (100% de Javadoc requise sur les types publics).
   - **Sécurité et anonymat** : Retrait de l'e-mail personnel dans le `pom.xml`.
   - **Licence** : Mise à jour du POM et du fichier `LICENSE` vers `Apache-2.0`.
   - **Tests d'intégration réels** : Ajout de tests avec Testcontainers (Keycloak 24/25) pour garantir l'absence de régression.

---

## 6. Synthèse des Recommandations pour les Phases Suivantes

| Phase | Priorité | Action Principale |
|---|---|---|
| **Phase B (Public API)** | **Bloquante** | Renommage `groupId: io.github.jihedbfr-art`, packages `io.github.jihedbfr_art.keycloak.*`, préfixe `jihedailabs.keycloak.*`, 100% Javadoc sans avertissement, licence Apache-2.0. |
| **Phase C (Tests)** | **Majeure** | Intégration Testcontainers Keycloak + matrice CI croisée (Spring Boot 3.2/3.3 + Keycloak 24/25) + JaCoCo ≥ 70%. |
| **Phase D (Publishing)** | **Bloquante** | Validation du namespace Central Portal, configuration GPG, plugin `central-publishing-maven-plugin`, rédaction de `docs/RELEASING.md`. |
| **Phase E (CI Release)** | **Majeure** | Workflow de release sécurisé déclenché sur tag `v*` avec garde-fous. |
| **Phase F (Adoption)** | **Majeure** | Refonte README bilingue, module `samples/` exécutable en 1 commande. |
