import { RouteRecordRaw } from 'vue-router';

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: () => import('layouts/DefaultLayout.vue'),
    children: [{ path: '', component: () => import('pages/IndexPage.vue') }],
  },
  {
    path: '/account',
    component: () => import('layouts/DefaultLayout.vue'),
    children: [{ path: '', component: () => import('pages/UserAccountPage.vue') }],
  },
  {
    path: '/admin',
    component: () => import('layouts/AdminLayout.vue'),
    children: [],
  },
  {
    path: "/project/:id",
    component: () => import('layouts/ProjectLayout.vue'),
    children: [],
  },
  {
    path: "/tasks/:id",
    component: () => import('layouts/ProjectTasksLayout.vue'),
    children: [],
  },
  {
    path: "/results/list/:id",
    component: () => import('layouts/ResultsIndexLayout.vue'),
    children: [],
  },
  {
    path: "/results/view/:id",
    component: () => import('layouts/ResultsViewLayout.vue'),
    children: [],
  },
  {
    path: "/results/view/:id/:path",
    component: () => import('layouts/ResultsViewLayout.vue'),
    children: [],
  },
  {
    path: "/mask-image-annotation/:imageId/:annotationTypeId",
    component: () => import('layouts/MaskImageAnnotationLayout.vue'),
    children: [],
  },
  // Always leave this as last one,
  // but you can also remove it
  {
    path: '/:catchAll(.*)*',
    component: () => import('pages/ErrorNotFound.vue'),
  },
];

export default routes;
