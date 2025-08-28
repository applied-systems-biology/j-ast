# Project Documentation Rules (Non-Obvious Only)

## Structure Misconceptions

- **Frontend `src/` directory**: Contains VSCode extension code, not web app source (counterintuitive)
- **Backend tasks package**: `org.hkijena.jast.tasks.workloads/` contains actual implementations - base interfaces are misleading
- **File storage paths**: Configured separately from build system - manual intervention required for deployment

## Hidden Dependencies

- **JIPipe Plugin System**: External Fiji workflows require `.jipipe` files with special parameter prefixes (`__jast__result-name`, etc.)
- **Konva Canvas Library**: Initialized in boot file but not documented in component usage examples
- **Desktop Application**: Electron build capability exists but requires manual backend JAR packaging

## API Patterns

- **Task Polling**: `/task/{id}/running-log` endpoint returns incremental log chunks - must handle partial responses
- **Progress Tracking**: Uses custom `ProgressInfo` class instead of standard Spring progress mechanisms
- **File Operations**: All file references go through `FileStorageService` with automatic cleanup cycles

## Version Constraints

- **Legacy Axios**: 1.2.1 required for compatibility despite vulnerabilities - cannot upgrade without breaking changes
- **Quasar Framework**: Older configuration targets browsers from 2020+ but Electron builds require specific setup