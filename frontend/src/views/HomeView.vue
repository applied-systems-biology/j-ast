<template>
  <div class="home">
    <img alt="Vue logo" src="../assets/logo.png">
    <HelloWorld msg="Welcome to Your Vue.js App"/>
    <TestHello/>
  </div>
</template>

<script>
// @ is an alias to /src
import HelloWorld from '@/components/HelloWorld.vue'
import TestHello from "@/components/TestHello.vue";

export default {
  name: 'HomeView',
  components: {
    TestHello,
    HelloWorld
  }
}
</script>
