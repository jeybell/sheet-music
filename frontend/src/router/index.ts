import { createRouter, createWebHistory } from 'vue-router'
import SongDetailView from '../views/SongDetailView.vue'
import SongListView from '../views/SongListView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      redirect: '/songs',
    },
    {
      path: '/songs',
      name: 'songs',
      component: SongListView,
    },
    {
      path: '/songs/:id',
      name: 'song-detail',
      component: SongDetailView,
      props: (route) => ({
        id: Number(route.params.id),
      }),
    },
  ],
})

export default router
