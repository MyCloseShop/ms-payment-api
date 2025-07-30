# Documentation MS-Payment-API

## Vue d'ensemble

Le **MS-Payment-API** est un microservice Java Spring Boot dédié à la gestion des paiements et à l'intégration avec
Stripe Connect pour la plateforme MyCloseShop. Il permet aux shops de recevoir des paiements directement sur leurs
comptes Stripe tout en appliquant des frais de plateforme.

## Architecture technique

### Technologies utilisées

- **Java 21** avec Spring Boot 3.3.2
- **Spring Data JPA** pour la persistance
- **MySQL** comme base de données principale
- **H2** pour les tests
- **Stripe SDK Java** (version 24.16.0) pour l'intégration des paiements
- **RabbitMQ** pour la messagerie asynchrone
- **Flyway** pour les migrations de base de données
- **Maven** comme gestionnaire de dépendances
- **JWT** pour l'authentification

### Dépendances principales

```xml
<!-- Intégration Stripe -->
<dependency>
    <groupId>com.stripe</groupId>
    <artifactId>stripe-java</artifactId>
    <version>24.16.0</version>
</dependency>

<!-- API communes du projet -->
<dependency>
    <groupId>com.etna.gpe.mycloseshop</groupId>
    <artifactId>common-api</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>

<!-- Sécurité -->
<dependency>
    <groupId>com.etna.gpe.mycloseshop</groupId>
    <artifactId>security-api</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

## Modèle de données

### Entité Payment

L'entité principale qui représente un paiement dans le système :

```java
@Entity
@Table(name = "payment")
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false)
    private long amount;  // Montant en centimes
    
    @Column(nullable = false, length = 3)
    private String currency;  // Code devise (EUR, USD, etc.)
    
    @Column(name = "user_id", nullable = false)
    private UUID userId;  // ID de l'utilisateur qui paie
    
    @Column(name = "shop_id", nullable = false)
    private UUID shopId;  // ID du shop qui reçoit le paiement
    
    @Column(name = "appointment_id")
    private UUID appointmentId;  // ID du rendez-vous (optionnel)
    
    @Column(name = "service_id")
    private UUID serviceId;  // ID du service payé
    
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;  // Statut du paiement
    
    // Identifiants Stripe
    private String stripeSessionId;
    private String stripePaymentIntentId;
    private String stripeChargeId;
    
    // Timestamps automatiques
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

### Tables de base de données

Le système utilise deux tables principales :

1. **shop_stripe_accounts** : Stocke les comptes Stripe Connect des shops
2. **stripe_payments** : Historique détaillé des paiements avec commission

## Architecture des services

Le microservice suit le pattern **Interface-Implementation** pour une meilleure maintenabilité :

### Services principaux

#### 1. IStripeConnectService / StripeConnectService

Gère l'onboarding des shops sur Stripe Connect :

- Création de comptes connectés Stripe
- Génération des liens d'onboarding
- Gestion du processus de validation des comptes

#### 2. PaymentService / PaymentServiceImpl

Gère le cycle de vie des paiements :

- Création de sessions Checkout Stripe
- Traitement des paiements avec destination aux shops
- Gestion des remboursements
- Application des frais de plateforme

### Contrôleurs REST

#### PaymentController

```java
POST /api/payments/checkout    // Créer une session de paiement
GET  /api/payments/{id}        // Récupérer un paiement
POST /api/payments/{id}/refund // Initier un remboursement
```

#### StripeConnectController

```java
POST /api/stripe/connect/onboard/{shopId}  // Démarrer l'onboarding
GET  /api/stripe/connect/account/{shopId}  // Info compte Stripe
```

#### StripeWebhookController

```java
POST /api/stripe/webhook  // Réception des événements Stripe
```

## Configuration

### Variables d'environnement

```yaml
# Configuration Stripe
stripe:
  api:
    key: ${STRIPE_SECRET_KEY:sk_test_}
    version: ${STRIPE_API_VERSION:2025-03-31.preview}
  webhook:
    secret: ${STRIPE_WEBHOOK_SECRET:whsec_your_webhook_secret}
  platform:
    fee_percentage: ${STRIPE_PLATFORM_FEE_PERCENTAGE:5.0}

# URLs des microservices
microservices:
  shop-api:
    url: ${MS_SHOP_API_URL:http://localhost:8082/api/ms-shop-api}

# Configuration RabbitMQ
rabbitmq:
  exchange:
    payment: payment.exchange
    appointment: appointment.exchange
  queue:
    payment-completed: payment.completed.queue
    payment-refunded: payment.refunded.queue
  routing-keys:
    payment-completed: payment.completed
    payment-refunded: payment.refunded
```

## Fonctionnalités principales

### 1. Stripe Connect Integration

Le microservice utilise **Stripe Connect Accounts v2** pour permettre aux shops de recevoir des paiements directement :

- **Configuration Standard** : Comptes avec capacités customer, merchant et recipient
- **Onboarding automatisé** : Liens d'onboarding personnalisés
- **Validation automatique** : Vérification des capacités de paiement

### 2. Gestion des paiements

#### Processus de paiement

1. Création d'une session Checkout Stripe
2. Redirection vers l'interface de paiement Stripe
3. Traitement du paiement avec destination au shop
4. Application automatique des frais de plateforme
5. Notification via webhooks et messages RabbitMQ

#### Modèle de frais

- **Frais de plateforme configurables** (défaut : 5%)
- **Charges directes** vers les comptes des shops
- **Commission automatique** prélevée par Stripe

### 3. Messagerie asynchrone

Le microservice communique avec les autres services via RabbitMQ :

```yaml
# Événements émis
- payment.completed    # Paiement réussi
- payment.refunded     # Remboursement effectué

# Événements écoutés  
- appointment.confirmed  # Confirmation de RDV
- appointment.cancelled  # Annulation de RDV
```

### 4. Gestion des webhooks Stripe

Le microservice traite les événements Stripe importants :

- `checkout.session.completed` : Finalisation des paiements
- `payment_intent.succeeded` : Confirmation des paiements
- `account.updated` : Mise à jour des comptes connectés

## Sécurité

### Authentification

- **JWT tokens** pour l'authentification des utilisateurs
- **Validation des rôles** via prédicats personnalisés
- **Intégration OAuth2** pour les services externes

### Validation Stripe

- **Signature webhook** : Vérification de l'authenticité des événements
- **Clés API sécurisées** : Variables d'environnement pour les secrets
- **Mode test/production** : Configuration flexible selon l'environnement

## Installation et déploiement

### Prérequis

- Java 21+
- Maven 3.6+
- MySQL 8.0+
- RabbitMQ
- Compte Stripe avec API keys

### Installation locale

1. **Cloner le repository**

```bash
git clone <repository-url>
cd ms-payment-api
```

2. **Configuration de la base de données**

```bash
sh ./script/create_db.sh
```

3. **Configuration Stripe**
   Configurer les variables d'environnement :

```bash
export STRIPE_SECRET_KEY=sk_test_your_key
export STRIPE_WEBHOOK_SECRET=whsec_your_secret
```

4. **Lancement de l'application**

```bash
mvn spring-boot:run
```

### Docker

```bash
# Construction de l'image
docker build -t ms-payment-api .

# Lancement avec docker-compose
docker-compose up -d
```

## Tests

### Tests unitaires

```bash
mvn test
```

### Tests d'intégration

```bash
mvn verify
```

### Coverage

Le projet utilise JaCoCo pour la couverture de code. Les rapports sont générés dans `target/site/jacoco/`.

## Monitoring et observabilité

### Logs

- **Niveau DEBUG** pour le package principal
- **Niveau INFO** pour Spring Security et les services externes
- **Logs structurés** avec corrélation des requêtes

### Métriques

- Métriques Spring Boot Actuator
- Monitoring des performances Stripe
- Surveillance des files RabbitMQ

## Documentation API

### Swagger UI

La documentation interactive est disponible à : `http://localhost:8087/swagger-ui/index.html`

### Endpoints principaux

| Méthode | Endpoint                               | Description                   |
|---------|----------------------------------------|-------------------------------|
| POST    | `/api/payments/checkout`               | Créer une session de paiement |
| GET     | `/api/payments/{id}`                   | Récupérer un paiement         |
| POST    | `/api/payments/{id}/refund`            | Rembourser un paiement        |
| POST    | `/api/stripe/connect/onboard/{shopId}` | Onboarding Stripe             |
| GET     | `/api/stripe/connect/account/{shopId}` | Info compte Stripe            |
| POST    | `/api/stripe/webhook`                  | Webhooks Stripe               |

## Développement

### Conventions de code

- **Pattern Interface-Implementation** pour tous les services
- **Injection de dépendances** par constructeur
- **Validation des entrées** avec Bean Validation
- **Gestion d'erreurs** centralisée avec `@ControllerAdvice`

### Ajout de nouvelles fonctionnalités

1. **Nouveau service** :

```java
// Interface
public interface IMonService {
    void maMethode();
}

// Implémentation
@Service
public class MonServiceImpl implements IMonService {
    // ...
}
```

2. **Nouvelle entité** :

- Créer la table dans une migration Flyway
- Générer l'entité JPA
- Créer le repository associé

### Tests

- **Tests unitaires** avec Mockito
- **Tests d'intégration** avec TestContainers
- **Tests de contrat** pour les APIs Stripe

## Bonnes pratiques

### Sécurité

- Ne jamais exposer les clés API dans les logs
- Valider toutes les entrées utilisateur
- Utiliser HTTPS en production
- Vérifier les signatures des webhooks

### Performance

- Utiliser le cache pour les comptes Stripe fréquemment consultés
- Traitement asynchrone des webhooks
- Pagination pour les listes de paiements

### Monitoring

- Alertes sur les échecs de paiement
- Surveillance des taux d'erreur Stripe
- Monitoring des files RabbitMQ

## Dépannage

### Problèmes courants

1. **Erreur d'authentification Stripe**
    - Vérifier les clés API
    - Contrôler le mode test/production

2. **Échec de création de compte connecté**
    - Vérifier les permissions de l'API key
    - Contrôler la configuration du compte principal

3. **Webhooks non reçus**
    - Vérifier l'URL du webhook
    - Contrôler le secret de signature

### Logs utiles

```bash
# Logs du service de paiement
grep "PaymentService" application.log

# Logs des webhooks Stripe
grep "StripeWebhook" application.log
```

## Évolutions futures

### Roadmap technique

- Support de nouvelles méthodes de paiement (SEPA, Bancontact)
- Implémentation de la facturation récurrente
- Amélioration du système de reporting
- Migration vers Stripe Elements v2

### Optimisations

- Cache distribué pour les données Stripe
- Amélioration des performances des webhooks
- Monitoring avancé avec Micrometer

---

**Version** : 1.0.2-SNAPSHOT  
**Dernière mise à jour** : 30 juillet 2025  
**Auteur** : Équipe MyCloseShop
