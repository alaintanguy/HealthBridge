import React, { useState } from 'react';

function App() {
  const [status, setStatus] = useState('Press button to get GPS');
  const [coords, setCoords] = useState(null);

  const getLocation = () => {
    setStatus('📡 Locating...');

    if (!navigator.geolocation) {
      setStatus('❌ GPS not supported by your browser');
      return;
    }

    navigator.geolocation.getCurrentPosition(
      (position) => {
        const lat = position.coords.latitude;
        const lon = position.coords.longitude;
        setCoords({ lat, lon });
        setStatus('✅ Location found!');
      },
      (error) => {
        setStatus('❌ Error: ' + error.message);
      }
    );
  };

  return (
    <div style={{ fontFamily: 'Arial', maxWidth: '500px', 
                  margin: '0 auto', padding: '20px', textAlign: 'center' }}>

      <h1>📍 GPS Test</h1>

      <button onClick={getLocation}
        style={{ padding: '15px 30px', fontSize: '18px',
                 background: '#3498db', color: 'white',
                 border: 'none', borderRadius: '10px', cursor: 'pointer' }}>
        📡 Get My Location
      </button>

      <p style={{ fontSize: '18px', marginTop: '20px' }}>{status}</p>

      {coords && (
        <div style={{ background: '#ecf0f1', padding: '20px', borderRadius: '12px' }}>
          <p>🌐 Latitude: <strong>{coords.lat}</strong></p>
          <p>🌐 Longitude: <strong>{coords.lon}</strong></p>
        </div>
      )}

    </div>
  );
}

export default App;
