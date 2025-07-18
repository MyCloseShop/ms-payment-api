# 💳 Microservice de Paiement Stripe Connect - MVP

## 🎯 Vue d'ensemble

Ce microservice gère les paiements avec **Stripe Connect** pour une plateforme de réservation de rendez-vous. Les
utilisateurs paient directement aux shops avec une **commission variable** prélevée automatiquement. Remboursement
possible dans les **48h** après paiement.

## 🔄 Flux de paiement complet (Bout en bout)

### 📋 **Prérequis avant premier paiement**

1. **Shop doit avoir un compte Stripe Connect configuré**
2. **Utilisateur doit être authentifié** (JWT)
3. **Rendez-vous doit être créé** dans le système

---

## 🚀 **Phase 1 : Onboarding du Shop (Une seule fois)**

### 1.1 Créer un compte Stripe Connect pour le shop

```bash
POST /api/ms-payment-api/stripe-connect/shops/{shopId}/create-account
Authorization: Bearer ADMIN_JWT_TOKEN
```

**Réponse :**

```json
{
  "success": true,
  "message": "Compte Stripe créé avec succès",
  "stripeAccountId": "acct_1Hxxxxx",
  "onboardingCompleted": false
}
```

### 1.2 Générer le lien d'onboarding

```bash
POST /api/ms-payment-api/stripe-connect/shops/{shopId}/onboarding-link
Content-Type: application/json

{
  "returnUrl": "https://monapp.com/onboarding/success",
  "refreshUrl": "https://monapp.com/onboarding/refresh"
}
```

**Réponse :**

```json
{
  "success": true,
  "onboardingUrl": "https://connect.stripe.com/setup/s/cTxxxxx"
}
```

### 1.3 Shop complète son onboarding

- Le shop clique sur `onboardingUrl`
- Renseigne ses informations bancaires sur Stripe
- Stripe vérifie automatiquement son identité (KYC)
- **Webhook automatique** met à jour le statut en base

### 1.4 Vérifier que le shop peut recevoir des paiements

```bash
GET /api/ms-payment-api/stripe-connect/shops/{shopId}/status
```

**Réponse :**

```json
{
  "shopId": 1,
  "stripeAccountId": "acct_1Hxxxxx",
  "onboardingCompleted": true,
  "chargesEnabled": true,
  "payoutsEnabled": true,
  "canReceivePayments": true
}
```

---

## 💰 **Phase 2 : Création d'un paiement (À chaque réservation)**

### 2.1 L'utilisateur veut réserver un rendez-vous

**Frontend → Backend :** Création du paiement

```bash
POST /api/ms-payment-api/payments/create
Authorization: Bearer USER_JWT_TOKEN
Content-Type: application/json

{
  "shopId": 1,
  "amount": 50.00,
  "commissionRate": 10.0,
  "description": "Coupe + Brushing - Salon Marie",
  "successUrl": "https://monapp.com/payment/success",
  "cancelUrl": "https://monapp.com/payment/cancel"
}
```

### 2.2 Le microservice crée une session Stripe Checkout

**Réponse du microservice :**

```json
{
  "paymentId": 123,
  "checkoutUrl": "https://checkout.stripe.com/c/pay/cs_test_xxxxx",
  "stripeCheckoutSessionId": "cs_test_xxxxx",
  "status": "PENDING",
  "message": "Paiement créé avec succès"
}
```

**Ce qui se passe côté Stripe :**

- ✅ Session Checkout créée sur le **compte du shop**
- ✅ Commission de **5€** (10% de 50€) configurée
- ✅ L'argent ira **directement au shop** (45€ net)
- ✅ Paiement sauvegardé en base avec statut `PENDING`

### 2.3 L'utilisateur est redirigé vers Stripe

**Frontend :** Redirection vers `checkoutUrl`

- ✅ Interface Stripe sécurisée
- ✅ L'utilisateur saisit ses informations de carte
- ✅ **Mode TEST** : Peut utiliser `4242 4242 4242 4242`

---

## ⚡ **Phase 3 : Traitement automatique du paiement**

### 3.1 Stripe traite le paiement

**Si paiement réussi :**

- ✅ Stripe débite la carte de l'utilisateur (50€)
- ✅ Stripe vire **45€** au shop (montant - commission)
- ✅ **5€ de commission** transférée sur le compte plateforme
- ✅ Stripe envoie un **webhook** à notre microservice

### 3.2 Webhook automatique confirme le paiement

```bash
POST /api/ms-payment-api/stripe-webhook
Stripe-Signature: t=1234567890,v1=xxxxx
Content-Type: application/json

{
  "type": "checkout.session.completed",
  "data": {
    "object": {
      "id": "cs_test_xxxxx",
      "payment_status": "paid"
    }
  }
}
```

**Traitement automatique du webhook :**

1. ✅ Vérification de la signature Stripe (sécurité)
2. ✅ Mise à jour du paiement : `PENDING` → `COMPLETED`
3. ✅ **Publication d'un événement RabbitMQ**

### 3.3 Message RabbitMQ envoyé au service de rendez-vous

**Queue :** `payment.completed.queue`

```json
{
  "eventType": "PAYMENT_COMPLETED",
  "paymentId": 123,
  "userId": 456,
  "shopId": 1,
  "appointmentId": 789,
  "amount": 50.00,
  "commissionAmount": 5.00,
  "currency": "EUR",
  "stripePaymentIntentId": "pi_xxxxx",
  "timestamp": "2025-07-18T14:30:00Z"
}
```

**Et aussi :** `appointment.confirmed.queue`

```json
{
  "eventType": "APPOINTMENT_CONFIRMED",
  "paymentId": 123,
  "userId": 456,
  "shopId": 1,
  "appointmentId": 789,
  "amount": 50.00,
  "timestamp": "2025-07-18T14:30:00Z"
}
```

### 3.4 Service de rendez-vous traite l'événement

**Le microservice de rendez-vous :**

- ✅ Écoute la queue `appointment.confirmed.queue`
- ✅ Met à jour le rendez-vous : `PENDING` → `CONFIRMED`
- ✅ Envoie une confirmation par email/SMS au client
- ✅ **Rendez-vous définitivement confirmé**

---

## 🔄 **Phase 4 : Remboursement (Si annulation dans les 48h)**

### 4.1 Vérifier l'éligibilité au remboursement

```bash
GET /api/ms-payment-api/payments/123/refund-eligible
Authorization: Bearer USER_JWT_TOKEN
```

**Réponse :**

```
true  // ou false si > 48h
```

### 4.2 Effectuer le remboursement

```bash
POST /api/ms-payment-api/payments/123/refund?amount=50.00
Authorization: Bearer USER_JWT_TOKEN
```

**Traitement automatique :**

1. ✅ Vérification : < 48h après paiement
2. ✅ **Remboursement Stripe** de 50€ sur la carte
3. ✅ **Commission remboursée** (remboursement complet)
4. ✅ Shop débité de 45€ sur son solde Stripe
5. ✅ Statut paiement : `COMPLETED` → `REFUNDED`

### 4.3 Messages RabbitMQ pour annulation

**Queue :** `payment.refunded.queue` + `appointment.cancelled.queue`

**Le service de rendez-vous :**

- ✅ Annule automatiquement le rendez-vous
- ✅ Libère le créneau pour un autre client
- ✅ Notifie le shop et le client

---

## 📊 **Suivi des flux financiers**

### Exemple concret pour un paiement de 50€ (commission 10%) :

1. **Utilisateur paie :** 50€
2. **Shop reçoit :** 45€ (après commission et frais Stripe ~1.4%)
3. **Plateforme reçoit :** 5€ de commission
4. **Frais Stripe :** ~0.6€ (prélevés sur le shop)

### En cas de remboursement complet :

1. **Utilisateur récupère :** 50€
2. **Shop est débité de :** 45€
3. **Plateforme perd :** 5€ de commission
4. **Frais Stripe :** Non remboursés (~0.6€ perdus par le shop)

---

## 🎯 **Récapitulatif du flux MVP**

| Étape | Acteur           | Action                    | Résultat                    |
|-------|------------------|---------------------------|-----------------------------|
| 1     | **Shop**         | Onboarding Stripe Connect | Peut recevoir des paiements |
| 2     | **User**         | Réserve + Paie            | Paiement créé (`PENDING`)   |
| 3     | **Stripe**       | Traite le paiement        | Webhook → Confirmation auto |
| 4     | **Microservice** | Reçoit webhook            | Paiement `COMPLETED`        |
| 5     | **RabbitMQ**     | Événement publié          | Service rendez-vous notifié |
| 6     | **Service RDV**  | Confirme rendez-vous      | Client et shop informés     |

## 🧪 **Mode Test - Zéro risque**

- ✅ **Aucune vraie transaction** avec les clés `sk_test_...`
- ✅ **Cartes de test** : `4242 4242 4242 4242` (succès)
- ✅ **Webhooks fonctionnels** : Même logique qu'en production
- ✅ **Dashboard séparé** : https://dashboard.stripe.com/test

## 🚀 **Démarrage rapide**

```bash
# 1. Configure tes clés Stripe de test
cp .env.example .env
# Édite .env avec tes clés pk_test_... et sk_test_...

# 2. Lance l'application
./mvnw spring-boot:run

# 3. Teste le flux complet
# - Onboarding d'un shop
# - Création d'un paiement  
# - Simulation d'un webhook
```

Le flux est maintenant **100% automatisé et sécurisé** pour ton MVP ! 🎉
