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
      path: '/songs/:songId',
      name: 'song-detail',
      component: SongDetailView,
      props: (route) => ({
        songId: Number(route.params.songId),
      }),
    },
  ],
})

export default router
