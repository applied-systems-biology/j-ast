# Project Coding Rules (Non-Obvious Only)

## Backend Java Development

- **Task Implementation**: All analysis tasks MUST extend `BackendTaskWorkload` and implement required interface methods - framework discovery relies on this pattern
- **JIPipe Integration**: Use `BackendTaskUtils.writeRawImages()` and `runJIPipe()` for standard operations - manual file handling will corrupt data
- **Progress Tracking**: Always use `ProgressInfo` parameter for logging and progress reporting - UI depends on real-time updates
- **Data Types**: Tasks MUST declare input/output types using predefined enums (Raw, Plate, StripDisk, ZOIShape) - custom types break the pipeline

## Frontend Vue Development

- **Task Status Management**: Use polling `/task/{id}/running-log` every 2.5 seconds for progress updates - WebSocket not implemented
- **Konva Canvas**: Initialize via `boot/konva.ts` - direct imports will cause runtime errors  
- **TypeScript**: Any types allowed but discouraged - legacy codebase has permissive ESLint configuration
- **Backend Communication**: Follow existing pattern in stores/backendProcessors/ for task parameter handling

## Hidden Dependencies

- `JASTDataSlot` utility class required for data type conversions between frontend and backend
- `FileStorageService` handles all file operations with automatic cleanup - direct filesystem access breaks the system
- Process execution requires Xvfb wrapper on Linux systems for graphical applications