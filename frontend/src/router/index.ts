import { createRouter, createWebHistory } from 'vue-router';
import GuidePage from '../pages/GuidePage.vue';
import TestPage from '../pages/TestPage.vue';
import ResultPage from '../pages/ResultPage.vue';
import MatchPage from '../pages/MatchPage.vue';
import AdminDashboard from '../pages/AdminDashboard.vue';
import AdminShortLinkDetail from '../pages/AdminShortLinkDetail.vue';
import NotFoundPage from '../pages/NotFoundPage.vue';

const router = createRouter({
  history: createWebHistory(),
  scrollBehavior(to, _from, savedPosition) {
    if (savedPosition) {
      return savedPosition;
    }
    if (to.hash) {
      return { el: to.hash, top: 0 };
    }
    return { left: 0, top: 0 };
  },
  routes: [
    { path: '/', name: 'guide', component: GuidePage },
    { path: '/test', name: 'test', component: TestPage },
    { path: '/result/:resultId', name: 'result', component: ResultPage },
    { path: '/match/:partnerShortCode/:currentShortCode', name: 'match', component: MatchPage },
    { path: '/admin', name: 'admin', component: AdminDashboard },
    {
      path: '/admin/short-links/:shortCode',
      name: 'admin-short-link-detail',
      component: AdminShortLinkDetail,
    },
    { path: '/:pathMatch(.*)*', name: 'not-found', component: NotFoundPage },
  ],
});

export default router;
