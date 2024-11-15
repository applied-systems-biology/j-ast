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
    path: "/project/:id",
    component: () => import('layouts/ProjectLayout.vue'),
    children: [],
  },
  {
    path: "/annotation/:annotationTypeId/:imageId",
    component: () => import('layouts/ImageAnnotationLayout.vue'),
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
