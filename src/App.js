import React, { useState } from 'react';

const symptoms = {
  fever: "🤒 Possible: Flu or Cold. Rest and drink fluids.",
  headache: "🤕 Possible: Tension headache. Try rest and hydration.",
  cough: "😷 Possible: Cold or Bronchitis. Drink warm fluids.",
  tired: "😴 Possible: Fatigue. Get more sleep and rest.",
  nausea: "🤢 Possible: Stomach bug. Avoid heavy foods.",
};

function App() {
  const [page, setPage] = useState('home');
  const [symptom, setSymptom] = useState('');
  const [result, setResult] = useState('');
  const [medicines, setMedicines] = useState([]);
  const [medName, setMedName] = useState('');
  const [medTime, setMedTime] = useState('');

  const checkSymptom = () => {
    const key = symptom.toLowerCase().trim();
    setResult(symptoms[key] || "❓ Symptom not found. Please consult a doctor.");
  };

  const addMedicine = () => {
    if (medName && medTime) {
      setMedicines([...medicines, { name: medName, time: medTime, taken: false }]);
      setMedName('');
      setMedTime('');
    }
  };

  const toggleTaken = (index) => {
    const updated = [...medicines];
    updated[index].taken = !updated[index].taken;
    setMedicines(updated);
  };

  const deleteMed = (index) => {
    setMedicines(medicines.filter((_, i) => i !== index));
  };

  return (
    <div style={{ fontFamily: 'Arial', maxWidth: '500px', margin: '0 auto', padding: '20px' }}>
      
      {/* Header */}
      <h1 style={{ textAlign: 'center', color: '#2c3e50' }}>🏥 Health Monitor</h1>

      {/* Navigation */}
      <div style={{ display: 'flex', gap: '10px', marginBottom: '20px' }}>
        <button onClick={() => setPage('home')}
          style={{ flex: 1, padding: '10px', background: page === 'home' ? '#3498db' : '#ecf0f1', color: page === 'home' ? 'white' : 'black', border: 'none', borderRadius: '8px', cursor: 'pointer' }}>
          🏠 Home
        </button>
        <button onClick={() => setPage('symptoms')}
          style={{ flex: 1, padding: '10px', background: page === 'symptoms' ? '#3498db' : '#ecf0f1', color: page === 'symptoms' ? 'white' : 'black', border: 'none', borderRadius: '8px', cursor: 'pointer' }}>
          🤒 Symptoms
        </button>
        <button onClick={() => setPage('medicines')}
          style={{ flex: 1, padding: '10px', background: page === 'medicines' ? '#3498db' : '#ecf0f1', color: page === 'medicines' ? 'white' : 'black', border: 'none', borderRadius: '8px', cursor: 'pointer' }}>
          💊 Medicines
        </button>
      </div>

      {/* Home Page */}
      {page === 'home' && (
        <div style={{ textAlign: 'center', padding: '40px', background: '#ecf0f1', borderRadius: '12px' }}>
          <h2>Welcome! 👋</h2>
          <p>Track your health and medicines easily.</p>
          <p>Use the menu above to get started.</p>
        </div>
      )}

      {/* Symptoms Page */}
      {page === 'symptoms' && (
        <div style={{ background: '#ecf0f1', padding: '20px', borderRadius: '12px' }}>
          <h2>🤒 Symptom Checker</h2>
          <input
            value={symptom}
            onChange={e => setSymptom(e.target.value)}
            placeholder="Type symptom (e.g. fever)"
            style={{ width: '100%', padding: '10px', marginBottom: '10px', borderRadius: '8px', border: '1px solid #ccc' }}
          />
          <button onClick={checkSymptom}
            style={{ width: '100%', padding: '10px', background: '#27ae60', color: 'white', border: 'none', borderRadius: '8px', cursor: 'pointer' }}>
            Check Symptoms
          </button>
          {result && (
            <div style={{ marginTop: '15px', padding: '15px', background: 'white', borderRadius: '8px' }}>
              {result}
            </div>
          )}
        </div>
      )}

      {/* Medicines Page */}
      {page === 'medicines' && (
        <div style={{ background: '#ecf0f1', padding: '20px', borderRadius: '12px' }}>
          <h2>💊 Medicine Tracker</h2>
          
          <input
            value={medName}
            onChange={e => setMedName(e.target.value)}
            placeholder="Medicine name (e.g. Aspirin)"
            style={{ width: '100%', padding: '10px', marginBottom: '10px', borderRadius: '8px', border: '1px solid #ccc' }}
          />
          <input
            value={medTime}
            onChange={e => setMedTime(e.target.value)}
            placeholder="Time (e.g. 8:00 AM)"
            style={{ width: '100%', padding: '10px', marginBottom: '10px', borderRadius: '8px', border: '1px solid #ccc' }}
          />
          <button onClick={addMedicine}
            style={{ width: '100%', padding: '10px', background: '#8e44ad', color: 'white', border: 'none', borderRadius: '8px', cursor: 'pointer' }}>
            ➕ Add Medicine
          </button>

          <div style={{ marginTop: '15px' }}>
            {medicines.length === 0 && <p style={{ textAlign: 'center' }}>No medicines added yet.</p>}
            {medicines.map((med, index) => (
              <div key={index} style={{ background: 'white', padding: '12px', borderRadius: '8px', marginBottom: '10px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div>
                  <strong style={{ textDecoration: med.taken ? 'line-through' : 'none', color: med.taken ? 'gray' : 'black' }}>
                    💊 {med.name}
                  </strong>
                  <div style={{ fontSize: '12px', color: 'gray' }}>⏰ {med.time}</div>
                </div>
                <div style={{ display: 'flex', gap: '8px' }}>
                  <button onClick={() => toggleTaken(index)}
                    style={{ padding: '6px 10px', background: med.taken ? '#95a5a6' : '#27ae60', color: 'white', border: 'none', borderRadius: '6px', cursor: 'pointer' }}>
                    {med.taken ? '↩️' : '✅'}
                  </button>
                  <button onClick={() => deleteMed(index)}
                    style={{ padding: '6px 10px', background: '#e74c3c', color: 'white', border: 'none', borderRadius: '6px', cursor: 'pointer' }}>
                    🗑️
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

    </div>
  );
}

export default App;