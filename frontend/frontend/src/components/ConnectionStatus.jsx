import React from 'react';
import './ConnectionStatus.css';

const ConnectionStatus = ({ isConnected, isConnecting, error }) => {
  if (isConnected) {
    return null; // Don't show banner when connected
  }

  return (
    <div className={`connection-status ${error ? 'error' : 'warning'}`}>
      <div className="connection-status-content">
        <span className="connection-status-icon">⚠️</span>
        <span className="connection-status-text">
          {isConnecting ? 'Connecting to server...' : 'Not connected to server'}
        </span>
        {error && (
          <span className="connection-status-error">({error})</span>
        )}
      </div>
    </div>
  );
};

export default ConnectionStatus;