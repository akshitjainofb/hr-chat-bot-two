import React, { useState, useEffect } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { useDarkMode } from '../contexts/DarkModeContext';
import { useNavigate } from 'react-router-dom';
import api from '../config/axios';
import toast from 'react-hot-toast';
import DarkModeToggle from '../components/DarkModeToggle';

const SettingsPage = () => {
  const { user, logout, setUser } = useAuth();
  const { isDarkMode } = useDarkMode();
  const navigate = useNavigate();
  const [llmProviders, setLlmProviders] = useState([]);
  const [selectedProvider, setSelectedProvider] = useState('');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [editingProfile, setEditingProfile] = useState(false);
  const [profileName, setProfileName] = useState(user?.name || '');
  const [updatingProfile, setUpdatingProfile] = useState(false);

  useEffect(() => {
    fetchLlmProviders();
  }, []);

  const fetchLlmProviders = async () => {
    try {
      const response = await api.get('/api/settings/llm-providers');
      setLlmProviders(response.data.providers);
      setSelectedProvider(user?.preferredLlmProvider || response.data.defaultProvider);
    } catch (error) {
      console.error('Error fetching LLM providers:', error);
      toast.error('Failed to load settings');
    } finally {
      setLoading(false);
    }
  };

  const handleProviderChange = async (provider) => {
    setSaving(true);
    try {
      const user = JSON.parse(localStorage.getItem('user'));
      if (!user || !user.email) {
        toast.error('User not authenticated');
        return;
      }
      
      const response = await api.post('/api/settings/llm-provider', { 
        provider,
        userEmail: user.email
      });
      
      // Update user in localStorage and context with the new preference
      const updatedUser = { ...user, preferredLlmProvider: response.data.preferredLlmProvider };
      localStorage.setItem('user', JSON.stringify(updatedUser));
      setUser(updatedUser);
      
      setSelectedProvider(provider);
      toast.success('LLM provider updated successfully');
    } catch (error) {
      console.error('Error updating LLM provider:', error);
      toast.error('Failed to update LLM provider');
    } finally {
      setSaving(false);
    }
  };

  const handleProfileUpdate = async (e) => {
    e.preventDefault();
    if (!profileName.trim()) return;

    setUpdatingProfile(true);
    try {
      const user = JSON.parse(localStorage.getItem('user'));
      if (!user || !user.email) {
        toast.error('User not authenticated');
        return;
      }

      const response = await api.put('/api/settings/user', {
        name: profileName.trim(),
        userEmail: user.email
      });

      // Update user in localStorage and context
      const updatedUser = { ...user, name: response.data.name };
      localStorage.setItem('user', JSON.stringify(updatedUser));
      
      setEditingProfile(false);
      toast.success('Profile updated successfully');
      
      // Refresh the page to update the user context
      window.location.reload();
    } catch (error) {
      console.error('Error updating profile:', error);
      toast.error('Failed to update profile');
    } finally {
      setUpdatingProfile(false);
    }
  };

  const startEditProfile = () => {
    setEditingProfile(true);
    setProfileName(user?.name || '');
  };

  const cancelEditProfile = () => {
    setEditingProfile(false);
    setProfileName(user?.name || '');
  };

  const providerInfo = {
    openai: {
      name: 'OpenAI GPT',
      description: 'Advanced language model with excellent reasoning capabilities',
      icon: '🤖',
      color: 'bg-green-100 text-green-800'
    },
    gemini: {
      name: 'Google Gemini',
      description: 'Google\'s multimodal AI model with strong performance',
      icon: '🧠',
      color: 'bg-blue-100 text-blue-800'
    },
    huggingface: {
      name: 'Hugging Face (API)',
      description: 'Open-source models via API with good performance',
      icon: '🤗',
      color: 'bg-purple-100 text-purple-800'
    },
    'local-huggingface': {
      name: 'Hugging Face (Local)',
      description: 'Run models locally - no API key needed, completely private',
      icon: '🏠',
      color: 'bg-orange-100 text-orange-800'
    }
  };

  if (loading) {
    return (
      <div className={`min-h-screen flex items-center justify-center ${
        isDarkMode ? 'bg-gradient-to-br from-gray-900 via-gray-800 to-gray-700' : 'bg-gray-50'
      }`}>
        <div className="text-center">
          <div className="w-8 h-8 border-4 border-primary-600 border-t-transparent rounded-full animate-spin mx-auto mb-4"></div>
          <p className={isDarkMode ? 'text-gray-300' : 'text-gray-600'}>Loading settings...</p>
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
      {/* Background Pattern */}
      <div className="absolute inset-0 overflow-hidden">
        <div className="absolute -top-40 -right-40 w-80 h-80 bg-blue-400 rounded-full mix-blend-multiply filter blur-xl opacity-20"></div>
        <div className="absolute -bottom-40 -left-40 w-80 h-80 bg-purple-400 rounded-full mix-blend-multiply filter blur-xl opacity-20"></div>
        <div className="absolute top-40 left-1/2 w-80 h-80 bg-pink-400 rounded-full mix-blend-multiply filter blur-xl opacity-20"></div>
      </div>
      <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 relative z-10">
        {/* Header */}
        <div className="text-center mb-8">
          <div className="flex justify-between items-center mb-4 relative z-20">
            <button
              onClick={() => navigate('/chat')}
              className={`flex items-center space-x-2 px-4 py-2 rounded-lg transition-all duration-200 relative z-30 ${
                isDarkMode
                  ? 'text-gray-300 hover:text-white hover:bg-gray-700'
                  : 'text-gray-600 hover:text-gray-800 hover:bg-gray-100'
              }`}
            >
              <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
              </svg>
              <span className="text-sm font-medium">Back to Chat</span>
            </button>
            <DarkModeToggle />
          </div>
          <div className="w-16 h-16 bg-gradient-to-br from-blue-500 to-purple-600 rounded-2xl flex items-center justify-center mx-auto mb-4 shadow-lg">
            <svg className="w-8 h-8 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
            </svg>
          </div>
          <h1 className={`text-3xl font-bold mb-2 ${
            isDarkMode 
              ? 'text-white' 
              : 'bg-gradient-to-r from-gray-900 to-gray-700 bg-clip-text text-transparent'
          }`}>Settings</h1>
          <p className={isDarkMode ? 'text-gray-300' : 'text-gray-600'}>
            Manage your account preferences and AI model settings
          </p>
        </div>

        <div className="space-y-6">
          {/* User Profile */}
          <div className={`rounded-2xl shadow-lg p-6 ${
            isDarkMode 
              ? 'bg-gray-800/90 backdrop-blur-xl border border-gray-700' 
              : 'bg-white/80 backdrop-blur-xl border border-white/20'
          }`}>
            <div className="flex items-center justify-between mb-4">
              <h2 className={`text-lg font-semibold flex items-center ${
                isDarkMode ? 'text-white' : 'text-gray-900'
              }`}>
                <svg className="w-5 h-5 mr-2 text-blue-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                </svg>
                Profile Information
              </h2>
              {!editingProfile && (
                <button
                  onClick={startEditProfile}
                  className={`px-3 py-1 rounded-lg text-sm font-medium ${
                    isDarkMode
                      ? 'bg-blue-900/30 text-blue-400 hover:bg-blue-900/50'
                      : 'bg-blue-100 text-blue-600 hover:bg-blue-200'
                  }`}
                >
                  Edit
                </button>
              )}
            </div>
            
            {editingProfile ? (
              <form onSubmit={handleProfileUpdate} className="space-y-4">
                <div>
                  <label className={`block text-sm font-medium mb-2 ${
                    isDarkMode ? 'text-gray-200' : 'text-gray-700'
                  }`}>Name</label>
                  <input
                    type="text"
                    value={profileName}
                    onChange={(e) => setProfileName(e.target.value)}
                    className={`w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 ${
                      isDarkMode
                        ? 'bg-gray-700 border-gray-600 text-white placeholder-gray-400'
                        : 'bg-white border-gray-300 text-gray-900'
                    }`}
                    placeholder="Enter your name"
                    required
                  />
                </div>
                <div className="flex space-x-3">
                  <button
                    type="submit"
                    disabled={updatingProfile || !profileName.trim()}
                    className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50 text-sm font-medium"
                  >
                    {updatingProfile ? 'Saving...' : 'Save'}
                  </button>
                  <button
                    type="button"
                    onClick={cancelEditProfile}
                    className={`px-4 py-2 rounded-lg text-sm font-medium ${
                      isDarkMode
                        ? 'bg-gray-600 text-gray-200 hover:bg-gray-500'
                        : 'bg-gray-300 text-gray-700 hover:bg-gray-400'
                    }`}
                  >
                    Cancel
                  </button>
                </div>
              </form>
            ) : (
              <div className="flex items-center space-x-4">
                <div className="w-16 h-16 bg-gradient-to-br from-blue-500 to-purple-600 rounded-2xl flex items-center justify-center shadow-lg">
                  <span className="text-2xl font-bold text-white">
                    {user?.name?.charAt(0)?.toUpperCase()}
                  </span>
                </div>
                <div>
                  <h3 className={`text-lg font-medium ${
                    isDarkMode ? 'text-white' : 'text-gray-900'
                  }`}>{user?.name}</h3>
                  <p className={isDarkMode ? 'text-gray-300' : 'text-gray-500'}>{user?.email}</p>
                  <p className={`text-sm ${
                    isDarkMode ? 'text-gray-400' : 'text-gray-400'
                  }`}>
                    Member since {new Date(user?.createdAt).toLocaleDateString()}
                  </p>
                </div>
              </div>
            )}
          </div>

          {/* LLM Provider Settings */}
          <div className={`rounded-2xl shadow-lg p-6 ${
            isDarkMode 
              ? 'bg-gray-800/90 backdrop-blur-xl border border-gray-700' 
              : 'bg-white/80 backdrop-blur-xl border border-white/20'
          }`}>
            <h2 className={`text-lg font-semibold mb-4 flex items-center ${
              isDarkMode ? 'text-white' : 'text-gray-900'
            }`}>
              <svg className="w-5 h-5 mr-2 text-blue-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9.75 17L9 20l-1 1h8l-1-1-.75-3M3 13h18M5 17h14a2 2 0 002-2V5a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
              </svg>
              AI Model Preferences
            </h2>
            <p className={`mb-6 ${
              isDarkMode ? 'text-gray-300' : 'text-gray-600'
            }`}>
              Choose your preferred AI model for generating responses. Each model has different strengths and capabilities.
            </p>
            
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              {llmProviders.map((provider) => {
                const info = providerInfo[provider];
                const isSelected = selectedProvider === provider;
                const isDisabled = saving;
                
                return (
                  <div
                    key={provider}
                    className={`relative border-2 rounded-xl p-4 cursor-pointer transition-all duration-200 ${
                      isSelected
                        ? isDarkMode
                          ? 'border-blue-500 bg-gradient-to-br from-blue-900/30 to-purple-900/30 shadow-md'
                          : 'border-blue-500 bg-gradient-to-br from-blue-50 to-purple-50 shadow-md'
                        : isDarkMode
                          ? 'border-gray-600 hover:border-gray-500 hover:shadow-sm'
                          : 'border-gray-200 hover:border-gray-300 hover:shadow-sm'
                    } ${isDisabled ? 'opacity-50 cursor-not-allowed' : ''}`}
                    onClick={() => !isDisabled && handleProviderChange(provider)}
                  >
                    <div className="text-center">
                      <div className="text-3xl mb-3">{info?.icon}</div>
                      <h3 className={`font-semibold mb-2 ${
                        isDarkMode ? 'text-white' : 'text-gray-900'
                      }`}>{info?.name}</h3>
                      <p className={`text-sm mb-3 ${
                        isDarkMode ? 'text-gray-300' : 'text-gray-600'
                      }`}>{info?.description}</p>
                      <span className={`px-3 py-1 rounded-full text-xs font-medium ${info?.color}`}>
                        {provider}
                      </span>
                    </div>
                  </div>
                );
              })}
            </div>
            
            {saving && (
              <div className="mt-4 flex items-center justify-center space-x-2 text-blue-600">
                <div className="w-4 h-4 border-2 border-blue-600 border-t-transparent rounded-full animate-spin"></div>
                <span className="text-sm">Updating preferences...</span>
              </div>
            )}
          </div>

          {/* System Information */}
          <div className={`rounded-2xl shadow-lg p-6 ${
            isDarkMode 
              ? 'bg-gray-800/90 backdrop-blur-xl border border-gray-700' 
              : 'bg-white/80 backdrop-blur-xl border border-white/20'
          }`}>
            <h2 className={`text-lg font-semibold mb-4 flex items-center ${
              isDarkMode ? 'text-white' : 'text-gray-900'
            }`}>
              <svg className="w-5 h-5 mr-2 text-blue-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
              </svg>
              System Information
            </h2>
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
              <div className="space-y-4">
                <h3 className={`font-semibold text-lg ${
                  isDarkMode ? 'text-white' : 'text-gray-900'
                }`}>Features</h3>
                <div className="space-y-3">
                  <div className={`flex items-center p-3 rounded-lg ${
                    isDarkMode ? 'bg-green-900/30' : 'bg-green-50'
                  }`}>
                    <svg className="w-5 h-5 text-green-500 mr-3" fill="currentColor" viewBox="0 0 20 20">
                      <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" />
                    </svg>
                    <span className={`font-medium ${
                      isDarkMode ? 'text-gray-200' : 'text-gray-700'
                    }`}>PDF Document Search</span>
                  </div>
                  <div className={`flex items-center p-3 rounded-lg ${
                    isDarkMode ? 'bg-blue-900/30' : 'bg-blue-50'
                  }`}>
                    <svg className="w-5 h-5 text-blue-500 mr-3" fill="currentColor" viewBox="0 0 20 20">
                      <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" />
                    </svg>
                    <span className={`font-medium ${
                      isDarkMode ? 'text-gray-200' : 'text-gray-700'
                    }`}>Multiple AI Models</span>
                  </div>
                  <div className={`flex items-center p-3 rounded-lg ${
                    isDarkMode ? 'bg-purple-900/30' : 'bg-purple-50'
                  }`}>
                    <svg className="w-5 h-5 text-purple-500 mr-3" fill="currentColor" viewBox="0 0 20 20">
                      <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" />
                    </svg>
                    <span className={`font-medium ${
                      isDarkMode ? 'text-gray-200' : 'text-gray-700'
                    }`}>Chat History</span>
                  </div>
                </div>
              </div>
              <div className="space-y-4">
                <h3 className={`font-semibold text-lg ${
                  isDarkMode ? 'text-white' : 'text-gray-900'
                }`}>System Status</h3>
                <div className="space-y-3">
                  <div className={`flex items-center justify-between p-3 rounded-lg ${
                    isDarkMode ? 'bg-gray-700' : 'bg-gray-50'
                  }`}>
                    <span className={`font-medium ${
                      isDarkMode ? 'text-gray-200' : 'text-gray-700'
                    }`}>Database</span>
                    <span className="px-3 py-1 bg-green-100 text-green-800 text-sm font-medium rounded-full">Connected</span>
                  </div>
                  <div className={`flex items-center justify-between p-3 rounded-lg ${
                    isDarkMode ? 'bg-gray-700' : 'bg-gray-50'
                  }`}>
                    <span className={`font-medium ${
                      isDarkMode ? 'text-gray-200' : 'text-gray-700'
                    }`}>Vector Search</span>
                    <span className="px-3 py-1 bg-green-100 text-green-800 text-sm font-medium rounded-full">Active</span>
                  </div>
                  <div className={`flex items-center justify-between p-3 rounded-lg ${
                    isDarkMode ? 'bg-gray-700' : 'bg-gray-50'
                  }`}>
                    <span className={`font-medium ${
                      isDarkMode ? 'text-gray-200' : 'text-gray-700'
                    }`}>AI Models</span>
                    <span className="px-3 py-1 bg-green-100 text-green-800 text-sm font-medium rounded-full">Available</span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          {/* Logout */}
          <div className={`rounded-2xl shadow-lg p-6 ${
            isDarkMode 
              ? 'bg-gray-800/90 backdrop-blur-xl border border-gray-700' 
              : 'bg-white/80 backdrop-blur-xl border border-white/20'
          }`}>
            <h2 className={`text-lg font-semibold mb-4 flex items-center ${
              isDarkMode ? 'text-white' : 'text-gray-900'
            }`}>
              <svg className="w-5 h-5 mr-2 text-red-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
              </svg>
              Account Actions
            </h2>
            <button
              onClick={logout}
              className="px-6 py-3 bg-gradient-to-r from-red-500 to-red-600 text-white rounded-xl hover:from-red-600 hover:to-red-700 shadow-lg hover:shadow-xl transition-all duration-200 font-medium"
            >
              Sign Out
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default SettingsPage;
