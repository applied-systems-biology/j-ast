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

## Architecture Constraints

- **Plugin-based tasks**: All analysis workflows extend `BackendTaskWorkload` interface
- **JIPipe integration**: External Fiji workflow engine requires temp directory operations
- **Canvas processing**: Vue-Konva library initialized in boot files, not globally