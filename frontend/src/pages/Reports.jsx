import React, { useEffect, useState } from 'react';
import api from '../services/api';
import { Download, FileSpreadsheet, FileText } from 'lucide-react';

const Reports = () => {
  const [monthlyReports, setMonthlyReports] = useState([]);
  const [categoryReports, setCategoryReports] = useState([]);
  const [selectedYear, setSelectedYear] = useState(new Date().getFullYear());
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchReports();
  }, [selectedYear]);

  const fetchReports = async () => {
    setLoading(true);
    try {
      const [mRes, cRes] = await Promise.all([
        api.get(`/reports/monthly?year=${selectedYear}`),
        api.get('/reports/categories')
      ]);
      setMonthlyReports(mRes.data);
      setCategoryReports(cRes.data);
    } catch (err) {
      console.error('Failed to load reports', err);
    } finally {
      setLoading(false);
    }
  };

  const handleDownloadCsv = async () => {
    try {
      const response = await api.get('/reports/export/csv', { responseType: 'blob' });
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `financial-report-${selectedYear}.csv`);
      document.body.appendChild(link);
      link.click();
      link.remove();
    } catch (err) {
      alert('Failed to download CSV report');
    }
  };

  const handleDownloadPdf = async () => {
    try {
      const response = await api.get('/reports/export/pdf', { responseType: 'blob' });
      const url = window.URL.createObjectURL(new Blob([response.data], { type: 'application/pdf' }));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `financial-report-${selectedYear}.pdf`);
      document.body.appendChild(link);
      link.click();
      link.remove();
    } catch (err) {
      alert('Failed to download PDF report');
    }
  };

  if (loading) return <div style={{ padding: '2rem' }}>Loading reports...</div>;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <h1 style={{ fontSize: '1.5rem', fontWeight: 'bold', color: '#0f172a' }}>Financial Reports & Analytics</h1>
          <p style={{ color: '#64748b', fontSize: '0.875rem' }}>Download official financial statements and track multi-month trends</p>
        </div>
        <div style={{ display: 'flex', gap: '0.75rem' }}>
          <button onClick={handleDownloadCsv} className="btn-secondary" style={{ display: 'flex', alignItems: 'center', gap: '0.375rem' }}>
            <FileSpreadsheet size={18} /> Export CSV
          </button>
          <button onClick={handleDownloadPdf} className="btn-primary" style={{ display: 'flex', alignItems: 'center', gap: '0.375rem' }}>
            <FileText size={18} /> Export PDF
          </button>
        </div>
      </div>

      {/* Year Filter */}
      <div className="card" style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
        <label style={{ fontWeight: '500', fontSize: '0.875rem' }}>Select Reporting Year:</label>
        <select value={selectedYear} onChange={(e) => setSelectedYear(parseInt(e.target.value))} style={{ width: '120px' }}>
          <option value="2024">2024</option>
          <option value="2025">2025</option>
          <option value="2026">2026</option>
          <option value="2027">2027</option>
        </select>
      </div>

      {/* Monthly Summary Table */}
      <div className="card">
        <h3 style={{ fontSize: '1.125rem', fontWeight: 'bold', marginBottom: '1rem' }}>Monthly Financial Overview ({selectedYear})</h3>
        <table>
          <thead>
            <tr>
              <th>Month</th>
              <th>Total Income (₹)</th>
              <th>Total Expenses (₹)</th>
              <th>Net Savings (₹)</th>
            </tr>
          </thead>
          <tbody>
            {monthlyReports.map(m => (
              <tr key={m.month}>
                <td style={{ fontWeight: '500' }}>{m.monthName}</td>
                <td style={{ color: '#16a34a', fontWeight: '600' }}>+ ₹{m.totalIncome}</td>
                <td style={{ color: '#dc2626', fontWeight: '600' }}>- ₹{m.totalExpenses}</td>
                <td style={{ fontWeight: '700', color: m.netSavings >= 0 ? '#16a34a' : '#dc2626' }}>
                  ₹{m.netSavings}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Category Breakdown Table */}
      <div className="card">
        <h3 style={{ fontSize: '1.125rem', fontWeight: 'bold', marginBottom: '1rem' }}>Category Distribution Summary</h3>
        {categoryReports.length > 0 ? (
          <table>
            <thead>
              <tr>
                <th>Category Name</th>
                <th>Type</th>
                <th>Total Amount</th>
                <th>Percentage of Total</th>
              </tr>
            </thead>
            <tbody>
              {categoryReports.map((c, i) => (
                <tr key={i}>
                  <td style={{ fontWeight: '500' }}>{c.categoryName}</td>
                  <td>{c.categoryType}</td>
                  <td style={{ fontWeight: '600' }}>₹{c.totalAmount}</td>
                  <td>{c.percentage.toFixed(1)}%</td>
                </tr>
              ))}
            </tbody>
          </table>
        ) : (
          <p style={{ color: '#64748b' }}>No category spending data available for this range.</p>
        )}
      </div>
    </div>
  );
};

export default Reports;
