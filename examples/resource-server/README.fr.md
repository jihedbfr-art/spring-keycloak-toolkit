# Laboratoire de frontières JWT

[English version](README.md)

Une petite API utilisatrice du toolkit, avec des requêtes HTTP réelles et des refus vérifiés. Le projet est autonome ; ce n’est pas un module Maven ajouté au build racine.

## Construire et tester

Prérequis : JDK 17 ou 21, Maven, Docker et Compose pour le contrôle du JAR, Python 3 standard pour le smoke test. Le POM fixe Spring Boot 4.1.0 et les tests Keycloak 26.7.2. Les téléchargements Maven et Docker nécessitent le réseau.

Depuis la racine du dépôt :

```bash
mvn clean install
mvn -f examples/resource-server/pom.xml clean verify
```

La première commande teste et installe le toolkit `0.1.1` dans le cache Maven local. La seconde construit le JAR consommateur et exécute 19 tests : 14 avec une clé RSA et un JWKS locaux, 5 avec un vrai Keycloak Testcontainers. Les requêtes traversent le filtre de sécurité et un serveur HTTP sur un port aléatoire ; elles n’utilisent pas de JWT injecté par un mock de sécurité.

| Vérification | Résultat attendu |
|---|---:|
| Aucun Bearer sur la route lecteur | 401 + challenge Bearer |
| Lecteur vers route lecteur | 200 |
| Lecteur vers route administrateur | 403 |
| Administrateur vers route administrateur | 200 |
| Même clé, autre issuer | 401 |
| Audience absente ou autre | 401 |
| Expiration ou nbf au-delà de la tolérance testée | 401 |
| Autre signature ou Bearer malformé | 401 |
| Rôle d’un autre client ou valeurs de rôles malformées | 403 |

Le test d’issuer garde la même clé et les mêmes autres claims pour isoler ce refus. Les tests de dates se placent à dix minutes de la frontière, au-delà du décalage toléré par défaut ; ils ne mesurent pas précisément cette tolérance. Les tests de signature ne prouvent pas une rotation JWKS.

## Essayer le JAR construit

Depuis `examples/resource-server`, lancer Keycloak puis l’API dans un autre terminal :

```bash
docker compose -p toolkit-iam-lab up -d
java -jar target/resource-server-lab-0.1.0-SNAPSHOT.jar
```

Dans un troisième terminal, depuis le même dossier :

```bash
python smoke.py
```

Six contrôles sont attendus : 401, 200, 403, 200, 403, 401. Le script attend la disponibilité du realm et de l’API, puis obtient ses jetons en mémoire sans les afficher ni les enregistrer. Il n’accepte que des URL HTTP de loopback. Les deux commandes de lancement occupent par défaut `127.0.0.1:18080` pour Keycloak et `127.0.0.1:18081` pour l’API. Arrêter l’API avec Ctrl+C, puis :

```bash
docker compose -p toolkit-iam-lab down
```

`LAB_KEYCLOAK_PORT` change le port Compose ; modifier alors `LAB_ISSUER` et `LAB_JWK_SET_URI` pour l’API, ainsi que `--issuer` du script. `--server.port` change le port du JAR, et `--api` celui du script. Les tests Maven utilisent leurs propres ports et conteneurs.

## Ce qui est configuré

Le toolkit convertit les rôles du client `toolkit-lab` en autorités. Cet exemple désactive les rôles de realm, injecte explicitement le convertisseur et configure les réponses Problem Details. L’application conserve la responsabilité de sa chaîne de filtres et du validateur JWT : signature RS256, issuer, temps et audience `toolkit-lab`. Indiquer seulement une URL de JWKS ne remplace pas une validation d’issuer ou d’audience.

Le [realm synthétique](src/test/resources/iam-lab-realm.json) comporte quatre clients de comptes de service : lecteur, administrateur, sans rôle et avec une autre audience. Le flux est `client_credentials`, sans mot de passe utilisateur. Les rôles sont limités par des scope mappings explicites ; `fullScopeAllowed` est faux. Un mapper d’audience est distinct du mapper de rôles.

Les secrets du fixture sont publics, synthétiques et réservés à ce laboratoire local. Aucun compte réel ni donnée opérateur. Le mode développement, HTTP et le stockage éphémère servent uniquement à la démonstration. Ce modèle ne couvre ni navigateur/PKCE, ni session utilisateur, ni MFA, ni révocation immédiate, ni rotation des clés, ni transport MCP.

## Vérifier la preuve

`JwtBoundaryTest` couvre les claims signés localement. `KeycloakServiceAccountTest` couvre l’émission Keycloak et les rôles/audiences des comptes de service. `smoke.py` exerce le JAR lancé. La CI rejoue les 19 tests et les six contrôles du JAR avec JDK 17 et 21 ; ses résultats doivent être vérifiés à la tête de la PR. La matrice du toolkit racine reste indépendante.

Références : [validation JWT Spring Security](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html), [comptes de service Keycloak](https://www.keycloak.org/docs/latest/server_admin/index.html#_service_accounts), [audiences Keycloak](https://www.keycloak.org/docs/latest/server_admin/index.html#_audience), [RFC 6750 section 3](https://www.rfc-editor.org/rfc/rfc6750#section-3).

---

<div align="center">
  <img src="https://raw.githubusercontent.com/jihedbfr-art/jihedbfr-art/main/assets/brand/jihedailabs-logo.svg" alt="JihedAiLabs" width="120"/>
  <br/>
  <sub>Un projet <a href="https://github.com/jihedbfr-art"><b>JihedAiLabs</b></a></sub>
</div>
