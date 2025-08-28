# Project Debug Rules (Non-Obvious Only)

## Backend Debugging

- **Fiji/ImageJ**: External process debugging requires attaching to Xvfb display server - no direct GUI access
- **JobRunr Dashboard**: Available at `/jobrunr` but only works when `spring.jobrunr.background-job-server.enabled=true`
- **Admin Credentials**: Hardcoded in config (admin@localhost / Ckw55ryzy1mXGxb8) - will fail if changed without updating SecurityConfig
- **File Storage**: All uploads go to configured storage path with cleanup service running every hour

## Frontend Debugging  

- **Electron Dev Tools**: Access via Command Palette > "Developer: Open Webview Developer Tools" (not F12)
- **Progress Updates**: Polling `/task/{id}/running-log` shows real-time logs - check network tab for debugging
- **Canvas Rendering**: Konva issues may stem from image loading in boot sequence - verify konva.ts initialization
- **CORS Issues**: Backend must allow frontend origin in WebSecurityConfig - development requires specific setup

## Common Gotchas

- **10MB File Limit**: Uploads exceeding this size fail silently without error messages
- **Memory Allocation**: JIPipe processes allocated 8GB RAM each - system may OOM with concurrent tasks  
- **Worker Threads**: Only 2 JobRunr workers configured - long-running tasks block others
- **JWT Tokens**: Expire after set duration (configurable) - refresh tokens required for long sessions