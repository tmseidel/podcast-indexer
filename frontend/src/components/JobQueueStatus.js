import React, { useCallback, useEffect, useState } from 'react';
import { jobsApi } from '../services/api';
import './JobQueueStatus.css';

const REFRESH_INTERVAL_MS = 5000;

function JobQueueStatus() {
  const [status, setStatus] = useState(null);
  const [limit, setLimit] = useState(10);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const loadStatus = useCallback(async () => {
    try {
      setLoading(true);
      const response = await jobsApi.getStatus(limit);
      setStatus(response.data);
      setError(null);
    } catch (err) {
      setError('Failed to load job status');
      console.error(err);
    } finally {
      setLoading(false);
    }
  }, [limit]);

  useEffect(() => {
    loadStatus();
  }, [loadStatus]);

  useEffect(() => {
    const timer = setInterval(loadStatus, REFRESH_INTERVAL_MS);
    return () => clearInterval(timer);
  }, [loadStatus]);

  const formatDateTime = (value) => {
    if (!value) return '—';
    return new Date(value).toLocaleString();
  };

  return (
    <div className="job-queue-status">
      <div className="job-queue-header">
        <div>
          <h2>Job Worker Status</h2>
          <p className="job-queue-subtitle">
            Monitor the Redis job queue and active worker threads.
          </p>
        </div>
        <button className="refresh-button" onClick={loadStatus} disabled={loading}>
          {loading ? 'Refreshing...' : 'Refresh'}
        </button>
      </div>

      {error && <div className="error-message">{error}</div>}

      <div className="job-summary-grid">
        <div className="summary-card">
          <span className="summary-label">Parallelism</span>
          <span className="summary-value">{status?.parallelism ?? '—'}</span>
        </div>
        <div className="summary-card">
          <span className="summary-label">Active Jobs</span>
          <span className="summary-value">{status?.activeJobCount ?? '—'}</span>
        </div>
        <div className="summary-card">
          <span className="summary-label">Queue Size</span>
          <span className="summary-value">{status?.queueSize ?? '—'}</span>
        </div>
        <div className="summary-card">
          <span className="summary-label">Last Updated</span>
          <span className="summary-value">{formatDateTime(status?.lastUpdated)}</span>
        </div>
      </div>

      <div className="job-section">
        <div className="job-section-header">
          <h3>Currently Processing</h3>
          <span className="job-count">{status?.activeJobs?.length ?? 0} jobs</span>
        </div>
        {status?.activeJobs?.length ? (
          <div className="job-list">
            {status.activeJobs.map((job) => (
              <div key={job.jobId} className="job-card active">
                <div className="job-card-header">
                  <span className="job-type">{job.type}</span>
                  <span className="job-id">#{job.jobId.slice(0, 8)}</span>
                </div>
                <div className="job-details">
                  <div>
                    <span className="detail-label">Resource</span>
                    <span className="detail-value">{job.resourceId ?? '—'}</span>
                  </div>
                  <div>
                    <span className="detail-label">Part</span>
                    <span className="detail-value">{job.partIndex ?? '—'}</span>
                  </div>
                  <div>
                    <span className="detail-label">Started</span>
                    <span className="detail-value">{formatDateTime(job.startedAt)}</span>
                  </div>
                </div>
                {job.audioFilePath && (
                  <div className="job-audio">
                    <span className="detail-label">Audio</span>
                    <span className="detail-value">{job.audioFilePath}</span>
                  </div>
                )}
              </div>
            ))}
          </div>
        ) : (
          <div className="empty-state">No jobs are currently running.</div>
        )}
        {status?.queueSize > (status?.queuedJobs?.length || 0) && (
          <div className="queue-note">
            Showing first {status?.queuedJobs?.length || 0} of {status?.queueSize} queued jobs.
          </div>
        )}
      </div>

      <div className="job-section">
        <div className="job-section-header">
          <h3>Queued Jobs</h3>
          <div className="job-section-controls">
            <label htmlFor="queue-limit">Preview</label>
            <select
              id="queue-limit"
              value={limit}
              onChange={(event) => setLimit(Number(event.target.value))}
            >
              <option value={5}>5</option>
              <option value={10}>10</option>
              <option value={25}>25</option>
              <option value={50}>50</option>
            </select>
          </div>
        </div>
        {status?.queuedJobs?.length ? (
          <div className="job-list">
            {status.queuedJobs.map((job, index) => (
              <div key={`${job.jobId}-${index}`} className="job-card">
                <div className="job-card-header">
                  <span className="job-type">{job.type}</span>
                  <span className="job-id">#{job.jobId?.slice(0, 8) || 'queued'}</span>
                </div>
                <div className="job-details">
                  <div>
                    <span className="detail-label">Resource</span>
                    <span className="detail-value">{job.resourceId ?? '—'}</span>
                  </div>
                  <div>
                    <span className="detail-label">Part</span>
                    <span className="detail-value">{job.partIndex ?? '—'}</span>
                  </div>
                </div>
                {job.audioFilePath && (
                  <div className="job-audio">
                    <span className="detail-label">Audio</span>
                    <span className="detail-value">{job.audioFilePath}</span>
                  </div>
                )}
              </div>
            ))}
          </div>
        ) : (
          <div className="empty-state">Queue is empty.</div>
        )}
      </div>
    </div>
  );
}

export default JobQueueStatus;
