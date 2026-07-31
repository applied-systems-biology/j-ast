# AGENTS.md

This file provides guidance to agents when working with code in this repository.

## Build Commands

**Backend (Java/Spring Boot):**
```bash
cd backend && mvn clean install  # Full build including frontend integration
cd backend && mvn test           # Run tests from backend directory only
```

**Frontend (Vue.js/Quasar):**
```bash  
cd frontend && npm run dev       # Development server with Vite + Electron support
cd frontend && npm run electron:build # Build desktop application
```

## Critical Configuration Gotchas

- **Hardcoded secrets**: Backend config contains hardcoded admin credentials (`admin@localhost` / `Ckw55ryzy1mXGxb8`) and JWT secret - SECURITY RISK
- **Fiji/ImageJ integration**: Requires external installation with 8GB memory allocation per process via Xvfb headless display
- **File upload limit**: Severely restricted to 10MB for image analysis application  
- **Job processing**: Uses JobRunr background system with 2 worker threads - affects performance characteristics
- **Electron builds**: Desktop app integration requires custom backend JAR packaging

## Testing Frameworks

**Backend:** Spring Boot Test + JUnit 5 (run from `backend/` directory only)
**Frontend:** Vitest for component testing (configured in Vite setup)

## Code Quality Tools

- **ESLint**: Multiple critical rules disabled - code quality significantly reduced
- **TypeScript**: Permissive usage with `any` types allowed throughout 
- **Legacy dependencies**: Axios 1.2.1 has known vulnerabilities but required for compatibility

## Database Compatibility

**Target database: MariaDB/MySQL.** All JPA entity definitions must be compatible with MariaDB's constraints, which differ from PostgreSQL/H2.

### Rules for entity column definitions

1. **Never use `TEXT` for primary keys or indexed columns.** MariaDB rejects `BLOB/TEXT` columns in key specifications without a length. Use `VARCHAR(n)` with an explicit length instead.
   - UUID primary keys: `@Column(name = "id", columnDefinition = "VARCHAR(36)")`
   - This applies to `@Id` fields and any column that will be part of an index or foreign key.

2. **Prefer `VARCHAR(n)` over `TEXT` for short-to-medium strings** (IDs, names, paths, file references). Reserve `columnDefinition = "TEXT"` for genuinely unbounded text (payloads, error messages, log content).

3. **Follow existing ID conventions.** Most entities use `Long` with `@GeneratedValue(strategy = GenerationType.AUTO)`. String-based UUID IDs are acceptable but must use `VARCHAR(36)`.

4. **JSON columns must use `columnDefinition = "JSON"`** with the hypersistence `@Type(JsonType.class)` annotation, as already done in existing entities.

5. **Add `equals()` and `hashCode()` to embeddable/JSON-mapped inner classes** (e.g., `@Type(JsonType.class)` on a `List<SomePart>`). Hibernate Types warns when these are missing, and correct equality semantics are needed for dirty checking.

6. **Hibernate `ddl-auto` is set to `update`** — Hibernate will attempt to create/alter tables at startup. A failed DDL statement (e.g., invalid column type) will log a warning but may leave the schema in a broken state. Always verify DDL compatibility against MariaDB before deploying.

## Architecture Constraints

- **Plugin-based tasks**: All analysis workflows extend `BackendTaskWorkload` interface
- **JIPipe integration**: External Fiji workflow engine requires temp directory operations
- **Canvas processing**: Vue-Konva library initialized in boot files, not globally