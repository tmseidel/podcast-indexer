import React, { useState, useEffect } from 'react';
import { podcastApi, qaApi } from '../services/api';
import './AskQuestion.css';

function AskQuestion() {
  const [podcasts, setPodcasts] = useState([]);
  const [selectedPodcast, setSelectedPodcast] = useState('');
  const [question, setQuestion] = useState('');
  const [answer, setAnswer] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    loadPodcasts();
  }, []);

  const loadPodcasts = async () => {
    try {
      const response = await podcastApi.getAllPodcasts();
      setPodcasts(response.data);
      if (response.data.length > 0) {
        setSelectedPodcast(response.data[0].id.toString());
      }
    } catch (err) {
      setError('Failed to load podcasts');
      console.error(err);
    }
  };

  const handleAsk = async (e) => {
    e.preventDefault();
    if (!question.trim() || !selectedPodcast) return;

    setLoading(true);
    setError(null);
    setAnswer(null);

    try {
      const response = await qaApi.askQuestion(parseInt(selectedPodcast), question);
      setAnswer(response.data);
    } catch (err) {
      setError('Failed to get answer. Please try again.');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const formatTimestamp = (ms) => {
    const seconds = Math.floor(ms / 1000);
    const minutes = Math.floor(seconds / 60);
    const hours = Math.floor(minutes / 60);

    if (hours > 0) {
      return `${hours}:${String(minutes % 60).padStart(2, '0')}:${String(seconds % 60).padStart(2, '0')}`;
    }
    return `${minutes}:${String(seconds % 60).padStart(2, '0')}`;
  };

  return (
    <div className="ask-question">
      <h2>Ask a Question</h2>

      <form onSubmit={handleAsk} className="question-form">
        <div className="form-group">
          <label htmlFor="podcast-select">Select Podcast:</label>
          <select
            id="podcast-select"
            value={selectedPodcast}
            onChange={(e) => setSelectedPodcast(e.target.value)}
            disabled={loading || podcasts.length === 0}
          >
            {podcasts.length === 0 ? (
              <option value="">No podcasts available</option>
            ) : (
              podcasts.map((podcast) => (
                <option key={podcast.id} value={podcast.id}>
                  {podcast.title}
                </option>
              ))
            )}
          </select>
        </div>

        <div className="form-group">
          <label htmlFor="question-input">Your Question:</label>
          <textarea
            id="question-input"
            rows="4"
            placeholder="Ask a question about this podcast..."
            value={question}
            onChange={(e) => setQuestion(e.target.value)}
            disabled={loading || podcasts.length === 0}
          />
        </div>

        <button 
          type="submit" 
          disabled={loading || !question.trim() || !selectedPodcast || podcasts.length === 0}
          className="submit-button"
        >
          {loading ? 'Getting Answer...' : 'Ask Question'}
        </button>
      </form>

      {error && <div className="error-message">{error}</div>}

      {answer && (
        <div className="answer-section">
          <h3>Answer</h3>
          <div className="answer-text">{answer.answer}</div>

          {answer.citations && answer.citations.length > 0 && (
            <div className="citations">
              <h4>Sources</h4>
              {answer.citations.map((citation, index) => (
                <div key={index} className="citation-card">
                  <div className="citation-header">
                    <strong>{citation.episodeTitle}</strong>
                    <span className="timestamp">
                      {formatTimestamp(citation.startMs)} - {formatTimestamp(citation.endMs)}
                    </span>
                  </div>
                  {citation.speakerLabels && (
                    <div className="citation-speakers">Speakers: {citation.speakerLabels}</div>
                  )}
                  <p className="citation-snippet">{citation.textSnippet}</p>
                  <div className="citation-actions">
                    <a 
                      href={citation.audioUrl} 
                      target="_blank" 
                      rel="noopener noreferrer"
                      className="audio-link"
                    >
                      Listen to Original
                    </a>
                    <span className="listen-link">
                      Play at {formatTimestamp(citation.startMs)}
                    </span>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {podcasts.length === 0 && (
        <div className="empty-state">
          <p>No podcasts available. Please add a podcast first.</p>
        </div>
      )}
    </div>
  );
}

export default AskQuestion;
