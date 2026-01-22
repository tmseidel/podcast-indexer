import React from 'react';
import { BrowserRouter as Router, Routes, Route, Link } from 'react-router-dom';
import PodcastList from './components/PodcastList';
import PodcastDetail from './components/PodcastDetail';
import AskQuestion from './components/AskQuestion';
import './App.css';

function App() {
  return (
    <Router>
      <div className="App">
        <nav className="navbar">
          <div className="nav-container">
            <h1 className="nav-title">Podcast Indexer</h1>
            <div className="nav-links">
              <Link to="/">Podcasts</Link>
              <Link to="/ask">Ask Question</Link>
            </div>
          </div>
        </nav>
        
        <div className="main-content">
          <Routes>
            <Route path="/" element={<PodcastList />} />
            <Route path="/podcast/:id" element={<PodcastDetail />} />
            <Route path="/ask" element={<AskQuestion />} />
          </Routes>
        </div>
      </div>
    </Router>
  );
}

export default App;
