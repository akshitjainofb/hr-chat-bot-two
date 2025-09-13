import React, { useState, useEffect } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { useDarkMode } from '../contexts/DarkModeContext';
import api from '../config/axios';
import toast from 'react-hot-toast';

const Sidebar = ({ selectedRoom, onRoomSelect, onClose, refreshTrigger }) => {
  const { isDarkMode } = useDarkMode();
  const [rooms, setRooms] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showNewRoomForm, setShowNewRoomForm] = useState(false);
  const [newRoomName, setNewRoomName] = useState('');
  const [creatingRoom, setCreatingRoom] = useState(false);
  const [editingRoom, setEditingRoom] = useState(null);
  const [editRoomName, setEditRoomName] = useState('');
  const [updatingRoom, setUpdatingRoom] = useState(false);
  const location = useLocation();

  useEffect(() => {
    fetchChatRooms();
  }, []);

  // Refresh rooms when refreshTrigger changes
  useEffect(() => {
    if (refreshTrigger > 0) {
      fetchChatRooms();
    }
  }, [refreshTrigger]);

  const fetchChatRooms = async () => {
    try {
      const user = JSON.parse(localStorage.getItem('user'));
      if (!user || !user.email) {
        toast.error('User not authenticated');
        return;
      }
      
      const response = await api.get(`/api/chat/rooms?userEmail=${encodeURIComponent(user.email)}`);
      setRooms(response.data);
      if (response.data.length > 0 && !selectedRoom) {
        onRoomSelect(response.data[0]);
      }
    } catch (error) {
      console.error('Error fetching chat rooms:', error);
      toast.error('Failed to load chat rooms');
    } finally {
      setLoading(false);
    }
  };

  const createNewRoom = async (e) => {
    e.preventDefault();
    if (!newRoomName.trim()) return;

    setCreatingRoom(true);
    try {
      const user = JSON.parse(localStorage.getItem('user'));
      if (!user || !user.email) {
        toast.error('User not authenticated');
        return;
      }
      
      const response = await api.post('/api/chat/rooms', {
        name: newRoomName.trim(),
        userEmail: user.email
      });
      
      const newRoom = response.data;
      setRooms(prev => [newRoom, ...prev]);
      onRoomSelect(newRoom);
      setNewRoomName('');
      setShowNewRoomForm(false);
      toast.success('Chat room created successfully');
    } catch (error) {
      console.error('Error creating chat room:', error);
      toast.error('Failed to create chat room');
    } finally {
      setCreatingRoom(false);
    }
  };

  const deleteRoom = async (roomId, e) => {
    e.stopPropagation();
    if (!window.confirm('Are you sure you want to delete this chat room?')) return;

    try {
      const user = JSON.parse(localStorage.getItem('user'));
      if (!user || !user.email) {
        toast.error('User not authenticated');
        return;
      }

      await api.delete(`/api/chat/rooms/${roomId}?userEmail=${user.email}`);
      setRooms(prev => prev.filter(room => room.id !== roomId));
      if (selectedRoom?.id === roomId) {
        onRoomSelect(null);
      }
      toast.success('Chat room deleted successfully');
    } catch (error) {
      console.error('Error deleting chat room:', error);
      toast.error('Failed to delete chat room');
    }
  };

  const startEditRoom = (room, e) => {
    e.stopPropagation();
    setEditingRoom(room);
    setEditRoomName(room.name);
  };

  const updateRoom = async (e) => {
    e.preventDefault();
    if (!editRoomName.trim()) return;

    setUpdatingRoom(true);
    try {
      const user = JSON.parse(localStorage.getItem('user'));
      if (!user || !user.email) {
        toast.error('User not authenticated');
        return;
      }

      const response = await api.put(`/api/chat/rooms/${editingRoom.id}`, {
        name: editRoomName.trim(),
        userEmail: user.email
      });

      setRooms(prev => prev.map(room => 
        room.id === editingRoom.id ? response.data : room
      ));
      
      if (selectedRoom?.id === editingRoom.id) {
        onRoomSelect(response.data);
      }
      
      setEditingRoom(null);
      setEditRoomName('');
      toast.success('Room name updated successfully');
    } catch (error) {
      console.error('Error updating room:', error);
      toast.error('Failed to update room name');
    } finally {
      setUpdatingRoom(false);
    }
  };

  const cancelEdit = () => {
    setEditingRoom(null);
    setEditRoomName('');
  };

  const navigation = [
    { name: 'Chat', href: '/chat', icon: '💬', current: location.pathname === '/chat' },
    { name: 'Upload PDFs', href: '/upload', icon: '📄', current: location.pathname === '/upload' },
    { name: 'Settings', href: '/settings', icon: '⚙️', current: location.pathname === '/settings' },
  ];

  return (
    <div className={`flex flex-col h-full border-r ${
      isDarkMode 
        ? 'bg-gray-800/95 border-gray-700/50' 
        : 'bg-gray-50/95 border-gray-200/50'
    }`}>
      {/* Header */}
      <div className={`flex items-center justify-between p-6 ${
        isDarkMode 
          ? 'bg-gray-800/80 shadow-lg' 
          : 'bg-white/80 shadow-sm border-b border-gray-200/30'
      }`}>
        <h2 className={`text-xl font-bold ${
          isDarkMode ? 'text-white' : 'text-gray-900'
        }`}>
          Chat Rooms
        </h2>
        <button
          onClick={() => setShowNewRoomForm(true)}
          className={`p-2 rounded-lg transition-colors duration-200 ${
            isDarkMode 
              ? 'text-gray-300 hover:text-blue-400 hover:bg-gray-700' 
              : 'text-gray-600 hover:text-blue-600 hover:bg-gray-100'
          }`}
        >
          <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
          </svg>
        </button>
      </div>

      {/* New Room Form */}
      {showNewRoomForm && (
        <div className="p-4">
          <div className={`rounded-xl border border-dashed p-4 ${
            isDarkMode
              ? 'border-blue-500/30 bg-blue-900/10'
              : 'border-blue-300 bg-blue-50/50'
          }`}>
            <form onSubmit={createNewRoom} className="space-y-3">
              <div className="relative">
                <input
                  type="text"
                  value={newRoomName}
                  onChange={(e) => setNewRoomName(e.target.value)}
                  placeholder="Enter room name"
                  className={`w-full px-4 py-2.5 pl-10 rounded-lg text-sm border focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-all duration-200 ${
                    isDarkMode
                      ? 'bg-gray-700 border-gray-600 text-white placeholder-gray-400 focus:bg-gray-600'
                      : 'bg-white border-gray-300 text-gray-900 placeholder-gray-500 focus:bg-gray-50'
                  }`}
                  autoFocus
                />
                <div className="absolute inset-y-0 left-0 flex items-center pl-3">
                  <svg className="w-4 h-4 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
                  </svg>
                </div>
              </div>
              <div className="flex space-x-2">
                <button
                  type="submit"
                  disabled={creatingRoom || !newRoomName.trim()}
                  className={`flex-1 px-3 py-2 rounded-lg text-sm font-medium hover:shadow-sm disabled:opacity-50 disabled:cursor-not-allowed transition-all duration-200 ${
                    isDarkMode
                      ? 'bg-blue-600 text-white hover:bg-blue-700'
                      : 'bg-blue-600 text-white hover:bg-blue-700'
                  }`}
                >
                  {creatingRoom ? (
                    <div className="flex items-center justify-center">
                      <div className="w-3 h-3 border-2 border-white border-t-transparent rounded-full animate-spin mr-1.5"></div>
                      Creating...
                    </div>
                  ) : (
                    'Create'
                  )}
                </button>
                <button
                  type="button"
                  onClick={() => {
                    setShowNewRoomForm(false);
                    setNewRoomName('');
                  }}
                  className={`px-3 py-2 rounded-lg text-sm font-medium transition-all duration-200 ${
                    isDarkMode
                      ? 'bg-gray-600 text-gray-300 hover:bg-gray-500'
                      : 'bg-white border border-gray-300 text-gray-600 hover:bg-gray-50'
                  }`}
                >
                  Cancel
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Chat Rooms List */}
      <div className={`flex-1 overflow-y-auto relative z-10 ${
        isDarkMode 
          ? 'shadow-inner' 
          : ''
      }`}>
        {loading ? (
          <div className="p-6">
            <div className="space-y-4">
              {[...Array(3)].map((_, i) => (
                <div key={i} className="animate-pulse">
                  <div className={`h-16 rounded-xl ${
                    isDarkMode ? 'bg-gray-700' : 'bg-gray-200'
                  }`}></div>
                </div>
              ))}
            </div>
          </div>
        ) : rooms.length === 0 ? (
          <div className={`p-6 text-center ${
            isDarkMode ? 'text-gray-400' : 'text-gray-500'
          }`}>
            <div className={`w-16 h-16 rounded-xl flex items-center justify-center mx-auto mb-4 ${
              isDarkMode ? 'bg-gray-700' : 'bg-gray-100'
            }`}>
              <svg className="w-8 h-8 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
              </svg>
            </div>
            <p className={`text-sm font-semibold mb-1 ${
              isDarkMode ? 'text-gray-300' : 'text-gray-700'
            }`}>No chat rooms yet</p>
            <p className={`text-xs ${
              isDarkMode ? 'text-gray-500' : 'text-gray-400'
            }`}>Create one to get started</p>
          </div>
        ) : (
          <div className="p-4">
            <div>
              {rooms.map((room, index) => (
                <div key={room.id}>
                  {editingRoom?.id === room.id ? (
                    <form onSubmit={updateRoom} className={`mb-3 p-4 rounded-2xl border-2 ${
                      isDarkMode
                        ? 'bg-blue-900/20 border-blue-500/50'
                        : 'bg-blue-50 border-blue-300'
                    }`}>
                      <div className="relative">
                        <input
                          type="text"
                          value={editRoomName}
                          onChange={(e) => setEditRoomName(e.target.value)}
                          className={`w-full px-4 py-3 pl-10 text-sm border rounded-xl focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-all duration-200 ${
                            isDarkMode
                              ? 'bg-gray-700 border-gray-600 text-white placeholder-gray-400 focus:bg-gray-600'
                              : 'bg-white border-gray-300 text-gray-900 placeholder-gray-500 focus:bg-gray-50'
                          }`}
                          placeholder="Enter new room name"
                          autoFocus
                        />
                        <div className="absolute inset-y-0 left-0 flex items-center pl-4">
                          <svg className="w-4 h-4 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                          </svg>
                        </div>
                      </div>
                      <div className="flex space-x-2 mt-3">
                        <button
                          type="submit"
                          disabled={updatingRoom || !editRoomName.trim()}
                          className="px-4 py-2 bg-blue-600 text-white text-sm font-medium rounded-lg hover:bg-blue-700 disabled:opacity-50 transition-all duration-200 flex items-center"
                        >
                          {updatingRoom ? (
                            <>
                              <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin mr-2"></div>
                              Saving...
                            </>
                          ) : (
                            <>
                              <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                              </svg>
                              Save
                            </>
                          )}
                        </button>
                        <button
                          type="button"
                          onClick={cancelEdit}
                          className={`px-4 py-2 text-sm font-medium rounded-lg transition-all duration-200 ${
                            isDarkMode
                              ? 'bg-gray-600 text-gray-300 hover:bg-gray-500'
                              : 'bg-gray-300 text-gray-700 hover:bg-gray-400'
                          }`}
                        >
                          Cancel
                        </button>
                      </div>
                    </form>
                  ) : (
                    <div
                      className={`group relative mb-3 rounded-xl cursor-pointer border transition-all duration-200 ${
                        selectedRoom?.id === room.id
                          ? isDarkMode
                            ? 'bg-gray-700/60 border-gray-600 shadow-sm'
                            : 'bg-gray-100 border-gray-300 shadow-sm'
                          : isDarkMode
                            ? 'bg-gray-800/50 border-gray-700 hover:bg-gray-700/80 hover:border-gray-600 hover:shadow-sm'
                            : 'bg-white/80 border-gray-200 hover:bg-gray-50/80 hover:border-gray-300 hover:shadow-sm'
                      }`}
                      onClick={() => onRoomSelect(room)}
                    >
                      <div className="flex items-center justify-between p-4">
                        <div className="flex-1 min-w-0">
                          <div className="flex items-center">
                            <p className={`text-sm font-medium truncate transition-colors duration-200 ${
                              selectedRoom?.id === room.id
                                ? isDarkMode 
                                  ? 'text-white' 
                                  : 'text-gray-900'
                                : isDarkMode 
                                  ? 'text-gray-200 group-hover:text-white' 
                                  : 'text-gray-700 group-hover:text-gray-900'
                            }`}>
                              {room.name}
                            </p>
                          </div>
                          <div className="mt-1">
                            <p className={`text-xs transition-colors duration-200 ${
                              selectedRoom?.id === room.id
                                ? isDarkMode 
                                  ? 'text-gray-300' 
                                  : 'text-gray-600'
                                : isDarkMode 
                                  ? 'text-gray-500 group-hover:text-gray-400' 
                                  : 'text-gray-500 group-hover:text-gray-600'
                            }`}>
                              {room.messageCount} messages
                            </p>
                          </div>
                        </div>
                        <div className={`flex items-center space-x-1 transition-all duration-200 ${
                          selectedRoom?.id === room.id 
                            ? 'opacity-100' 
                            : 'opacity-0 group-hover:opacity-100'
                        }`}>
                          <button
                            onClick={(e) => startEditRoom(room, e)}
                            className={`p-2 rounded-lg transition-all duration-200 ${
                              isDarkMode
                                ? 'text-gray-400 hover:text-blue-400 hover:bg-gray-600/50'
                                : 'text-gray-400 hover:text-blue-500 hover:bg-blue-100/50'
                            }`}
                          >
                            <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                            </svg>
                          </button>
                          <button
                            onClick={(e) => deleteRoom(room.id, e)}
                            className={`p-2 rounded-lg transition-all duration-200 ${
                              isDarkMode
                                ? 'text-gray-400 hover:text-red-400 hover:bg-red-900/20'
                                : 'text-gray-400 hover:text-red-500 hover:bg-red-100/50'
                            }`}
                          >
                            <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                            </svg>
                          </button>
                        </div>
                      </div>
                    </div>
                  )}
                </div>
              ))}
            </div>
          </div>
        )}
      </div>

      {/* Navigation */}
      <div className={`p-6 ${
        isDarkMode 
          ? 'bg-gray-800/80 shadow-lg' 
          : 'bg-white/80 shadow-sm border-t border-gray-200/30'
      }`}>
        <nav className="space-y-3">
          {navigation.map((item, index) => (
            <Link
              key={item.name}
              to={item.href}
              onClick={onClose}
                className={`flex items-center px-4 py-3 text-sm font-medium rounded-xl transition-all duration-200 ${
                  item.current
                    ? 'bg-gradient-to-r from-blue-500 to-purple-600 text-white shadow-sm'
                    : isDarkMode
                      ? 'text-gray-200 hover:bg-gray-700 hover:text-white'
                      : 'text-gray-700 hover:bg-gray-100 hover:text-gray-900'
                }`}
            >
              <span className="mr-3 text-lg">{item.icon}</span>
              {item.name}
            </Link>
          ))}
        </nav>
      </div>
    </div>
  );
};

export default Sidebar;
