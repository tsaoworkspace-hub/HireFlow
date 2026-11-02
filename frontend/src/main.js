import { createPinia } from 'pinia'
import { createApp } from 'vue'
import 'bootstrap/dist/css/bootstrap.min.css'
import './assets/main.css'

import App from './App.vue'
import router from './router'

createApp(App)
  .use(createPinia())
  .use(router)
  .mount('#app')

