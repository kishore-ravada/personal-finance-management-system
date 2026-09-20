import React, { useEffect, useState } from 'react';
import api from '../services/api';
import Modal from '../components/Modal';
import { Plus, Trash2, Edit2, Search, Filter } from 'lucide-react';

const Transactions = () => {
  const [transactions, setTransactions] = useState([]);
  const [accounts, setAccounts] = useState([]);
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);

  // Pagination & Filtering
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [typeFilter, setTypeFilter] = useState('');
  const [categoryFilter, setCategoryFilter] = useState('');
  const [accountFilter, setAccountFilter] = useState('');
  const [search, setSearch] = useState('');

  // Form State
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [formType, setFormType] = useState('EXPENSE');
  const [accountId, setAccountId] = useState('');
  const [categoryId, setCategoryId] = useState('');
  const [amount, setAmount] = useState('');
  const [description, setDescription] = useState('');
  const [transactionDate, setTransactionDate] = useState(new Date().toISOString().split('T')[0]);

  useEffect(() => {
    fetchAccountsAndCategories();
  }, []);

  useEffect(() => {
    fetchTransactions();
  }, [page, typeFilter, categoryFilter, accountFilter, search]);

  const fetchAccountsAndCategories = async () => {
    try {
      const [accRes, catRes] = await Promise.all([
        api.get('/accounts'),
        api.get('/categories')
      ]);
      setAccounts(accRes.data);
      setCategories(catRes.data);
      if (accRes.data.length > 0) setAccountId(accRes.data[0].id);
    } catch (err) {
      console.error('Error fetching dropdowns', err);
    }
  };

  const fetchTransactions = async () => {
    setLoading(true);
    try {
      const params = {
        page,
        size: 10,
        sortBy: 'transactionDate',
        sortDir: 'desc'
      };
      if (typeFilter) params.type = typeFilter;
      if (categoryFilter) params.categoryId = categoryFilter;
      if (accountFilter) params.accountId = accountFilter;
      if (search) params.search = search;

      const res = await api.get('/transactions', { params });
      setTransactions(res.data.content);
      setTotalPages(res.data.totalPages);
    } catch (err) {
      console.error('Failed to fetch transactions', err);
    } finally {
      setLoading(false);
    }
  };

  const handleOpenModal = (t = null) => {
    if (t) {
      setEditingId(t.id);
      setFormType(t.type);
      setAccountId(t.accountId);
      setCategoryId(t.categoryId);
      setAmount(t.amount);
      setDescription(t.description || '');
      setTransactionDate(t.transactionDate);
    } else {
      setEditingId(null);
      setFormType('EXPENSE');
      if (accounts.length > 0) setAccountId(accounts[0].id);
      const firstExpCat = categories.find(c => c.type === 'EXPENSE');
      if (firstExpCat) setCategoryId(firstExpCat.id);
      setAmount('');
      setDescription('');
      setTransactionDate(new Date().toISOString().split('T')[0]);
    }
    setIsModalOpen(true);
  };

  const handleFormTypeChange = (newType) => {
    setFormType(newType);
    const matchingCat = categories.find(c => c.type === newType);
    if (matchingCat) setCategoryId(matchingCat.id);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      const payload = {
        accountId: parseInt(accountId),
        categoryId: parseInt(categoryId),
        amount: parseFloat(amount),
        type: formType,
        description,
        transactionDate
      };

      if (editingId) {
        await api.put(`/transactions/${editingId}`, payload);
      } else {
        await api.post('/transactions', payload);
      }

      setIsModalOpen(false);
      fetchTransactions();
    } catch (err) {
      alert(err.response?.data?.message || 'Error saving transaction');
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Are you sure you want to delete this transaction?')) return;
    try {
      await api.delete(`/transactions/${id}`);
      fetchTransactions();
    } catch (err) {
      alert('Failed to delete transaction');
    }
  };

  const filteredCategoriesForForm = categories.filter(c => c.type === formType);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <h1 style={{ fontSize: '1.5rem', fontWeight: 'bold', color: '#0f172a' }}>Transaction Ledger</h1>
        <button onClick={() => handleOpenModal()} className="btn-primary" style={{ display: 'flex', alignItems: 'center', gap: '0.375rem' }}>
          <Plus size={18} /> Record Transaction
        </button>
      </div>

      {/* Filter Bar */}
      <div className="card" style={{ display: 'flex', gap: '1rem', flexWrap: 'wrap', alignItems: 'center' }}>
        <div style={{ flex: '1 1 200px', display: 'flex', alignItems: 'center', position: 'relative' }}>
          <input
            type="text"
            placeholder="Search description..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>

        <select value={typeFilter} onChange={(e) => setTypeFilter(e.target.value)} style={{ flex: '1 1 140px' }}>
          <option value="">All Types</option>
          <option value="INCOME">INCOME</option>
          <option value="EXPENSE">EXPENSE</option>
        </select>

        <select value={accountFilter} onChange={(e) => setAccountFilter(e.target.value)} style={{ flex: '1 1 160px' }}>
          <option value="">All Accounts</option>
          {accounts.map(a => <option key={a.id} value={a.id}>{a.name}</option>)}
        </select>

        <select value={categoryFilter} onChange={(e) => setCategoryFilter(e.target.value)} style={{ flex: '1 1 160px' }}>
          <option value="">All Categories</option>
          {categories.map(c => <option key={c.id} value={c.id}>{c.name} ({c.type})</option>)}
        </select>
      </div>

      {/* Transactions Table */}
      <div className="card">
        {loading ? (
          <p>Loading transactions...</p>
        ) : transactions.length > 0 ? (
          <>
            <table>
              <thead>
                <tr>
                  <th>Date</th>
                  <th>Type</th>
                  <th>Account</th>
                  <th>Category</th>
                  <th>Amount</th>
                  <th>Description</th>
                  <th style={{ textAlign: 'right' }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {transactions.map(t => (
                  <tr key={t.id}>
                    <td>{t.transactionDate}</td>
                    <td>
                      <span style={{
                        padding: '0.125rem 0.5rem',
                        borderRadius: '0.25rem',
                        fontSize: '0.75rem',
                        fontWeight: '600',
                        backgroundColor: t.type === 'INCOME' ? '#dcfce7' : '#fee2e2',
                        color: t.type === 'INCOME' ? '#15803d' : '#b91c1c'
                      }}>
                        {t.type}
                      </span>
                    </td>
                    <td>{t.accountName}</td>
                    <td>{t.categoryName}</td>
                    <td style={{ fontWeight: '600', color: t.type === 'INCOME' ? '#16a34a' : '#dc2626' }}>
                      {t.type === 'INCOME' ? '+' : '-'} ₹{t.amount}
                    </td>
                    <td>{t.description || '-'}</td>
                    <td style={{ textAlign: 'right' }}>
                      <button onClick={() => handleOpenModal(t)} style={{ background: 'none', border: 'none', color: '#64748b', marginRight: '0.5rem' }}>
                        <Edit2 size={16} />
                      </button>
                      <button onClick={() => handleDelete(t.id)} style={{ background: 'none', border: 'none', color: '#dc2626' }}>
                        <Trash2 size={16} />
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>

            {/* Pagination Controls */}
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '1.25rem' }}>
              <span style={{ fontSize: '0.875rem', color: '#64748b' }}>Page {page + 1} of {totalPages || 1}</span>
              <div style={{ display: 'flex', gap: '0.5rem' }}>
                <button disabled={page === 0} onClick={() => setPage(page - 1)} className="btn-secondary">Previous</button>
                <button disabled={page >= totalPages - 1} onClick={() => setPage(page + 1)} className="btn-secondary">Next</button>
              </div>
            </div>
          </>
        ) : (
          <p style={{ color: '#64748b' }}>No transactions found matching your criteria.</p>
        )}
      </div>

      {/* Transaction Modal */}
      <Modal isOpen={isModalOpen} onClose={() => setIsModalOpen(false)} title={editingId ? 'Edit Transaction' : 'Record Transaction'}>
        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
          <div>
            <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: '500', marginBottom: '0.25rem' }}>Transaction Type</label>
            <select value={formType} onChange={(e) => handleFormTypeChange(e.target.value)}>
              <option value="EXPENSE">EXPENSE</option>
              <option value="INCOME">INCOME</option>
            </select>
          </div>

          <div>
            <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: '500', marginBottom: '0.25rem' }}>Account</label>
            <select value={accountId} onChange={(e) => setAccountId(e.target.value)} required>
              {accounts.map(a => <option key={a.id} value={a.id}>{a.name} (₹{a.balance})</option>)}
            </select>
          </div>

          <div>
            <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: '500', marginBottom: '0.25rem' }}>Category</label>
            <select value={categoryId} onChange={(e) => setCategoryId(e.target.value)} required>
              {filteredCategoriesForForm.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
            </select>
          </div>

          <div>
            <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: '500', marginBottom: '0.25rem' }}>Amount (₹)</label>
            <input type="number" step="0.01" value={amount} onChange={(e) => setAmount(e.target.value)} required min="0.01" />
          </div>

          <div>
            <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: '500', marginBottom: '0.25rem' }}>Date</label>
            <input type="date" value={transactionDate} onChange={(e) => setTransactionDate(e.target.value)} required />
          </div>

          <div>
            <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: '500', marginBottom: '0.25rem' }}>Description</label>
            <input type="text" value={description} onChange={(e) => setDescription(e.target.value)} placeholder="e.g. Grocery shopping, Monthly salary" />
          </div>

          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.5rem', marginTop: '1rem' }}>
            <button type="button" onClick={() => setIsModalOpen(false)} className="btn-secondary">Cancel</button>
            <button type="submit" className="btn-primary">Save Transaction</button>
          </div>
        </form>
      </Modal>
    </div>
  );
};

export default Transactions;
