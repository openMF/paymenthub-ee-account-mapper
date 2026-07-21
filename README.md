# paymenthub-ee-account-mapper

A Payment Hub EE service that stores and resolves the link between a beneficiary's identity and their payment account, so payments know where to go.

[![License](https://img.shields.io/badge/License-MPL--2.0-blue.svg)](LICENSE)

## What it does

- Registers beneficiaries, linking each identity (a functional or foundational ID, scoped by the registering institution) to exactly one payment account and modality.
- Looks up the payment account for a given identity, one request at a time or in batches.
- Fetches the list of registered beneficiaries and updates their identity or payment details.
- Persists everything to MySQL, with the schema created and versioned by Flyway migrations.
- Exposes REST APIs (documented with OpenAPI/Swagger) and connects to Zeebe (Camunda) for workflow steps.
- Includes a `validator` module (the former `ph-ee-id-account-validator-impl`) that can check an account against a switch, with GSMA and Mojaloop connectors.

## How it fits into Payment Hub EE

In G2P and bulk payment flows, the switch knows a beneficiary by their identity, not by a raw account number. This service answers the question "which account and payment modality does this identity map to?" for the institution making the payment. Other Payment Hub EE components call it to turn an identity into a concrete payment target before money moves, and the validator module can confirm that the resolved account is reachable on the connected switch.

## Tech stack

- Java 21
- Spring Boot 3.4 (Web, Data JPA, Actuator, Cache/Caffeine)
- Jakarta EE 10
- MySQL with Flyway migrations
- Zeebe (Camunda) client
- Gradle multi-module build: the `account-mapper` service plus the `validator` module
- Depends on `paymenthub-ee-bom` and `paymenthub-ee-core`

## Branches

- `dev` is the active development branch — all PRs should target `dev`.
- `main` holds released versions.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) and our [Code of Conduct](CODE_OF_CONDUCT.md).
