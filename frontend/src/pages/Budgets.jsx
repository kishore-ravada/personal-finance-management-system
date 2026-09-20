import React, { useEffect, useState } from 'react';
import api from '../services/api';
import Modal from '../components/Modal';
import { Plus, Trash2, Edit2, AlertTriangle } from 'lucide-react';

const Budgets = () => {
  const [budgets, setBudgets] = useState([]);
  const [expenseCategories, setExpenseCategories] = useState([]);
  const [loading, setLoading] = useState(true);

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [categoryId, setCategoryId] = useState('');
  const [amount, setAmount] = useState('');
  const [month, setMonth] = useState(new Date().getMonth() + 1);
  const [year, setYear] = useState(new Date().getFullYear());

  useEffect(() => {
    fetchBudgets();
    fetchExpenseCategories();
  }, []);

  const fetchBudgets = async () => {
    try {
      const res = await api.get('/budgets');
      setBudgets(res.data);
    } catch (err) {
      console.error('Failed to load budgets', err);
    } finally {
      setLoading(false);
    }
  };

  const fetchExpenseCategories = async () => {
    try {
      const res = await api.get('/categories?type=EXPENSE');
      setExpenseCategories(res.data);
      if (res.data.length > 0) setCategoryId(res.data[0].id);
    } catch (err) {
      console.error('Failed to load expense categories', err);
    }
  };

  const handleOpenModal = (b = null) => {
    if (b) {
      setEditingId(b.id);
      setCategoryId(b.categoryId);
      setAmount(b.amount);
      setMonth(b.month);
      setYear(b.year);
    } else {
      setEditingId(null);
      if (expenseCategories.length > 0) setCategoryId(expenseCategories[0].id);
      setAmount('');
      setMonth(new Date().getMonth() + 1);
      setYear(new Date().getFullYear());
    }
    setIsModalOpen(true);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      const payload = {
        categoryId: parseInt(categoryId),
        amount: parseFloat(amount),
        month: parseInt(month),
        year: parseInt(year)
      };

      if (editingId) {
        await api.put(`/budgets/${editingId}`, payload);
      } else {
        await api.post('/budgets', payload);
      }

      setIsModalOpen(false);
      fetchBudgets();
    } catch (err) {
      alert(err.response?.data?.message || 'Error saving budget');
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Are you sure you want to delete this budget?')) return;
    try {
      await api.delete(`/budgets/${id}`);
      fetchBudgets();
    } catch (err) {
      alert('Failed to delete budget');
    }
  };

  if (loading) return <div style={{ padding: '2rem' }}>Loading budgets...</div>;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <h1 style={{ fontSize: '1.5rem', fontWeight: 'bold', color: '#0f172a' }}>Category Budgets</h1>
        <button onClick={() => handleOpenModal()} className="btn-primary" style={{ display: 'flex', alignItems: 'center', gap: '0.375rem' }}>
          <Plus size={18} /> Set New Budget
        </button>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))', gap: '1.25rem' }}>
        {budgets.map(b => (
          <div key={b.id} className="card" style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
              <div>
                <h3 style={{ fontSize: '1.125rem', fontWeight: 'bold', color: '#0f172a' }}>{b.categoryName}</h3>
                <span style={{ fontSize: '0.75rem', color: '#64748b' }}>Period: {b.month}/{b.year}</span>
              </div>
              <div style={{ display: 'flex', gap: '0.25rem' }}>
                <button onClick={() => handleOpenModal(b)} style={{ background: 'none', border: 'none', color: '#64748b' }}>
                  <Edit2 size={16} />
                </button>
                <button onClick={() => handleDelete(b.id)} style={{ background: 'none', border: 'none', color: '#dc2626' }}>
                  <Trash2 size={16} />
                </button>
              </div>
            </div>

            {b.overBudget && (
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', backgroundColor: '#fee2e2', color: '#b91c1c', padding: '0.5rem', borderRadius: '0.375rem', fontSize: '0.75rem', fontWeight: '600' }}>
                <AlertTriangle size={16} /> Over Budget by ₹{Math.abs(b.remainingAmount)}!
              </div>
            )}

            <div>
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.875rem', marginBottom: '0.375rem' }}>
                <span>Spent: <strong>₹{b.spentAmount}</strong></span>
                <span>Budget: <strong>₹{b.amount}</strong></span>
              </div>
              <div className="progress-bg">
                <div
                  className="progress-fill"
                  style={{
                    width: `${Math.min(b.percentageUsed, 100)}%`,
                    backgroundColor: b.overBudget ? '#dc2626' : b.percentageUsed > 80 ? '#d97706' : '#16a34a'
                  }}
                />
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.75rem', color: '#64748b', marginTop: '0.375rem' }}>
                <span>{b.percentageUsed.toFixed(1)}% used</span>
                <span>Remaining: ₹{b.remainingAmount}</span>
              </div>
            </div>
          </div>
        ))}
      </div>

      <Modal isOpen={isModalOpen} onClose={() => setIsModalOpen(false)} title={editingId ? 'Edit Budget' : 'Set Category Budget'}>
        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
          <div>
            <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: '500', marginBottom: '0.25rem' }}>Expense Category</label>
            <select value={categoryId} onChange={(e) => setCategoryId(e.target.value)} required>
              {expenseCategories.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
            </select>
          </div>

          <div>
            <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: '500', marginBottom: '0.25rem' }}>Budget Limit (₹)</label>
            <input type="number" step="0.01" value={amount} onChange={(e) => setAmount(e.target.value)} required min="0.01" />
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
            <div>
              <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: '500', marginBottom: '0.25rem' }}>Month</label>
              <input type="number" min="1" max="12" value={month} onChange={(e) => setMonth(e.target.value)} required />
            </div>
            <div>
              <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: '500', marginBottom: '0.25rem' }}>Year</label>
              <input type="number" min="2000" max="2100" value={year} onChange={(e) => setYear(e.target.value)} required />
            </div>
          </div>

          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.5rem', marginTop: '1rem' }}>
            <button type="button" onClick={() => setIsModalOpen(false)} className="btn-secondary">Cancel</button>
            <button type="submit" className="btn-primary">Save Budget</button>
          </div>
        </form>
      </Modal>
    </div>
  );
};

export default Budgets;
