/* eslint-env node */

/*
 * This file runs in a Node context (it's NOT transpiled by Babel), so use only
 * the ES6 features that are supported by your Node version. https://node.green/
 */

// Configuration for your app
// https://v2.quasar.dev/quasar-cli-vite/quasar-config-js


const { configure } = require('quasar/wrappers');


module.exports = configure(function (/* ctx */) {
  return {
    // https://v2.quasar.dev/quasar-cli-vite/prefetch-feature
    // preFetch: true,

    // app boot file (/src/boot)
    // --> boot files are part of "main.js"
    // https://v2.quasar.dev/quasar-cli-vite/boot-files
    boot: [
      'axios',
      "konva"
    ],

    // https://v2.quasar.dev/quasar-cli-vite/quasar-config-js#css
    css: [
      'app.scss'
    ],

    // https://github.com/quasarframework/quasar/tree/dev/extras
    extras: [
      // 'ionicons-v4',
      // 'mdi-v7',
      'fontawesome-v6',
      // 'eva-icons',
      // 'themify',
      // 'line-awesome',
      // 'roboto-font-latin-ext', // this or either 'roboto-font', NEVER both!

      'roboto-font', // optional, you are not bound to it
      'material-icons', // optional, you are not bound to it
    ],

    // Full list of options: https://v2.quasar.dev/quasar-cli-vite/quasar-config-js#build
    build: {
      target: {
        browser: [ 'es2019', 'edge88', 'firefox78', 'chrome87', 'safari13.1' ],
        node: 'node20'
      },
      env: {
        API_LOCATION: process.env.API_LOCATION || "/api",
        FRONTEND_LOCATION: process.env.FRONTEND_LOCATION || "/",
      },
      vueRouterMode: 'history', // available values: 'hash', 'history'
      vueRouterBase: process.env.FRONTEND_LOCATION || "/",
      publicPath: process.env.FRONTEND_LOCATION || "/",

      // vueDevtools,
      // vueOptionsAPI: false,

      // rebuildCache: true, // rebuilds Vite/linter/etc cache on startup

      // analyze: true,
      // env: {},
      // rawDefine: {}
      // ignorePublicFolder: true,
      // minify: false,
      // polyfillModulePreload: true,
      // distDir

      // extendViteConf (viteConf) {},
      // viteVuePluginOptions: {},

      vitePlugins: [
        ['vite-plugin-checker', {
          vueTsc: {
            tsconfigPath: 'tsconfig.vue-tsc.json'
          },
          eslint: {
            lintCommand: 'eslint "./**/*.{js,ts,mjs,cjs,vue}"'
          }
        }, { server: false }]
      ]
    },

    // Full list of options: https://v2.quasar.dev/quasar-cli-vite/quasar-config-js#devServer
    devServer: {
      // https: true
      open: true,
      proxy: {
        "/api": {
          target: "http://localhost:8081/",
          changeOrigin: true,
          // rewrite: (path) => path.replace(/^\/api/, ''),
        }
      }
    },

    // https://v2.quasar.dev/quasar-cli-vite/quasar-config-js#framework
    framework: {
      config: {
        loadingBar: {
        }
      },

      // iconSet: 'material-icons', // Quasar icon set
      // lang: 'en-US', // Quasar language pack

      // For special cases outside of where the auto-import strategy can have an impact
      // (like functional components as one of the examples),
      // you can manually specify Quasar components/directives to be available everywhere:
      //
      // components: [],
      // directives: [],

      // Quasar plugins
      plugins: [
        "Notify",
        "Dialog",
        "Loading",
        // "LoadingBar",
        'LocalStorage',
        'SessionStorage'
      ]
    },

    // animations: 'all', // --- includes all animations
    // https://v2.quasar.dev/options/animations
    animations: [],

    // https://v2.quasar.dev/quasar-cli-vite/quasar-config-js#sourcefiles
    // sourceFiles: {
    //   rootComponent: 'src/App.vue',
    //   router: 'src/router/index',
    //   store: 'src/store/index',
    //   registerServiceWorker: 'src-pwa/register-service-worker',
    //   serviceWorker: 'src-pwa/custom-service-worker',
    //   pwaManifestFile: 'src-pwa/manifest.json',
    //   electronMain: 'src-electron/electron-main',
    //   electronPreload: 'src-electron/electron-preload'
    // },

    // https://v2.quasar.dev/quasar-cli-vite/developing-ssr/configuring-ssr
    ssr: {
      // ssrPwaHtmlFilename: 'offline.html', // do NOT use index.html as name!
                                          // will mess up SSR

      // extendSSRWebserverConf (esbuildConf) {},
      // extendPackageJson (json) {},

      pwa: false,

      // manualStoreHydration: true,
      // manualPostHydrationTrigger: true,

      prodPort: 3000, // The default port that the production server should use
                      // (gets superseded if process.env.PORT is specified at runtime)

      middlewares: [
        'render' // keep this as last one
      ]
    },

    // https://v2.quasar.dev/quasar-cli-vite/developing-pwa/configuring-pwa
    pwa: {
      workboxMode: 'generateSW', // or 'injectManifest'
      injectPwaMetaTags: true,
      swFilename: 'sw.js',
      manifestFilename: 'manifest.json',
      useCredentialsForManifestTag: false,
      // useFilenameHashes: true,
      // extendGenerateSWOptions (cfg) {}
      // extendInjectManifestOptions (cfg) {},
      // extendManifestJson (json) {}
      // extendPWACustomSWConf (esbuildConf) {}
    },

    // Full list of options: https://v2.quasar.dev/quasar-cli-vite/developing-cordova-apps/configuring-cordova
    cordova: {
      // noIosLegacyBuildFlag: true, // uncomment only if you know what you are doing
    },

    // Full list of options: https://v2.quasar.dev/quasar-cli-vite/developing-capacitor-apps/configuring-capacitor
    capacitor: {
      hideSplashscreen: true
    },

    // Full list of options: https://v2.quasar.dev/quasar-cli-vite/developing-electron-apps/configuring-electron
    electron: {
      // extendElectronMainConf (esbuildConf)
      // extendElectronPreloadConf (esbuildConf)

      inspectPort: 5858,

      bundler: 'packager', // 'packager' or 'builder'

      packager: {
        // https://electron.github.io/packager/main/interfaces/Options.html
        platform: ["linux", "win32", "darwin"],
        arch: ["x64", "arm64"],
        // Release version, e.g. 1.0.0.132 (injected by dist/electron/build.sh).
        // Falls back to the Maven base version for local builds. The 4th
        // component is the CI build number: it goes into FileVersion /
        // CFBundleVersion (buildVersion), while ProductVersion /
        // CFBundleShortVersionString stays a 3-part version as required
        // by the Windows and macOS version formats.
        ...(process.env.JAST_RELEASE_VERSION
          ? {
              appVersion: process.env.JAST_RELEASE_VERSION.split('.').slice(0, 3).join('.'),
              buildVersion: process.env.JAST_RELEASE_VERSION,
            }
          : {
              appVersion: '1.0.0',
              buildVersion: '1.0.0',
            }),
        appCopyright: process.env.JAST_COPYRIGHT || 'Copyright (c) 2023 Leibniz-HKI Jena',
        win32metadata: {
          CompanyName: 'Leibniz-HKI Jena',
          FileDescription: 'J-AST - Analysis tool for antimicrobial susceptibility testing',
          ProductName: 'J-AST',
        },
      },

      builder: {
        // https://www.electron.build/configuration/configuration

        appId: 'org.hkijena.jast.desktop',
        productName: "J-AST Desktop",
        extraResources: [
          {
            from: 'backend-electron/bin',
            to: 'backend-electron/bin',
            filter: ['**/*'],
          },
          {
            from: 'backend-electron/share',
            to: 'backend-electron/share',
            filter: ['**/*'],
          },
          {
            from: 'backend-electron/backend.jar',   // or whatever your JAR is named
            to: 'backend-electron/backend.jar',
          },
        ],

        // Output formats per platform
        linux: {
          target: 'dir',
          executableName: 'j-ast-desktop',
        },
        win: {
          target: 'dir',
          executableName: 'j-ast-desktop',
        },
        mac: {
          target: 'dir', // this gives you a .app bundle in a folder
          artifactName: 'j-ast-desktop-${version}.app',
        },
      }
    },

    // Full list of options: https://v2.quasar.dev/quasar-cli-vite/developing-browser-extensions/configuring-bex
    bex: {
      contentScripts: [
        'my-content-script'
      ],

      // extendBexScriptsConf (esbuildConf) {}
      // extendBexManifestJson (json) {}
    }
  }
});
