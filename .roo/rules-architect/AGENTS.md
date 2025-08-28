# Project Architecture Rules (Non-Obvious Only)

## Core System Constraints

- **Plugin Task Discovery**: Tasks MUST use `@BackendTaskType` annotation with unique typeId - interface implementations alone are insufficient for registration
- **JIPipe Integration**: External workflow engine requires temp directory operations and file copying pipeline - cannot be replaced by native Java processing
- **Progress Tracking**: Real-time updates depend on polling `/task/{id}/running-log` every 2.5 seconds - WebSocket architecture not implemented

## Performance Bottlenecks

- **JobRunr Workers**: Only 2 background workers configured - limits concurrent task execution despite apparent parallelism
- **File Upload Limitation**: 10MB restriction prevents large image analysis workflows despite server capabilities
- **Xvfb Display Wrapper**: Each JIPipe process requires full GUI context with 8GB allocation - memory usage scales poorly

## Hidden Coupling

- **Frontend-Bind Task Parameters**: TypeScript interfaces in `types/backendTasks.ts` MUST match Java BackendTaskWorkload implementations - breaks if out of sync
- **Konva Initialization**: Canvas library must be initialized before any component rendering order-dependent operations
- **File Storage Reaper**: Automatic cleanup runs hourly but doesn't handle process crashes - manual cleanup may be required

## Security Implications

- **Hardcoded Credentials**: Admin credentials in config file present security vulnerability that cannot be changed without system redesign  
- **JWT Secret Management**: Secret key exposed in configuration prevents proper token rotation
- **CORS Configuration**: Development and production require different settings not easily configurable via environment variables