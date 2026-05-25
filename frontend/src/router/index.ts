import { createRouter, createWebHistory } from "vue-router";
import SongCreateView from "../views/SongCreateView.vue";
import SongDetailView from "../views/SongDetailView.vue";
import SongListView from "../views/SongListView.vue";

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: "/",
      redirect: "/songs",
    },
    {
      path: "/songs",
      name: "songs",
      component: SongListView,
    },
    {
      path: "/songs/new",
      name: "song-create",
      component: SongCreateView,
    },
    {
      path: "/songs/:songId",
      name: "song-detail",
      component: SongDetailView,
      props: (route) => ({
        songId: Number(route.params.songId),
      }),
    },
  ],
});

export default router;
