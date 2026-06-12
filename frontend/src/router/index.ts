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
