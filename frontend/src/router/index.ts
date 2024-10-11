import {createRouter, createWebHistory, RouteRecordRaw} from 'vue-router'
import AboutView from "@/views/AboutView.vue";
import DocumentationView from "@/views/DocumentationView.vue";
import AnalyzeView from "@/views/AnalyzeView.vue";

declare module "vue-router" {
    interface RouteMeta {
        title?: string;
    }
}

const routes: Array<RouteRecordRaw> = [
    {
        path: '/',
        name: 'analyze',
        component: AnalyzeView,
        meta: { title: "J-AST" }
    },
    {
        path: '/documentation',
        name: 'documentation',
        component: DocumentationView,
        meta: { title: "Documentation - J-AST" }
    },
    {
        path: '/about',
        name: 'about',
        component: AboutView,
        meta: { title: "About - J-AST" }
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})
router.beforeEach((to, from, next) => {
    document.title = to.meta.title || 'J-AST'
    next();
});

export default router
