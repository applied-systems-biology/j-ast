import { boot } from "quasar/wrappers"
import VueKonva from 'vue-konva';

export default boot(({ app }) => {
  app.use(VueKonva, { prefix: 'Konva' });
});
