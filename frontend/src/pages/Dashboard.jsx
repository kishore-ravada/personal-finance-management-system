import React, { useEffect, useState } from 'react';
import api from '../services/api';
import MetricCard from '../components/MetricCard';
import { Wallet, TrendingUp, TrendingDown, PiggyBank } from 'lucide-react';
import { Chart as ChartJS, ArcElement, Tooltip, Legend, CategoryScale, LinearScale, BarElement, Title } from 'chart.js';
import { Doughnut, Bar } from 'react-chartjs-2';

ChartJS.register(ArcElement, Tooltip, Legend, CategoryScale, LinearScale, BarElement, Title);

const Dashboard = () => {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    fetchDashboard();
  }, []);

  const fetchDashboard = async () => {
    try {
      const res = await api.get('/dashboard');
      setData(res.data);
    } catch (err) {
      setError('Failed to load dashboard financial data');
    } finally {
      setLoading(false);
    }
  };

  if (loading) return <div style={{ padding: '2rem' }}>Loading dashboard data...</div>;
  if (error) return <div style={{ padding: '2rem', color: '#dc2626' }}>{error}</div>;

  // Chart Data Preparation
  const categoryNames = data?.expenseByCategory?.map(item => item.categoryName) || [];
  const categoryAmounts = data?.expenseByCategory?.map(item => item.amount) || [];

  const doughnutData = {
    labels: categoryNames,
    datasets: [
      {
        label: 'Expenses (₹)',
        data: categoryAmounts,
        backgroundColor: [
          '#ef4444', '#3b82f6', '#10b981', '#f59e0b', '#8b5cf6',
          '#ec4899', '#14b8a6', '#6366f1', '#84cc16', '#64748b'
        ],
        borderWidth: 1,
      },
    ],
  };

  const barData = {
    labels: ['Current Month'],
    datasets: [
      {
        label: 'Income (₹)',
        data: [data?.monthlyIncome || 0],
        backgroundColor: '#16a34a',
      },
      {
        label: 'Expenses (₹)',
        data: [data?.monthlyExpenses || 0],
        backgroundColor: '#dc2626',
      }
    ]
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      <h1 style={{ fontSize: '1.5rem', fontWeight: 'bold', color: '#0f172a' }}>Financial Dashboard</h1>

      {/* Metrics Row */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '1rem' }}>
        <MetricCard title="Total Balance" amount={data?.totalBalance} icon={Wallet} color="#2563eb" />
        <MetricCard title="Monthly Income" amount={data?.monthlyIncome} icon={TrendingUp} color="#16a34a" />
        <MetricCard title="Monthly Expenses" amount={data?.monthlyExpenses} icon={TrendingDown} color="#dc2626" />
        <MetricCard title="Monthly Savings" amount={data?.monthlySavings} icon={PiggyBank} color="#0284c7" />
      </div>

      {/* Charts Row */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: '1.5rem' }}>
        <div className="card">
          <h3 style={{ fontSize: '1rem', fontWeight: '600', marginBottom: '1rem' }}>Expense by Category</h3>
          {categoryAmounts.length > 0 ? (
            <div style={{ height: '240px', display: 'flex', justifyContent: 'center' }}>
              <Doughnut data={doughnutData} options={{ maintainAspectRatio: false }} />
            </div>
          ) : (
            <p style={{ color: '#64748b', fontSize: '0.875rem' }}>No expense transactions logged this month.</p>
          )}
        </div>

        <div className="card">
          <h3 style={{ fontSize: '1rem', fontWeight: '600', marginBottom: '1rem' }}>Income vs Expense</h3>
          <div style={{ height: '240px' }}>
            <Bar data={barData} options={{ maintainAspectRatio: false, responsive: true }} />
          </div>
        </div>
      </div>

      {/* Budget & Savings Status */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: '1.5rem' }}>
        <div className="card">
          <h3 style={{ fontSize: '1rem', fontWeight: '600', marginBottom: '1rem' }}>Active Budget Usage</h3>
          {data?.budgetStatus && data.budgetStatus.length > 0 ? (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.875rem' }}>
              {data.budgetStatus.slice(0, 4).map(b => (
                <div key={b.id}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.875rem', marginBottom: '0.25rem' }}>
                    <span style={{ fontWeight: '500' }}>{b.categoryName}</span>
                    <span>₹{b.spentAmount} / ₹{b.amount} ({b.percentageUsed.toFixed(1)}%)</span>
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
                </div>
              ))}
            </div>
          ) : (
            <p style={{ color: '#64748b', fontSize: '0.875rem' }}>No active budgets configured for this month.</p>
          )}
        </div>

        <div className="card">
          <h3 style={{ fontSize: '1rem', fontWeight: '600', marginBottom: '1rem' }}>Savings Goals Progress</h3>
          {data?.savingsGoalsProgress && data.savingsGoalsProgress.length > 0 ? (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.875rem' }}>
              {data.savingsGoalsProgress.slice(0, 4).map(s => (
                <div key={s.id}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.875rem', marginBottom: '0.25rem' }}>
                    <span style={{ fontWeight: '500' }}>{s.name}</span>
                    <span>₹{s.currentAmount} / ₹{s.targetAmount} ({s.progressPercentage.toFixed(1)}%)</span>
                  </div>
                  <div className="progress-bg">
                    <div
                      className="progress-fill"
                      style={{
                        width: `${Math.min(s.progressPercentage, 100)}%`,
                        backgroundColor: '#2563eb'
                      }}
                    />
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <p style={{ color: '#64748b', fontSize: '0.875rem' }}>No savings goals created yet.</p>
          )}
        </div>
      </div>

      {/* Recent Transactions Table */}
      <div className="card">
        <h3 style={{ fontSize: '1rem', fontWeight: '600', marginBottom: '1rem' }}>Recent Transactions</h3>
        {data?.recentTransactions && data.recentTransactions.length > 0 ? (
          <table>
            <thead>
              <tr>
                <th>Date</th>
                <th>Type</th>
                <th>Account</th>
                <th>Category</th>
                <th>Amount</th>
                <th>Description</th>
              </tr>
            </thead>
            <tbody>
              {data.recentTransactions.map(t => (
                <tr key={t.id}>
                  <td>{t.transactionDate}</td>
                  <td>
                    <span style={{
                      padding: '0.125rem 0.375rem',
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
                </tr>
              ))}
            </tbody>
          </table>
        ) : (
          <p style={{ color: '#64748b', fontSize: '0.875rem' }}>No transactions recorded yet.</p>
        )}
      </div>
    </div>
  );
};

export default Dashboard;
