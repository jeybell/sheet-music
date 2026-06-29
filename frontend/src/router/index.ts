import { createRouter, createWebHistory } from "vue-router";
import SongCreateView from "../views/SongCreateView.vue";
import SongDetailView from "../views/SongDetailView.vue";
import SongListView from "../views/SongListView.vue";
import BulkUploadView from "../views/BulkUploadView.vue";
import SetlistListView from "../views/SetlistListView.vue";
import SetlistDetailView from "../views/SetlistDetailView.vue";
import FeatureRequestView from "../views/FeatureRequestView.vue";
import ShareView from "../views/ShareView.vue";

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
      path: "/songs/bulk",
      name: "song-bulk-upload",
      component: BulkUploadView,
    },
    {
      path: "/songs/:songId",
      name: "song-detail",
      component: SongDetailView,
      props: (route) => ({
        songId: Number(route.params.songId),
      }),
    },
    {
      path: "/setlists",
      name: "setlists",
      component: SetlistListView,
    },
    {
      path: "/setlists/:setlistId",
      name: "setlist-detail",
      component: SetlistDetailView,
      props: (route) => ({ setlistId: Number(route.params.setlistId) }),
    },
    {
      path: "/feature-requests",
      name: "feature-requests",
      component: FeatureRequestView,
    },
    {
      path: "/share/:token",
      name: "share",
      component: ShareView,
    },
  ],
});

export default router;
