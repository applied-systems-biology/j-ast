<script lang="ts">
import {Options, Vue} from 'vue-class-component';
import axios from "axios";

@Options({})
export default class TestHello extends Vue {
  message = "Please wait ..."

  mounted() {
    axios.get("/api/test")
        .then(response => this.message = response.data)
        .catch(() => this.message = "Error")
  }

}
</script>

<template>
  <div>{{ message }}</div>
</template>

<style scoped>

</style>