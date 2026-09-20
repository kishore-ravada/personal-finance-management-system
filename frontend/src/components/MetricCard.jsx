import React from 'react';

const MetricCard = ({ title, amount, icon: Icon, color = '#2563eb' }) => {
  const formattedAmount = typeof amount === 'number' || typeof amount === 'string'
    ? `₹ ${Number(amount || 0).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
    : '₹ 0.00';

  return (
    <div className="card" style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
      <div>
        <span style={{ fontSize: '0.875rem', color: '#64748b', fontWeight: '500' }}>{title}</span>
        <h3 style={{ fontSize: '1.5rem', fontWeight: 'bold', marginTop: '0.25rem', color: '#0f172a' }}>
          {formattedAmount}
        </h3>
      </div>
      {Icon && (
        <div style={{
          width: '48px',
          height: '48px',
          borderRadius: '0.5rem',
          backgroundColor: `${color}15`,
          color: color,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center'
        }}>
          <Icon size={24} />
        </div>
      )}
    </div>
  );
};

export default MetricCard;
