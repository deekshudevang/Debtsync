# DebtSync X

<div align="center">
  <img src="https://img.shields.io/badge/Status-Production_Ready-success?style=flat-square"/>
  <img src="https://img.shields.io/badge/Architecture-Clean_MVVM-blue?style=flat-square"/>
  <img src="https://img.shields.io/badge/UI-Material_3_Dark-magenta?style=flat-square"/>
</div>

<br/>

**Track, pay, and reconcile together, seamlessly.**

DebtSync X is a professional-grade peer-to-peer debt tracking and settlement application. Designed for students, roommates, and professionals who frequently split bills or borrow money, it eliminates the awkwardness of asking for your money back. 

With intelligent deep-linking to UPI apps, AI voice intent capabilities, and a premium "Midnight Obsidian" dark-mode layout, DebtSync X feels like a real startup product, not a student project.

---

## 🚀 Key Innovation Features

* **Smart UPI Redirect (Deep Linked)**: Instantly launch GPay, PhonePe, Paytm, or Cred pre-filled with the exact owed amount and contact name.
* **Biometric Vault Integration**: Fingerprint and 4-digit PIN lock screen ensures your financial data stays completely private.
* **AI Voice Transaction Parsing**: Tap the mic and say *"Rahul owes me 500"* – our voice intent pipeline parses the command.
* **Premium Dark Fintech UI**: A sleek, minimal, glassmorphic design language designed to mimic high-end tier-1 fintech products like Cred.
* **Offline-First with Google Cloud Sync**: Fully local Room DB ensures 100% functionality without internet. Connect with your Google Account for cross-device cloud backups.

## 📱 Tech Stack & Architecture

DebtSync X relies on a modern, robust architecture built strictly around Android best practices:

* **UI Engine**: Jetpack Compose with Material Design 3 and canonical layouts.
* **Architecture**: Clean Architecture / MVVM (Model-View-ViewModel).
* **Local Persistence**: Room Database (SQLite) with optimized Flow observables.
* **Authentication & Sync**: CredentialManager (Google Sign-In) and exportable JSON sync scripts.
* **Security Modules**: Android `androidx.biometric` integration.
* **Concurrency**: Kotlin Coroutines & Flows.

## 🗺️ Execution Roadmap

- [x] **Phase 1**: Professional UI, UPI payment redirect, Clean architecture, Onboarding, Fingerprint Auth.
- [x] **Phase 2**: Local Database Sync, Google Account Integration, Analytics graphs, Realtime updates.
- [ ] **Phase 3**: Split bills optimization, AI chat parsing, Group settlements.

---
Built with passion to solve real emotional friction when handling peer-to-peer debts.
