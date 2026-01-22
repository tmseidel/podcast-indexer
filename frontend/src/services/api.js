import axios from 'axios';

const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

export const podcastApi = {
  getAllPodcasts: () => api.get('/podcasts'),
  getPodcast: (id) => api.get(`/podcasts/${id}`),
  addPodcast: (feedUrl, downloadUntilDate) => api.post('/podcasts', {
    feedUrl,
    downloadUntilDate: downloadUntilDate || null,
  }),
  syncPodcast: (id) => api.post(`/podcasts/${id}/sync`),
};

export const qaApi = {
  askQuestion: (podcastId, question) => 
    api.post('/qa/ask', { podcastId, question }),
};

export default api;
