const { defineConfig } = require('@vue/cli-service')
module.exports = defineConfig({
  transpileDependencies: true,
  outputDir: 'target/dist',
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
