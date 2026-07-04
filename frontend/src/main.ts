import { createApp } from 'vue';
import App from './App.vue';
import router from './router';
import { initAnalytics } from './utils/tracker';
import './style.css';

initAnalytics(router);
createApp(App).use(router).mount('#app');
