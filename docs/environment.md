# Inspected environment

- Existing project: untouched Spring Boot starter, reused in place. No Git operations performed.
- Spring Boot 4.1.1, Java target 25, Maven wrapper 3.9.16 retained.
- Working JDK: `C:\Users\LENOVO\.jdks\openjdk-25.0.1`. The Oracle PATH launcher returned no useful output; scripts select the working JDK without global changes.
- Node 24.14.1 / npm 11.18.0 available. Frontend uses browser ES modules; no frontend dependency installation required.
- PostgreSQL 18 Windows service already running. The new dedicated sentinelx database was created with user authorization on 15 September 2026; unrelated databases were not modified.
- Docker CLI installed; Linux engine was unavailable during initial inspection.
- Redis not required for P0. Gateway traffic state is process-local; this limitation is explicit.
- Local development uses PostgreSQL on port 5432, with connection settings in ignored backend/data/postgresql.json. H2 is restricted to isolated automated tests.

Framework compatibility reference: [Spring Boot documentation](https://docs.spring.io/spring-boot/).


