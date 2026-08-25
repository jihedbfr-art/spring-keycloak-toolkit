# Security Policy

## Scope

This is a library consumed as a dependency, and it sits directly on the authorization path: it maps Keycloak realm and resource roles onto Spring Security authorities. A mapping bug here can silently grant or deny access in every application that depends on it. Reports against this repository get priority over the rest of the ecosystem.

## Supported versions

Only the `main` branch is maintained. Fixes land on the latest version; older releases are not patched.

## Reporting a vulnerability

**Do not open a public issue, and do not send email.** A public issue tells everyone
about the problem before there is a fix.

Report it through GitHub Security Advisories, which keeps the report private until a
fix is ready:

1. Open the **Security** tab of this repository.
2. Go to **Advisories**.
3. Click **Report a vulnerability**.

A useful report says what the flaw is, how to reproduce it, and what an attacker gets
out of it. A proof of concept helps more than a description.

## What to expect

This repository is maintained by one person, so treat the timeline accordingly: an
acknowledgement within a week, and an honest answer about whether the issue will be
fixed, rather than silence. If a fix is accepted, disclosure is coordinated with you.

If a report turns out not to be a vulnerability, you will be told why.

---

<div align="center">
  <img src="assets/brand/jihedailabs-logo.svg" alt="JihedAiLabs" width="120"/>
  <br/>
  <sub>A <a href="https://github.com/jihedbfr-art"><b>JihedAiLabs</b></a> project</sub>
  <br/>
  <sub><a href="./SECURITY.fr.md">Version française</a></sub>
</div>
