import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { podcastApi } from '../services/api';
import './PodcastList.css';

function PodcastList() {
  const [podcasts, setPodcasts] = useState([]);
  const [feedUrl, setFeedUrl] = useState('');
  const [downloadUntilDate, setDownloadUntilDate] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    loadPodcasts();
  }, []);

  const loadPodcasts = async () => {
    try {
      const response = await podcastApi.getAllPodcasts();
      setPodcasts(response.data);
    } catch (err) {
      setError('Failed to load podcasts');
      console.error(err);
    }
  };

  const handleAddPodcast = async (e) => {
    e.preventDefault();
    if (!feedUrl.trim()) return;

    setLoading(true);
    setError(null);

    try {
      await podcastApi.addPodcast(feedUrl, downloadUntilDate);
      setFeedUrl('');
      setDownloadUntilDate('');
      await loadPodcasts();
    } catch (err) {
      setError('Failed to add podcast. Please check the URL and try again.');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="podcast-list">
      <h2>My Podcasts</h2>
      
      <form onSubmit={handleAddPodcast} className="add-podcast-form">
        <input
          type="text"
          placeholder="Enter RSS feed URL..."
          value={feedUrl}
          onChange={(e) => setFeedUrl(e.target.value)}
          disabled={loading}
        />
        <input
          type="date"
          value={downloadUntilDate}
          onChange={(e) => setDownloadUntilDate(e.target.value)}
          disabled={loading}
          aria-label="Download episodes until"
          title="Download episodes up to this date"
        />
        <button type="submit" disabled={loading || !feedUrl.trim()}>
          {loading ? 'Adding...' : 'Add Podcast'}
        </button>
      </form>

      {error && <div className="error-message">{error}</div>}

      <div className="podcasts-grid">
        {podcasts.length === 0 ? (
          <p className="empty-state">
            No podcasts yet. Add one using an RSS feed URL above.
          </p>
        ) : (
          podcasts.map((podcast) => (
            <Link
              to={`/podcast/${podcast.id}`}
              key={podcast.id}
              className="podcast-card"
            >
              {podcast.imageUrl && (
                <img src={podcast.imageUrl} alt={podcast.title} />
              )}
              <div className="podcast-info">
                <h3>{podcast.title}</h3>
                <p className="author">{podcast.author}</p>
                {podcast.lastSyncedAt && (
                  <p className="sync-date">
                    Last synced: {new Date(podcast.lastSyncedAt).toLocaleDateString()}
                  </p>
                )}
              </div>
            </Link>
          ))
        )}
      </div>
    </div>
  );
}

export default PodcastList;
