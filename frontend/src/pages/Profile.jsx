import React, { useState, useContext } from 'react';
import api from '../services/api';
import { AuthContext } from '../context/AuthContext';
import { User, Key } from 'lucide-react';

const Profile = () => {
  const { user, setUser } = useContext(AuthContext);

  const [name, setName] = useState(user?.name || '');
  const [email, setEmail] = useState(user?.email || '');
  const [profileMsg, setProfileMsg] = useState('');
  const [profileErr, setProfileErr] = useState('');

  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [pwdMsg, setPwdMsg] = useState('');
  const [pwdErr, setPwdErr] = useState('');

  const handleUpdateProfile = async (e) => {
    e.preventDefault();
    setProfileMsg('');
    setProfileErr('');

    try {
      const res = await api.put('/users/me', { name, email });
      setUser(res.data);
      localStorage.setItem('user', JSON.stringify(res.data));
      setProfileMsg('Profile updated successfully!');
    } catch (err) {
      setProfileErr(err.response?.data?.message || 'Failed to update profile');
    }
  };

  const handleChangePassword = async (e) => {
    e.preventDefault();
    setPwdMsg('');
    setPwdErr('');

    try {
      await api.put('/users/me/password', { currentPassword, newPassword });
      setPwdMsg('Password updated successfully!');
      setCurrentPassword('');
      setNewPassword('');
    } catch (err) {
      setPwdErr(err.response?.data?.message || 'Failed to change password');
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem', maxWidth: '650px' }}>
      <h1 style={{ fontSize: '1.5rem', fontWeight: 'bold', color: '#0f172a' }}>User Settings & Profile</h1>

      {/* Profile Details */}
      <div className="card">
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '1.25rem' }}>
          <User size={20} color="#2563eb" />
          <h3 style={{ fontSize: '1.125rem', fontWeight: 'bold' }}>Personal Information</h3>
        </div>

        {profileMsg && <div style={{ padding: '0.5rem', backgroundColor: '#dcfce7', color: '#15803d', borderRadius: '0.375rem', marginBottom: '1rem', fontSize: '0.875rem' }}>{profileMsg}</div>}
        {profileErr && <div style={{ padding: '0.5rem', backgroundColor: '#fee2e2', color: '#dc2626', borderRadius: '0.375rem', marginBottom: '1rem', fontSize: '0.875rem' }}>{profileErr}</div>}

        <form onSubmit={handleUpdateProfile} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
          <div>
            <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: '500', marginBottom: '0.25rem' }}>Full Name</label>
            <input type="text" value={name} onChange={(e) => setName(e.target.value)} required />
          </div>

          <div>
            <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: '500', marginBottom: '0.25rem' }}>Email Address</label>
            <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
          </div>

          <div>
            <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: '500', marginBottom: '0.25rem' }}>Account Role</label>
            <input type="text" value={user?.role || 'USER'} disabled style={{ backgroundColor: '#f1f5f9', cursor: 'not-allowed' }} />
          </div>

          <button type="submit" className="btn-primary" style={{ width: 'fit-content', marginTop: '0.5rem' }}>
            Update Profile
          </button>
        </form>
      </div>

      {/* Password Change */}
      <div className="card">
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '1.25rem' }}>
          <Key size={20} color="#2563eb" />
          <h3 style={{ fontSize: '1.125rem', fontWeight: 'bold' }}>Security & Password</h3>
        </div>

        {pwdMsg && <div style={{ padding: '0.5rem', backgroundColor: '#dcfce7', color: '#15803d', borderRadius: '0.375rem', marginBottom: '1rem', fontSize: '0.875rem' }}>{pwdMsg}</div>}
        {pwdErr && <div style={{ padding: '0.5rem', backgroundColor: '#fee2e2', color: '#dc2626', borderRadius: '0.375rem', marginBottom: '1rem', fontSize: '0.875rem' }}>{pwdErr}</div>}

        <form onSubmit={handleChangePassword} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
          <div>
            <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: '500', marginBottom: '0.25rem' }}>Current Password</label>
            <input type="password" value={currentPassword} onChange={(e) => setCurrentPassword(e.target.value)} required />
          </div>

          <div>
            <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: '500', marginBottom: '0.25rem' }}>New Password</label>
            <input type="password" value={newPassword} onChange={(e) => setNewPassword(e.target.value)} required minLength={6} />
          </div>

          <button type="submit" className="btn-primary" style={{ width: 'fit-content', marginTop: '0.5rem' }}>
            Change Password
          </button>
        </form>
      </div>
    </div>
  );
};

export default Profile;
