# Guide de Test pour l'Intégration Stripe Connect

Ce guide explique comment tester l'intégration de Stripe Connect entre ms-shop-api et ms-payment-api.

## Prérequis

1. Les deux microservices doivent être en cours d'exécution
2. Une clé API Stripe de test doit être configurée dans `application.yaml` de ms-payment-api
3. Un shop doit exister dans la base de données

## Test 1: Mise à jour de l'ID de compte Stripe d'un shop

### Étape 1: Mettre à jour l'ID de compte Stripe d'un shop

Utilisez la commande curl suivante pour mettre à jour l'ID de compte Stripe d'un shop:

```bash
curl -X PUT http://localhost:8081/shop/{shopId}/stripe-account \
  -H "Content-Type: application/json" \
  -d '{"stripeAccountId": "acct_test123456789"}'
```

Remplacez `{shopId}` par l'ID réel du shop.

### Étape 2: Vérifier que l'ID de compte Stripe a été mis à jour

Utilisez la commande curl suivante pour vérifier que l'ID de compte Stripe a été mis à jour:

```bash
curl -X GET http://localhost:8081/shop/{shopId}/stripe-account
```

Remplacez `{shopId}` par l'ID réel du shop.

La réponse devrait contenir l'ID de compte Stripe que vous avez défini à l'étape précédente.

## Test 2: Processus d'onboarding Stripe Connect

### Étape 1: Démarrer l'onboarding d'un shop

Utilisez la commande curl suivante pour démarrer l'onboarding d'un shop:

```bash
curl -X POST http://localhost:8082/stripe/connect/onboard/{shopId}
```

Remplacez `{shopId}` par l'ID réel du shop.

La réponse devrait contenir une URL d'onboarding Stripe. Notez que cette URL est temporaire et expire après un certain temps.

### Étape 2: Vérifier que l'ID de compte Stripe a été mis à jour

Utilisez la commande curl suivante pour vérifier que l'ID de compte Stripe a été mis à jour:

```bash
curl -X GET http://localhost:8081/shop/{shopId}/stripe-account
```

Remplacez `{shopId}` par l'ID réel du shop.

La réponse devrait contenir l'ID de compte Stripe créé par Stripe lors de l'onboarding.

## Test 3: Processus de paiement de service avec Stripe Connect

> **Note importante**: La dernière mise à jour a modifié le comportement des paiements pour utiliser des charges directes vers les comptes connectés au lieu des charges de destination. Cela signifie que les paiements iront directement aux comptes Stripe des shops, sans passer par le compte de la plateforme.

### Étape 1: Créer une session de paiement pour un service

Utilisez la commande curl suivante pour créer une session de paiement pour un service:

```bash
curl -X POST http://localhost:8082/payments/checkout \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "00000000-0000-0000-0000-000000000001",
    "shopId": "{shopId}",
    "serviceId": "{serviceId}",
    "appointmentId": "{appointmentId}",
    "amount": 1000,
    "currency": "eur"
  }'
```

Remplacez:
- `{shopId}` par l'ID réel du shop
- `{serviceId}` par l'ID du service à payer
- `{appointmentId}` par l'ID du rendez-vous à confirmer

La réponse devrait contenir une URL de session Checkout Stripe. Cette URL peut être ouverte dans un navigateur pour simuler un paiement.

### Étape 2: Simuler un paiement réussi

Ouvrez l'URL de la session Checkout dans un navigateur et utilisez les informations de carte de test Stripe pour simuler un paiement réussi:
- Numéro de carte: 4242 4242 4242 4242
- Date d'expiration: n'importe quelle date future
- CVC: n'importe quel nombre à 3 chiffres
- Code postal: n'importe quel code postal valide

Après un paiement réussi, vous serez redirigé vers l'URL de succès configurée.

### Étape 3: Vérifier le statut du paiement

Le webhook Stripe devrait être appelé pour mettre à jour le statut du paiement. Vous pouvez vérifier les logs de ms-payment-api pour confirmer que le webhook a été appelé et que le statut du paiement a été mis à jour.

### Étape 3.1: Vérifier que le paiement est allé au compte connecté

Pour vérifier que le paiement est bien allé directement au compte Stripe du shop et non au compte de la plateforme, vous pouvez:

1. **Vérifier les logs de l'application**:
   ```
   grep "direct charge" /path/to/your/application.log
   ```
   Vous devriez voir des messages comme:
   ```
   Attempting direct charge to connected account: acct_XXXXX
   Calculated application fee: 100 (5.0% of 2000)
   Direct charge with application fee successful to connected account: acct_XXXXX
   ```

2. **Vérifier dans le Dashboard Stripe du shop**:
   - Connectez-vous au Dashboard Stripe du shop connecté
   - Allez dans la section "Paiements"
   - Vous devriez voir le paiement listé directement dans le compte du shop
   - Vous devriez également voir les frais d'application prélevés sur le paiement

3. **Vérifier dans le Dashboard Stripe de la plateforme**:
   - Connectez-vous au Dashboard Stripe de la plateforme
   - Allez dans la section "Connect" > "Comptes"
   - Trouvez le compte connecté du shop
   - Vérifiez les paiements associés à ce compte
   - Pour les charges directes, le paiement apparaîtra comme ayant été effectué directement sur le compte connecté
   - Vous devriez voir les frais d'application collectés dans la section "Connect" > "Frais d'application"

Si vous voyez dans les logs un message comme "Direct charge failed, falling back to platform payment", cela signifie que la charge directe a échoué et que le système est revenu à utiliser le compte de la plateforme pour le paiement. Dans ce cas, vérifiez que:
- Le compte connecté a bien la capacité "card_payments" activée
- Le compte connecté est correctement configuré et vérifié
- Les informations du compte sont correctement récupérées par l'application

### Étape 3.2: Vérifier les frais d'application

Pour vérifier que les frais d'application (commission de la plateforme) sont correctement prélevés:

1. **Vérifier le montant des frais dans les logs**:
   ```
   grep "application fee" /path/to/your/application.log
   ```
   Vous devriez voir des messages comme:
   ```
   Calculated application fee: 100 (5.0% of 2000)
   ```
   Cela indique que pour un paiement de 2000 centimes (20€), des frais de 100 centimes (1€) ont été prélevés.

2. **Vérifier les frais dans le Dashboard Stripe de la plateforme**:
   - Connectez-vous au Dashboard Stripe de la plateforme
   - Allez dans la section "Connect" > "Frais d'application"
   - Vous devriez voir les frais collectés pour chaque transaction

3. **Modifier le pourcentage de frais**:
   Pour tester avec un pourcentage de frais différent, modifiez la configuration dans `application.yaml`:
   ```yaml
   stripe:
     platform:
       fee_percentage: 10.0  # Changer à 10% pour les tests
   ```
   Ou définissez la variable d'environnement:
   ```bash
   export STRIPE_PLATFORM_FEE_PERCENTAGE=10.0
   ```
   Redémarrez l'application et effectuez un nouveau paiement pour vérifier que le nouveau pourcentage est appliqué.

### Étape 4: Vérifier le statut de paiement d'un rendez-vous

Utilisez la commande curl suivante pour vérifier le statut de paiement d'un rendez-vous:

```bash
curl -X GET http://localhost:8082/payments/appointment/{appointmentId}
```

Remplacez `{appointmentId}` par l'ID réel du rendez-vous.

La réponse devrait contenir les détails du paiement associé au rendez-vous, y compris son statut. Si le paiement a été effectué avec succès, le statut devrait être "COMPLETED" et le rendez-vous devrait être automatiquement confirmé.

Si aucun paiement n'est associé au rendez-vous, vous recevrez une réponse 404 Not Found.


## Notes importantes

- Pour les tests en environnement de développement, vous pouvez utiliser Stripe CLI pour transférer les webhooks Stripe vers votre environnement local.
- Les ID de compte Stripe commencent généralement par "acct_" suivi d'une chaîne alphanumérique.
- Les montants dans Stripe sont toujours en centimes (1000 = 10,00 €).
- Pour tester les paiements à partir du solde Stripe, assurez-vous que le compte connecté a un solde disponible suffisant. Vous pouvez ajouter des fonds au solde en effectuant un paiement au compte connecté.