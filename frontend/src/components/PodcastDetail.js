import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { podcastApi } from '../services/api';
import './PodcastDetail.css';

function PodcastDetail() {
  const { id } = useParams();
  const [podcast, setPodcast] = useState(null);
  const [loading, setLoading] = useState(true);
  const [syncing, setSyncing] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    loadPodcast();
    // Refresh every 10 seconds to show processing updates
    const interval = setInterval(loadPodcast, 10000);
    return () => clearInterval(interval);
  }, [id]);

  const loadPodcast = async () => {
    try {
      const response = await podcastApi.getPodcast(id);
      setPodcast(response.data);
      setLoading(false);
    } catch (err) {
      setError('Failed to load podcast');
      setLoading(false);
      console.error(err);
    }
  };

  const handleSync = async () => {
    setSyncing(true);
    try {
      await podcastApi.syncPodcast(id);
      await loadPodcast();
    } catch (err) {
      setError('Failed to sync podcast');
      console.error(err);
    } finally {
      setSyncing(false);
    }
  };

  const getStatusBadge = (status) => {
    const statusColors = {
      DISCOVERED: '#95a5a6',
      DOWNLOADING: '#3498db',
      DOWNLOADED: '#3498db',
      TRANSCRIBING: '#f39c12',
      TRANSCRIBED: '#f39c12',
      INDEXING: '#9b59b6',
      INDEXED: '#27ae60',
      FAILED: '#e74c3c'
    };

    return (
      <span 
        className="status-badge" 
        style={{ backgroundColor: statusColors[status] || '#95a5a6' }}
      >
        {status}
      </span>
    );
  };

  if (loading) return <div className="loading">Loading...</div>;
  if (error) return <div className="error-message">{error}</div>;
  if (!podcast) return <div className="error-message">Podcast not found</div>;

  return (
    <div className="podcast-detail">
      <Link to="/" className="back-link">&larr; Back to Podcasts</Link>
      
      <div className="podcast-header">
        {podcast.imageUrl && (
          <img src={podcast.imageUrl} alt={podcast.title} className="podcast-image" />
        )}
        <div className="podcast-meta">
          <h2>{podcast.title}</h2>
          {podcast.author && <p className="author">by {podcast.author}</p>}
          {podcast.description && <p className="description">{podcast.description}</p>}
          <button onClick={handleSync} disabled={syncing} className="sync-button">
            {syncing ? 'Syncing...' : 'Sync Episodes'}
          </button>
        </div>
      </div>

      <div className="episodes-section">
        <h3>Episodes ({podcast.episodes?.length || 0})</h3>
        
        {!podcast.episodes || podcast.episodes.length === 0 ? (
          <p className="empty-state">No episodes yet. Click "Sync Episodes" to discover episodes.</p>
        ) : (
          <div className="episodes-list">
            {podcast.episodes.map((episode) => (
              <div key={episode.id} className="episode-card">
                <div className="episode-header">
                  <h4>{episode.title}</h4>
                  {getStatusBadge(episode.status)}
                </div>
                {episode.description && (
                  <p className="episode-description">{episode.description}</p>
                )}
                <div className="episode-meta">
                  {episode.publishedDate && (
                    <span>Published: {new Date(episode.publishedDate).toLocaleDateString()}</span>
                  )}
                  {episode.durationSeconds && (
                    <span>Duration: {Math.floor(episode.durationSeconds / 60)} min</span>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

export default PodcastDetail;
