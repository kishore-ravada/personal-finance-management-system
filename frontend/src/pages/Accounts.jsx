import React, { useEffect, useState } from 'react';
import api from '../services/api';
import Modal from '../components/Modal';
import { Plus, Trash2, Edit2, CreditCard } from 'lucide-react';

const Accounts = () => {
  const [accounts, setAccounts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingId, setEditingId] = useState(null);

  const [name, setName] = useState('');
  const [type, setType] = useState('BANK');
  const [balance, setBalance] = useState('');
  const [currency, setCurrency] = useState('INR');

  useEffect(() => {
    fetchAccounts();
  }, []);

  const fetchAccounts = async () => {
    try {
      const res = await api.get('/accounts');
      setAccounts(res.data);
    } catch (err) {
      setError('Failed to load accounts');
    } finally {
      setLoading(false);
    }
  };

  const handleOpenModal = (acc = null) => {
    if (acc) {
      setEditingId(acc.id);
      setName(acc.name);
      setType(acc.type);
      setBalance(acc.balance);
      setCurrency(acc.currency || 'INR');
    } else {
      setEditingId(null);
      setName('');
      setType('BANK');
      setBalance('0');
      setCurrency('INR');
    }
    setIsModalOpen(true);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      const payload = { name, type, balance: parseFloat(balance || 0), currency };
      if (editingId) {
        await api.put(`/accounts/${editingId}`, payload);
      } else {
        await api.post('/accounts', payload);
      }
      setIsModalOpen(false);
      fetchAccounts();
    } catch (err) {
      alert(err.response?.data?.message || 'Error saving account');
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Are you sure you want to delete this account?')) return;
    try {
      await api.delete(`/accounts/${id}`);
      fetchAccounts();
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to delete account');
    }
  };

  if (loading) return <div style={{ padding: '2rem' }}>Loading accounts...</div>;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <h1 style={{ fontSize: '1.5rem', fontWeight: 'bold', color: '#0f172a' }}>Financial Accounts</h1>
        <button onClick={() => handleOpenModal()} className="btn-primary" style={{ display: 'flex', alignItems: 'center', gap: '0.375rem' }}>
          <Plus size={18} /> Add Account
        </button>
      </div>

      {error && <div style={{ color: '#dc2626' }}>{error}</div>}

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '1.25rem' }}>
        {accounts.map(acc => (
          <div key={acc.id} className="card" style={{ display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
            <div>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.75rem' }}>
                <span style={{ fontSize: '0.75rem', fontWeight: '700', padding: '0.125rem 0.5rem', borderRadius: '0.25rem', backgroundColor: '#e2e8f0', color: '#334155' }}>
                  {acc.type}
                </span>
                <div style={{ display: 'flex', gap: '0.25rem' }}>
                  <button onClick={() => handleOpenModal(acc)} style={{ background: 'none', border: 'none', color: '#64748b', cursor: 'pointer' }}>
                    <Edit2 size={16} />
                  </button>
                  <button onClick={() => handleDelete(acc.id)} style={{ background: 'none', border: 'none', color: '#dc2626', cursor: 'pointer' }}>
                    <Trash2 size={16} />
                  </button>
                </div>
              </div>
              <h3 style={{ fontSize: '1.25rem', fontWeight: 'bold', color: '#0f172a' }}>{acc.name}</h3>
            </div>
            <div style={{ marginTop: '1.5rem', borderTop: '1px solid #f1f5f9', paddingTop: '0.75rem' }}>
              <span style={{ fontSize: '0.75rem', color: '#64748b' }}>Current Balance</span>
              <div style={{ fontSize: '1.5rem', fontWeight: 'bold', color: '#16a34a' }}>
                {acc.currency} {Number(acc.balance).toLocaleString('en-IN', { minimumFractionDigits: 2 })}
              </div>
            </div>
          </div>
        ))}
      </div>

      <Modal isOpen={isModalOpen} onClose={() => setIsModalOpen(false)} title={editingId ? 'Edit Account' : 'Add New Account'}>
        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
          <div>
            <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: '500', marginBottom: '0.25rem' }}>Account Name</label>
            <input type="text" value={name} onChange={(e) => setName(e.target.value)} required placeholder="e.g. HDFC Bank, Cash Wallet" />
          </div>
          <div>
            <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: '500', marginBottom: '0.25rem' }}>Account Type</label>
            <select value={type} onChange={(e) => setType(e.target.value)}>
              <option value="BANK">BANK</option>
              <option value="CASH">CASH</option>
              <option value="WALLET">WALLET</option>
              <option value="CREDIT_CARD">CREDIT_CARD</option>
              <option value="SAVINGS">SAVINGS</option>
            </select>
          </div>
          <div>
            <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: '500', marginBottom: '0.25rem' }}>Initial Balance</label>
            <input type="number" step="0.01" value={balance} onChange={(e) => setBalance(e.target.value)} required />
          </div>
          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.5rem', marginTop: '1rem' }}>
            <button type="button" onClick={() => setIsModalOpen(false)} className="btn-secondary">Cancel</button>
            <button type="submit" className="btn-primary">Save Account</button>
          </div>
        </form>
      </Modal>
    </div>
  );
};

export default Accounts;
