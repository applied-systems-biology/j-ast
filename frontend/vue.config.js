const { defineConfig } = require('@vue/cli-service')
module.exports = defineConfig({
  transpileDependencies: true,
  outputDir: 'target/dist',
  css: {
    loaderOptions: {
      sass: {
        additionalData: `@import "@/styles/main.scss";`
      }
    }
  },
  devServer: {
    proxy: {
      "/api": {
        target: "http://localhost:8081",
        ws: true,
        changeOrigin: true
      }
    }
  }
})
