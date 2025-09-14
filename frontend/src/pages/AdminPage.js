import React, { useState, useEffect, useCallback, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { useDarkMode } from '../contexts/DarkModeContext';
import api from '../config/axios';
import toast from 'react-hot-toast';

const AdminPage = () => {
  const navigate = useNavigate();
  const { user, setUser } = useAuth();
  const { isDarkMode } = useDarkMode();
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [updating, setUpdating] = useState({});
  const [adminCheckLoading, setAdminCheckLoading] = useState(true);
  const [isAdmin, setIsAdmin] = useState(false);
  const hasCheckedAdmin = useRef(false);

  const fetchUsers = useCallback(async () => {
    if (!user?.email) return;
    
    try {
      setLoading(true);
      const response = await api.get(`/api/api/admin/users?userEmail=${user.email}`);
      if (response.data.success) {
        setUsers(response.data.data);
      } else {
        toast.error('Failed to fetch users');
      }
    } catch (error) {
      console.error('Error fetching users:', error);
      toast.error('Failed to fetch users');
    } finally {
      setLoading(false);
    }
  }, [user?.email]);

  const checkAdminStatus = useCallback(async () => {
    if (!user?.email) return;
    
    try {
      setAdminCheckLoading(true);
      const response = await api.get(`/api/api/admin/check-admin?userEmail=${user.email}`);
      if (response.data.success && response.data.data) {
        // User is admin, update their role in the context and localStorage
        const updatedUser = { ...user, role: 'ADMIN' };
        setUser(updatedUser);
        localStorage.setItem('user', JSON.stringify(updatedUser));
        setIsAdmin(true);
        // Fetch users
        await fetchUsers();
      } else {
        // User is not admin, show access denied
        setIsAdmin(false);
        console.log('User is not admin according to backend');
      }
    } catch (error) {
      console.error('Error checking admin status:', error);
      setIsAdmin(false);
      toast.error('Failed to verify admin status');
    } finally {
      setAdminCheckLoading(false);
    }
  }, [user, setUser, fetchUsers]);

  useEffect(() => {
    if (user && !hasCheckedAdmin.current) {
      // Check if user is actually admin by calling the backend
      checkAdminStatus();
      hasCheckedAdmin.current = true;
    }
  }, [user, checkAdminStatus]);

  const updateUserRole = async (userId, newRole) => {
    // Prevent admin from removing their own admin role
    if (userId === user.id && newRole === 'USER') {
      toast.error('You cannot remove admin privileges from yourself');
      return;
    }

    // Prevent removing the last admin
    const adminCount = users.filter(u => u.role === 'ADMIN').length;
    const targetUser = users.find(u => u.id === userId);
    
    if (targetUser && targetUser.role === 'ADMIN' && newRole === 'USER' && adminCount <= 1) {
      toast.error('Cannot remove the last admin user');
      return;
    }

    try {
      setUpdating(prev => ({ ...prev, [userId]: true }));
      
      const response = await api.put(`/api/api/admin/users/role?userEmail=${user.email}`, {
        userId: userId,
        role: newRole
      });

      if (response.data.success) {
        setUsers(prev => 
          prev.map(u => 
            u.id === userId ? { ...u, role: newRole } : u
          )
        );
        toast.success(`User role updated to ${newRole}`);
      } else {
        toast.error(response.data.error || 'Failed to update user role');
      }
    } catch (error) {
      console.error('Error updating user role:', error);
      toast.error(error.response?.data?.error || 'Failed to update user role');
    } finally {
      setUpdating(prev => ({ ...prev, [userId]: false }));
    }
  };

  const formatDate = (dateString) => {
    return new Date(dateString).toLocaleDateString();
  };

  const getRoleBadgeColor = (role) => {
    if (role === 'ADMIN') {
      return isDarkMode 
        ? 'bg-red-900 text-red-200 border-red-700' 
        : 'bg-red-100 text-red-800 border-red-200';
    }
    return isDarkMode 
      ? 'bg-gray-700 text-gray-200 border-gray-600' 
      : 'bg-gray-100 text-gray-800 border-gray-200';
  };

  // Show loading while checking admin status
  if (adminCheckLoading) {
    return (
      <div className={`min-h-screen flex items-center justify-center ${
        isDarkMode 
          ? 'bg-gradient-to-br from-gray-900 via-gray-800 to-gray-700' 
          : 'bg-gradient-to-br from-slate-50 via-blue-50 to-indigo-100'
      }`}>
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-500 mx-auto mb-4"></div>
          <p className={`${isDarkMode ? 'text-gray-300' : 'text-gray-600'}`}>
            Verifying admin access...
          </p>
        </div>
      </div>
    );
  }

  // Redirect if not admin
  if (!user || !isAdmin) {
    return (
      <div className={`min-h-screen flex items-center justify-center ${
        isDarkMode 
          ? 'bg-gradient-to-br from-gray-900 via-gray-800 to-gray-700' 
          : 'bg-gradient-to-br from-slate-50 via-blue-50 to-indigo-100'
      }`}>
        <div className="text-center">
          <div className="w-16 h-16 bg-red-500 rounded-full flex items-center justify-center mx-auto mb-4">
            <svg className="w-8 h-8 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L3.732 16.5c-.77.833.192 2.5 1.732 2.5z" />
            </svg>
          </div>
          <h2 className={`text-2xl font-bold mb-2 ${
            isDarkMode ? 'text-white' : 'text-gray-900'
          }`}>
            Access Denied
          </h2>
          <p className={`mb-4 ${
            isDarkMode ? 'text-gray-300' : 'text-gray-600'
          }`}>
            You need admin privileges to access this page.
          </p>
          <button
            onClick={() => navigate('/chat')}
            className={`px-6 py-2 rounded-lg font-medium transition-colors ${
              isDarkMode
                ? 'bg-blue-600 text-white hover:bg-blue-700'
                : 'bg-blue-500 text-white hover:bg-blue-600'
            }`}
          >
            Back to Chat
          </button>
        </div>
      </div>
    );
  }

  if (loading) {
    return (
      <div className={`min-h-screen flex items-center justify-center ${
        isDarkMode 
          ? 'bg-gradient-to-br from-gray-900 via-gray-800 to-gray-700' 
          : 'bg-gradient-to-br from-slate-50 via-blue-50 to-indigo-100'
      }`}>
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-500 mx-auto mb-4"></div>
          <p className={`${isDarkMode ? 'text-gray-300' : 'text-gray-600'}`}>
            Loading users...
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className={`min-h-screen py-8 ${
      isDarkMode 
        ? 'bg-gradient-to-br from-gray-900 via-gray-800 to-gray-700' 
        : 'bg-gradient-to-br from-slate-50 via-blue-50 to-indigo-100'
    }`}>
      <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8">
        {/* Header */}
        <div className="mb-8">
          <div className="flex justify-between items-center mb-4">
            <div className="flex items-center space-x-4">
              <button
                onClick={() => navigate('/chat')}
                className={`p-2 rounded-lg transition-colors ${
                  isDarkMode
                    ? 'bg-gray-700 text-gray-300 hover:bg-gray-600'
                    : 'bg-gray-200 text-gray-600 hover:bg-gray-300'
                }`}
                title="Back to Chat"
              >
                <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 19l-7-7m0 0l7-7m-7 7h18" />
                </svg>
              </button>
              <h1 className={`text-3xl font-bold ${
                isDarkMode ? 'text-white' : 'text-gray-900'
              }`}>
                Admin Management
              </h1>
            </div>
            <div className="flex items-center space-x-2">
              <button
                onClick={fetchUsers}
                disabled={loading}
                className={`p-2 rounded-lg transition-colors ${
                  loading
                    ? 'opacity-50 cursor-not-allowed'
                    : isDarkMode
                      ? 'bg-gray-700 text-gray-300 hover:bg-gray-600'
                      : 'bg-gray-200 text-gray-600 hover:bg-gray-300'
                }`}
                title="Refresh Users"
              >
                <svg className={`w-5 h-5 ${loading ? 'animate-spin' : ''}`} fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                </svg>
              </button>
              <div className={`px-3 py-1 rounded-full text-sm font-medium ${
                isDarkMode 
                  ? 'bg-red-900 text-red-200 border border-red-700' 
                  : 'bg-red-100 text-red-800 border border-red-200'
              }`}>
                Admin
              </div>
            </div>
          </div>
          <p className={`text-lg ${
            isDarkMode ? 'text-gray-300' : 'text-gray-600'
          }`}>
            Manage user roles and permissions
          </p>
        </div>

        {/* Users Table */}
        <div className={`rounded-2xl shadow-xl overflow-hidden ${
          isDarkMode 
            ? 'bg-gray-800 border border-gray-700' 
            : 'bg-white border border-gray-200'
        }`}>
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200 dark:divide-gray-700">
              <thead className={`${
                isDarkMode ? 'bg-gray-700' : 'bg-gray-50'
              }`}>
                <tr>
                  <th className={`px-6 py-4 text-left text-xs font-medium uppercase tracking-wider ${
                    isDarkMode ? 'text-gray-300' : 'text-gray-500'
                  }`}>
                    User
                  </th>
                  <th className={`px-6 py-4 text-left text-xs font-medium uppercase tracking-wider ${
                    isDarkMode ? 'text-gray-300' : 'text-gray-500'
                  }`}>
                    Email
                  </th>
                  <th className={`px-6 py-4 text-left text-xs font-medium uppercase tracking-wider ${
                    isDarkMode ? 'text-gray-300' : 'text-gray-500'
                  }`}>
                    Role
                  </th>
                  <th className={`px-6 py-4 text-left text-xs font-medium uppercase tracking-wider ${
                    isDarkMode ? 'text-gray-300' : 'text-gray-500'
                  }`}>
                    Joined
                  </th>
                  <th className={`px-6 py-4 text-left text-xs font-medium uppercase tracking-wider ${
                    isDarkMode ? 'text-gray-300' : 'text-gray-500'
                  }`}>
                    Actions
                  </th>
                </tr>
              </thead>
              <tbody className={`divide-y ${
                isDarkMode ? 'divide-gray-700' : 'divide-gray-200'
              }`}>
                {users.map((userData) => (
                  <tr key={userData.id} className={`${
                    isDarkMode ? 'hover:bg-gray-700' : 'hover:bg-gray-50'
                  }`}>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="flex items-center">
                        <div className={`h-10 w-10 rounded-full flex items-center justify-center text-sm font-medium ${
                          isDarkMode 
                            ? 'bg-blue-600 text-white' 
                            : 'bg-blue-500 text-white'
                        }`}>
                          {userData.name ? userData.name.charAt(0).toUpperCase() : 'U'}
                        </div>
                        <div className="ml-4">
                          <div className={`text-sm font-medium ${
                            isDarkMode ? 'text-white' : 'text-gray-900'
                          }`}>
                            {userData.name || 'Unknown User'}
                          </div>
                        </div>
                      </div>
                    </td>
                    <td className={`px-6 py-4 whitespace-nowrap text-sm ${
                      isDarkMode ? 'text-gray-300' : 'text-gray-500'
                    }`}>
                      {userData.email}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <span className={`inline-flex px-2 py-1 text-xs font-semibold rounded-full border ${
                        getRoleBadgeColor(userData.role)
                      }`}>
                        {userData.role}
                      </span>
                    </td>
                    <td className={`px-6 py-4 whitespace-nowrap text-sm ${
                      isDarkMode ? 'text-gray-300' : 'text-gray-500'
                    }`}>
                      {formatDate(userData.createdAt)}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium">
                      {userData.id !== user.id ? (
                        <div className="flex space-x-2">
                          <button
                            onClick={() => updateUserRole(userData.id, 'ADMIN')}
                            disabled={userData.role === 'ADMIN' || updating[userData.id]}
                            className={`px-3 py-1 text-xs font-medium rounded-md transition-colors ${
                              userData.role === 'ADMIN' || updating[userData.id]
                                ? 'opacity-50 cursor-not-allowed'
                                : isDarkMode
                                  ? 'bg-red-600 text-white hover:bg-red-700'
                                  : 'bg-red-500 text-white hover:bg-red-600'
                            }`}
                          >
                            {updating[userData.id] ? 'Updating...' : 'Make Admin'}
                          </button>
                          <button
                            onClick={() => updateUserRole(userData.id, 'USER')}
                            disabled={userData.role === 'USER' || updating[userData.id]}
                            className={`px-3 py-1 text-xs font-medium rounded-md transition-colors ${
                              userData.role === 'USER' || updating[userData.id]
                                ? 'opacity-50 cursor-not-allowed'
                                : isDarkMode
                                  ? 'bg-gray-600 text-white hover:bg-gray-700'
                                  : 'bg-gray-500 text-white hover:bg-gray-600'
                            }`}
                          >
                            {updating[userData.id] ? 'Updating...' : 'Make User'}
                          </button>
                        </div>
                      ) : (
                        <span className={`text-xs ${
                          isDarkMode ? 'text-gray-400' : 'text-gray-500'
                        }`}>
                          (You)
                        </span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>

        {/* PDF Upload Section */}
        <div className={`mt-8 rounded-2xl shadow-xl p-6 ${
          isDarkMode 
            ? 'bg-gray-800 border border-gray-700' 
            : 'bg-white border border-gray-200'
        }`}>
          <h2 className={`text-xl font-semibold mb-4 ${
            isDarkMode ? 'text-white' : 'text-gray-900'
          }`}>
            PDF Document Management
          </h2>
          <p className={`mb-4 ${
            isDarkMode ? 'text-gray-300' : 'text-gray-600'
          }`}>
            Upload and manage PDF documents for the HR chatbot knowledge base.
          </p>
          <button
            onClick={() => navigate('/upload')}
            className={`px-6 py-3 rounded-lg font-medium transition-colors ${
              isDarkMode
                ? 'bg-blue-600 text-white hover:bg-blue-700'
                : 'bg-blue-500 text-white hover:bg-blue-600'
            }`}
          >
            Upload PDF Documents
          </button>
        </div>
      </div>
    </div>
  );
};

export default AdminPage;


