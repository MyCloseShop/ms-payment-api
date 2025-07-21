# MS-PAYMENT-API

## Microservice de paiement

### Installation et configuration

#### Création de la base de données et lancement du conteneur Docker
    
```bash
sh ./script/create_db.sh
```

#### Suppression de la base de données et arrêt du conteneur Docker

```bash
docker compose down
```

Le script exécutera docker compose et créera la base de données avec le fichier SQL.

#### Configuration de Stripe

Pour utiliser ce microservice, vous devez configurer les variables suivantes dans le fichier `application.yaml` :

```yaml
stripe:
  api:
    key: sk_test_VOTRE_CLE_API_STRIPE
    version: 2025-03-31.preview
```

### Description

Ce microservice est responsable des paiements et de l'intégration avec Stripe Connect. Il permet :

1. **Paiements standard** : Traitement des paiements des utilisateurs via Stripe
2. **Intégration Stripe Connect** : Onboarding des shops en tant que comptes connectés Stripe

### Fonctionnalités principales

#### 1. Intégration Stripe Connect (Accounts v2)

Le microservice utilise l'API Stripe Connect Accounts v2 pour créer et gérer des comptes connectés pour les shops. Chaque compte connecté est configuré avec :

- Configuration **customer** : Permet au compte de payer des frais d'abonnement
- Configuration **merchant** : Permet au compte d'accepter des paiements par carte
- Configuration **recipient** : Permet les transferts entre le compte de la plateforme et le compte connecté

##### Méthodes de paiement pour les comptes connectés

Le microservice prend en charge deux méthodes de paiement pour les comptes connectés :

1. **Charges directes** (méthode principale) : Les paiements vont directement au compte Stripe du shop sans passer par le compte de la plateforme. Cette méthode est utilisée en priorité lorsque le compte connecté est correctement configuré.

2. **Charges via la plateforme** (méthode de secours) : Les paiements passent par le compte de la plateforme. Cette méthode est utilisée comme solution de secours si les charges directes échouent.

##### Modèle de frais de plateforme

Le microservice implémente un modèle de frais de plateforme pour générer des revenus sur chaque transaction :

1. **Frais d'application** : Un pourcentage configurable est prélevé sur chaque transaction effectuée via les charges directes. Ces frais sont automatiquement collectés par Stripe et transférés au compte de la plateforme.

2. **Configuration des frais** : Le pourcentage de frais est configurable dans le fichier `application.yaml` ou via la variable d'environnement `STRIPE_PLATFORM_FEE_PERCENTAGE`. Par défaut, il est fixé à 5%.

```yaml
stripe:
  platform:
    fee_percentage: 5.0  # 5% de frais sur chaque transaction
```


### Endpoints

#### Stripe Connect

- `POST /stripe/connect/onboard/{shopId}` : Démarre le processus d'onboarding pour un shop
- `GET /stripe/connect/account/{shopId}` : Récupère les informations du compte Stripe d'un shop


#### Paiements standard

- `POST /payments/checkout` : Crée une session de paiement Checkout
- `GET /payments/{paymentId}` : Récupère les informations d'un paiement
- `POST /payments/webhook` : Endpoint pour les webhooks Stripe liés aux paiements

### Documentation Swagger

La documentation Swagger est disponible à l'adresse : http://localhost:8082/swagger-ui/index.html

### Guide de test

Pour tester l'intégration Stripe Connect et la facturation des frais SaaS, consultez le [Guide de test](TESTING_GUIDE.md).

### Architecture et Design Patterns

Ce microservice utilise une architecture basée sur le pattern Interface-Implementation pour favoriser la modularité, la testabilité et la maintenabilité du code :

- **Pattern Interface-Implementation** : Chaque service et contrôleur possède une interface qui définit son contrat, et une classe d'implémentation qui fournit le comportement concret.
  - Exemple : `IStripeConnectService` (interface) et `StripeConnectService` (implémentation)
  - Avantages : Facilite les tests unitaires via le mocking, permet de changer d'implémentation sans modifier les clients, et améliore la lisibilité du code.

- **Injection de dépendances** : Les dépendances sont injectées via le constructeur, ce qui facilite les tests et réduit le couplage entre les composants.

- **Pattern Repository** : Utilisation du pattern Repository pour l'accès aux données via Spring Data JPA.

### Développement

Si vous souhaitez créer une nouvelle entité (nouvelle table), créez la table dans le fichier SQL et générez l'entité à partir de cette table avec JPA Buddy.

Pour ajouter un nouveau service ou contrôleur, suivez le pattern Interface-Implementation :
1. Créez d'abord l'interface (ex: `IMonService`)
2. Implémentez ensuite cette interface (ex: `MonServiceImpl`)
3. Utilisez l'interface plutôt que l'implémentation dans les dépendances des autres composants