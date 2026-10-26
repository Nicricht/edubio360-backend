# Security Remediation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove high/critical Snyk findings while preserving the EP2 quality gate and business APIs.

**Architecture:** Upgrade the Maven reactor's supported release trains, adapt library API imports to Spring Boot 4, and configure targeted CSRF exemption for bearer-token JSON APIs while keeping CSRF enabled elsewhere. Iterate against GitHub Actions for full JVM/Snyk/Docker validation.

**Tech Stack:** Java 21, Maven, Spring Boot, Spring Cloud, Snyk, JUnit, Mockito, GitHub Actions.

**Spec:** `docs/superpowers/specs/2026-10-26-security-remediation-design.md`

## Global Constraints

- Keep all eight Maven modules and Java 21.
- Do not allow CI security scans to continue on failure or reduce severity thresholds.
- Keep `main` untouched; validation branch and draft PR only.

## Review Focus

1. Public JSON login/register still work without a CSRF session token.
2. Unsafe non-API POST requests must not bypass CSRF controls.
3. JWT gateway auth and existing roles remain functional after Cloud upgrade.
4. Eureka, MySQL, RabbitMQ and healthchecks survive Compose deployment.
5. Scans must fail if any high/critical finding remains.

---

### Task 1: Update vulnerable dependency train

**Files:** `pom.xml`, per-module `pom.xml`, Java files affected by migration, `tests/ci/test_security_upgrade.py`.

**Interfaces:** Consumes Maven `cl.edubio360:edubio360-microservices` reactor; produces same executable module JAR paths and preserved APIs.

- [ ] Write a test requiring a supported Spring Boot 4 and Cloud 2025.1 dependency set.
- [ ] Run the new test and observe it fail against the vulnerable POM.
- [ ] Migrate parent/BOM/library versions and renamed artifacts/imports.
- [ ] Run the Python contract suite and the Maven reactor in GitHub Actions; resolve only root-cause incompatibilities.
- [ ] Commit only after tests demonstrate the transition.

### Task 2: Fix CSRF finding without breaking JSON endpoints

**Files:** `auth-service/src/main/java/cl/edubio360/auth/config/SecurityConfig.java`, `auth-service/src/test/java/cl/edubio360/auth/controller/AuthControllerTest.java`.

**Interfaces:** Preserves `SecurityFilterChain securityFilterChain(HttpSecurity)` and `/api/auth/{register,login}` JSON contract.

- [ ] Add MVC test that proves a non-API unsafe POST is CSRF-protected.
- [ ] Run the test and observe failure with globally disabled CSRF.
- [ ] Keep CSRF active for non-API routes; exempt stateless JWT/JSON API routes only.
- [ ] Run complete Maven tests and the Snyk Code job; inspect findings.
- [ ] Commit with a short note explaining bearer-token route exemption.

### Task 3: Re-run gated delivery and document evidence

**Files:** `README.md`, workflow file only if needed for a verified root-cause correction.

**Interfaces:** Consumes results of Tasks 1-2 and the existing `verify → security → image-build → deploy-smoke` DAG.

- [ ] Run the complete CI workflow on the isolated branch.
- [ ] Inspect vulnerability counts, JARs, Docker image build and full-stack smoke logs.
- [ ] Document actual results and remaining restrictions; never record a green result without evidence.
- [ ] Commit and update the existing draft pull request, leaving `main` unchanged.
