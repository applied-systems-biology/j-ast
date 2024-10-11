import {createRouter, createWebHistory, RouteRecordRaw} from 'vue-router'
import AboutView from "@/views/AboutView.vue";
import DocumentationView from "@/views/DocumentationView.vue";
import AnalyzeView from "@/views/ProjectView.vue";
import ProjectView from "@/views/ProjectView.vue";

declare module "vue-router" {
    interface RouteMeta {
        title?: string;
    }
}

const routes: Array<RouteRecordRaw> = [
    {
        path: '/',
        name: 'project',
        component: ProjectView,
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
