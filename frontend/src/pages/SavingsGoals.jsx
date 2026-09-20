import React, { useEffect, useState } from 'react';
import api from '../services/api';
import Modal from '../components/Modal';
import { Plus, Trash2, Edit2, Target } from 'lucide-react';

const SavingsGoals = () => {
  const [goals, setGoals] = useState([]);
  const [loading, setLoading] = useState(true);

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [name, setName] = useState('');
  const [targetAmount, setTargetAmount] = useState('');
  const [currentAmount, setCurrentAmount] = useState('0');
  const [targetDate, setTargetDate] = useState('');

  useEffect(() => {
    fetchGoals();
  }, []);

  const fetchGoals = async () => {
    try {
      const res = await api.get('/savings-goals');
      setGoals(res.data);
    } catch (err) {
      console.error('Failed to load savings goals', err);
    } finally {
      setLoading(false);
    }
  };

  const handleOpenModal = (g = null) => {
    if (g) {
      setEditingId(g.id);
      setName(g.name);
      setTargetAmount(g.targetAmount);
      setCurrentAmount(g.currentAmount);
      setTargetDate(g.targetDate || '');
    } else {
      setEditingId(null);
      setName('');
      setTargetAmount('');
      setCurrentAmount('0');
      setTargetDate('');
    }
    setIsModalOpen(true);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      const payload = {
        name,
        targetAmount: parseFloat(targetAmount),
        currentAmount: parseFloat(currentAmount || 0),
        targetDate: targetDate || null
      };

      if (editingId) {
        await api.put(`/savings-goals/${editingId}`, payload);
      } else {
        await api.post('/savings-goals', payload);
      }

      setIsModalOpen(false);
      fetchGoals();
    } catch (err) {
      alert(err.response?.data?.message || 'Error saving savings goal');
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Are you sure you want to delete this goal?')) return;
    try {
      await api.delete(`/savings-goals/${id}`);
      fetchGoals();
    } catch (err) {
      alert('Failed to delete goal');
    }
  };

  if (loading) return <div style={{ padding: '2rem' }}>Loading savings goals...</div>;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <h1 style={{ fontSize: '1.5rem', fontWeight: 'bold', color: '#0f172a' }}>Savings Goals</h1>
        <button onClick={() => handleOpenModal()} className="btn-primary" style={{ display: 'flex', alignItems: 'center', gap: '0.375rem' }}>
          <Plus size={18} /> Create Goal
        </button>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))', gap: '1.25rem' }}>
        {goals.map(g => (
          <div key={g.id} className="card" style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
              <div>
                <h3 style={{ fontSize: '1.125rem', fontWeight: 'bold', color: '#0f172a' }}>{g.name}</h3>
                <span style={{ fontSize: '0.75rem', color: '#64748b' }}>
                  Target Date: {g.targetDate ? g.targetDate : 'No deadline'}
                </span>
              </div>
              <div style={{ display: 'flex', gap: '0.25rem' }}>
                <button onClick={() => handleOpenModal(g)} style={{ background: 'none', border: 'none', color: '#64748b' }}>
                  <Edit2 size={16} />
                </button>
                <button onClick={() => handleDelete(g.id)} style={{ background: 'none', border: 'none', color: '#dc2626' }}>
                  <Trash2 size={16} />
                </button>
              </div>
            </div>

            <div>
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.875rem', marginBottom: '0.375rem' }}>
                <span>Saved: <strong>₹{g.currentAmount}</strong></span>
                <span>Target: <strong>₹{g.targetAmount}</strong></span>
              </div>
              <div className="progress-bg">
                <div
                  className="progress-fill"
                  style={{
                    width: `${Math.min(g.progressPercentage, 100)}%`,
                    backgroundColor: '#2563eb'
                  }}
                />
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.75rem', color: '#64748b', marginTop: '0.375rem' }}>
                <span>{g.progressPercentage.toFixed(1)}% Achieved</span>
                <span>Remaining: ₹{Math.max(0, g.targetAmount - g.currentAmount)}</span>
              </div>
            </div>
          </div>
        ))}
      </div>

      <Modal isOpen={isModalOpen} onClose={() => setIsModalOpen(false)} title={editingId ? 'Edit Goal' : 'Create Savings Goal'}>
        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
          <div>
            <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: '500', marginBottom: '0.25rem' }}>Goal Name</label>
            <input type="text" value={name} onChange={(e) => setName(e.target.value)} required placeholder="e.g. New Laptop, Emergency Fund" />
          </div>

          <div>
            <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: '500', marginBottom: '0.25rem' }}>Target Amount (₹)</label>
            <input type="number" step="0.01" value={targetAmount} onChange={(e) => setTargetAmount(e.target.value)} required min="0.01" />
          </div>

          <div>
            <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: '500', marginBottom: '0.25rem' }}>Current Saved Amount (₹)</label>
            <input type="number" step="0.01" value={currentAmount} onChange={(e) => setCurrentAmount(e.target.value)} min="0" />
          </div>

          <div>
            <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: '500', marginBottom: '0.25rem' }}>Target Deadline Date</label>
            <input type="date" value={targetDate} onChange={(e) => setTargetDate(e.target.value)} />
          </div>

          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.5rem', marginTop: '1rem' }}>
            <button type="button" onClick={() => setIsModalOpen(false)} className="btn-secondary">Cancel</button>
            <button type="submit" className="btn-primary">Save Goal</button>
          </div>
        </form>
      </Modal>
    </div>
  );
};

export default SavingsGoals;
