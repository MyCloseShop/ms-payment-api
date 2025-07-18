# 🧪 Guide de Test Rapide - Flux Stripe Connect Complet

## 🚀 Test du flux complet en 10 minutes

### 🔧 **Prérequis (2 min)**

1. **Lance l'application** :
   ```bash
   ./mvnw spring-boot:run
   ```

2. **Vérifie que l'app fonctionne** :
   ```bash
   curl http://localhost:8087/api/ms-payment-api/actuator/health
   ```

---

## 📋 **Étape 1 : Onboarding d'un Shop de Test (3 min)**

### 1.1 Créer un compte Stripe Connect

```bash
curl -X POST http://localhost:8087/api/ms-payment-api/stripe-connect/shops/1/create-account \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer fake-admin-token"
```

**Réponse attendue :**

```json
{
  "success": true,
  "message": "Compte Stripe créé avec succès",
  "stripeAccountId": "acct_test_xxxxx",
  "onboardingCompleted": false
}
```

### 1.2 Vérifier le statut du shop

```bash
curl http://localhost:8087/api/ms-payment-api/stripe-connect/shops/1/status
```

**Note :** En mode test, le shop sera automatiquement validé pour recevoir des paiements.

---

## 💳 **Étape 2 : Créer un Paiement de Test (2 min)**

```bash
curl -X POST http://localhost:8087/api/ms-payment-api/payments/create \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer fake-user-token" \
  -d '{
    "shopId": 1,
    "amount": 50.00,
    "commissionRate": 10.0,
    "description": "Test coiffeur - Coupe + Brushing",
    "successUrl": "http://localhost:3000/payment/success",
    "cancelUrl": "http://localhost:3000/payment/cancel"
  }'
```

**Réponse attendue :**

```json
{
  "paymentId": 1,
  "checkoutUrl": "https://checkout.stripe.com/c/pay/cs_test_xxxxx",
  "stripeCheckoutSessionId": "cs_test_xxxxx",
  "status": "PENDING",
  "message": "Paiement créé avec succès"
}
```

### 📱 **Test du paiement Stripe**

1. **Copie l'URL** `checkoutUrl` dans ton navigateur
2. **Utilise cette carte de test** : `4242 4242 4242 4242`
3. **Date d'expiration** : N'importe quelle date future (ex: 12/28)
4. **CVC** : N'importe quel 3 chiffres (ex: 123)
5. **Clique sur "Payer"**

---

## 🎣 **Étape 3 : Simuler le Webhook Stripe (1 min)**

### Option A : Webhook automatique (si configuré)

Le webhook sera appelé automatiquement après le paiement test.

### Option B : Simuler manuellement le webhook

```bash
curl -X POST http://localhost:8087/api/ms-payment-api/stripe-webhook \
  -H "Content-Type: application/json" \
  -H "Stripe-Signature: t=1699999999,v1=fake-signature-for-test" \
  -d '{
    "id": "evt_test_123",
    "type": "checkout.session.completed",
    "data": {
      "object": {
        "id": "cs_test_xxxxx",
        "payment_status": "paid",
        "payment_intent": "pi_test_xxxxx"
      }
    }
  }'
```

**Note :** Remplace `cs_test_xxxxx` par l'ID de session reçu à l'étape 2.

---

## 📊 **Étape 4 : Vérifier les Résultats (2 min)**

### 4.1 Vérifier que le paiement est confirmé

```bash
curl http://localhost:8087/api/ms-payment-api/payments/1/refund-eligible
```

**Réponse attendue :** `true`

### 4.2 Vérifier les logs de l'application

Dans les logs, tu devrais voir :

```
🧪 MODE TEST STRIPE ACTIVÉ - Aucune vraie transaction ne sera effectuée
INFO - Paiement confirmé avec succès: 1
INFO - Événement PAYMENT_COMPLETED publié pour le paiement: 1
INFO - Événement APPOINTMENT_CONFIRMED publié pour le paiement: 1
```

### 4.3 Vérifier la base de données

```sql
-- Se connecter à ta base H2 (pour les tests) ou MySQL
SELECT *
FROM stripe_payments
WHERE id = 1;
-- Status devrait être 'COMPLETED'

SELECT *
FROM shop_stripe_accounts
WHERE shop_id = 1;
-- Devrait contenir l'account Stripe du shop
```

---

## 🔄 **Étape 5 : Tester le Remboursement (1 min)**

```bash
curl -X POST "http://localhost:8087/api/ms-payment-api/payments/1/refund?amount=50.00" \
  -H "Authorization: Bearer fake-user-token"
```

**Réponse attendue :** `"Remboursement effectué avec succès"`

---

## 🧪 **Autres Tests Rapides**

### Test de carte qui échoue

```bash
# Utilise cette carte de test : 4000 0000 0000 0002
# Elle simulera un échec de paiement
```

### Test de vérification d'éligibilité remboursement

```bash
# Après 48h simulées (change la date en base) :
curl http://localhost:8087/api/ms-payment-api/payments/1/refund-eligible
# Devrait retourner false
```

### Test avec commission différente

```bash
curl -X POST http://localhost:8087/api/ms-payment-api/payments/create \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer fake-user-token" \
  -d '{
    "shopId": 1,
    "amount": 100.00,
    "commissionRate": 15.0,
    "description": "Test commission 15%"
  }'
```

---

## 📋 **Checklist de Test Complet**

- [ ] ✅ Application démarre sans erreur
- [ ] ✅ Shop peut être créé via API
- [ ] ✅ Paiement peut être créé (retourne checkoutUrl)
- [ ] ✅ Page Stripe Checkout s'affiche correctement
- [ ] ✅ Paiement test avec `4242 4242 4242 4242` fonctionne
- [ ] ✅ Webhook est reçu et traité (logs)
- [ ] ✅ Statut paiement passe à `COMPLETED`
- [ ] ✅ Messages RabbitMQ sont publiés (logs)
- [ ] ✅ Remboursement fonctionne dans les 48h
- [ ] ✅ Remboursement échoue après 48h

---

## 🎯 **Résultat Final Attendu**

Après ce test, tu auras validé :

- ✅ **Onboarding Stripe Connect** : Shop peut recevoir des paiements
- ✅ **Création de paiement** : API fonctionne, session Stripe créée
- ✅ **Traitement paiement** : Stripe Checkout + webhook
- ✅ **Confirmation automatique** : Base de données mise à jour
- ✅ **Événements RabbitMQ** : Messages publiés vers autres services
- ✅ **Remboursement 48h** : Logique métier respectée

**🎉 Ton MVP Stripe Connect est fonctionnel !**

---

## 🚨 **Si quelque chose ne marche pas**

### Problèmes courants :

1. **Erreur JWT** : L'authentification n'est pas configurée pour les tests
    - **Solution** : Désactive temporairement la sécurité pour les tests

2. **Webhook signature error** :
    - **Solution** : Utilise une fausse signature pour les tests locaux

3. **RabbitMQ connection error** :
    - **Solution** : Lance RabbitMQ avec Docker ou désactive temporairement

### Commandes de debug :

```bash
# Voir les logs en temps réel
tail -f logs/spring.log

# Vérifier les tables créées
curl http://localhost:8087/api/ms-payment-api/h2-console

# Vérifier les endpoints disponibles
curl http://localhost:8087/api/ms-payment-api/actuator/mappings
```

Ce guide te permet de tester **tout le flux en 10 minutes max** ! 🚀
