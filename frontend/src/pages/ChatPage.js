import React, { useState, useEffect, useRef } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { useDarkMode } from '../contexts/DarkModeContext';
import Sidebar from '../components/Sidebar';
import ChatInterface from '../components/ChatInterface';
import DarkModeToggle from '../components/DarkModeToggle';

const ChatPage = () => {
  const { user } = useAuth();
  const { isDarkMode } = useDarkMode();
  const [selectedRoom, setSelectedRoom] = useState(null);
  const [sidebarOpen, setSidebarOpen] = useState(true);
  const [refreshSidebar, setRefreshSidebar] = useState(0);

  return (
    <div className={`h-screen flex ${
      isDarkMode 
        ? 'bg-gradient-to-br from-gray-900 via-gray-800 to-gray-700' 
        : 'bg-gradient-to-br from-slate-50 via-blue-50 to-indigo-100'
    }`}>
      {/* Sidebar */}
      <div
        className={`fixed inset-y-0 left-0 z-50 w-72 sm:w-80 ${
          isDarkMode 
            ? 'bg-gray-800/95 shadow-xl border-r border-gray-700/50' 
            : 'bg-white/95 shadow-xl border-r border-gray-200/50'
        } transform ${
          sidebarOpen ? 'translate-x-0' : '-translate-x-full'
        } lg:translate-x-0 lg:static lg:inset-0`}
      >
        <Sidebar
          selectedRoom={selectedRoom}
          onRoomSelect={setSelectedRoom}
          onClose={() => setSidebarOpen(false)}
          refreshTrigger={refreshSidebar}
        />
      </div>

      {/* Mobile sidebar overlay */}
      {sidebarOpen && (
        <div
          className="fixed inset-0 z-40 bg-black/20 lg:hidden"
          onClick={() => setSidebarOpen(false)}
        />
      )}

      {/* Main content */}
      <div className="flex-1 flex flex-col overflow-hidden">
        {/* Header */}
        <header className={`${
          isDarkMode 
            ? 'bg-gray-800 shadow-sm border-b border-gray-700' 
            : 'bg-white shadow-sm border-b border-gray-200'
        } px-3 sm:px-6 py-4`}>
          <div className="flex items-center justify-between">
            <div className="flex items-center">
              <button
                onClick={() => setSidebarOpen(!sidebarOpen)}
                className={`lg:hidden p-2 rounded-lg mr-4 ${
                  isDarkMode 
                    ? 'text-gray-300 hover:text-white hover:bg-gray-700' 
                    : 'text-gray-600 hover:text-gray-800 hover:bg-gray-100'
                }`}
              >
                <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
                </svg>
              </button>
              
              <div className="flex items-center space-x-2 sm:space-x-4">
                <div className="w-8 h-8 sm:w-10 sm:h-10 bg-gradient-to-br from-blue-500 to-purple-600 rounded-xl flex items-center justify-center shadow-sm">
                  <svg className="w-4 h-4 sm:w-5 sm:h-5 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
                  </svg>
                </div>
                <div>
                  <h1 className={`text-lg sm:text-xl font-bold ${
                    isDarkMode ? 'text-white' : 'text-gray-900'
                  }`}>
                    {selectedRoom ? selectedRoom.name : 'HR Assist'}
                  </h1>
                  <p className={`text-xs sm:text-sm ${
                    isDarkMode ? 'text-gray-400' : 'text-gray-600'
                  }`}>
                    {selectedRoom ? 'Chat with your AI assistant' : 'Select a chat room to start'}
                  </p>
                </div>
              </div>
            </div>

            <div className="flex items-center">
              <DarkModeToggle />
              <div className="ml-6 mr-4">
                <div className={`w-8 h-8 rounded-lg flex items-center justify-center ${
                  isDarkMode 
                    ? 'bg-gray-700 border border-gray-600' 
                    : 'bg-gray-200 border border-gray-300'
                }`}>
                  <span className={`text-sm font-bold ${
                    isDarkMode ? 'text-gray-200' : 'text-gray-700'
                  }`}>
                    {user?.name ? (() => {
                      const nameParts = user.name.trim().split(' ');
                      if (nameParts.length >= 2) {
                        return (nameParts[0][0] + nameParts[nameParts.length - 1][0]).toUpperCase();
                      }
                      return user.name[0].toUpperCase();
                    })() : 'U'}
                  </span>
                </div>
              </div>
              <div className="hidden sm:block">
                <p className={`text-sm font-semibold ${
                  isDarkMode ? 'text-white' : 'text-gray-900'
                }`}>{user?.name}</p>
                <p className={`text-xs ${
                  isDarkMode ? 'text-gray-400' : 'text-gray-500'
                }`}>{user?.email}</p>
              </div>
            </div>
          </div>
        </header>

        {/* Chat interface */}
        <main className="flex-1 overflow-hidden">
          {selectedRoom ? (
            <ChatInterface
              room={selectedRoom}
              onRoomUpdate={(updatedRoom) => {
                setSelectedRoom(updatedRoom);
                setRefreshSidebar(prev => prev + 1);
              }}
            />
          ) : (
            <div className={`h-full flex items-center justify-center ${
              isDarkMode 
                ? 'bg-gradient-to-br from-gray-900 via-gray-800 to-gray-700' 
                : 'bg-gradient-to-br from-slate-50 via-blue-50 to-indigo-100'
            }`}>
              <div className="text-center max-w-4xl mx-auto px-4 sm:px-8">
                <div className="w-24 h-24 sm:w-32 sm:h-32 bg-gradient-to-br from-blue-500 to-purple-600 rounded-2xl flex items-center justify-center mx-auto mb-6 sm:mb-8 shadow-lg">
                  <svg className="w-12 h-12 sm:w-16 sm:h-16 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
                  </svg>
                </div>
                
                <h3 className={`text-2xl sm:text-3xl font-bold mb-4 ${
                  isDarkMode ? 'text-white' : 'text-gray-900'
                }`}>
                  Welcome to HR Assist
                </h3>
                
                <p className={`text-base sm:text-lg mb-8 sm:mb-12 leading-relaxed max-w-2xl mx-auto ${
                  isDarkMode ? 'text-gray-300' : 'text-gray-600'
                }`}>
                  Your go-to HR assistant for quick answers and workplace guidance. 
                  Start a chat room and ask away.
                </p>
                
                <div className="grid grid-cols-1 md:grid-cols-3 gap-6 max-w-4xl mx-auto">
                  <div className={`rounded-xl p-6 shadow-sm ${
                    isDarkMode 
                      ? 'bg-gray-800 border border-gray-700' 
                      : 'bg-white border border-gray-200'
                  }`}>
                    <div className="w-12 h-12 bg-gradient-to-br from-green-500 to-green-600 rounded-xl flex items-center justify-center mx-auto mb-4">
                      <svg className="w-6 h-6 text-white" fill="currentColor" viewBox="0 0 20 20">
                        <path fillRule="evenodd" d="M3 4a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1zm0 4a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1zm0 4a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1zm0 4a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1z" clipRule="evenodd" />
                      </svg>
                    </div>
                    <h4 className={`text-lg font-semibold mb-2 ${
                      isDarkMode ? 'text-white' : 'text-gray-900'
                    }`}>Ask Questions</h4>
                    <p className={`text-sm ${
                      isDarkMode ? 'text-gray-300' : 'text-gray-600'
                    }`}>Get instant answers from company knowledge</p>
                  </div>
                  
                  <div className={`rounded-xl p-6 shadow-sm ${
                    isDarkMode 
                      ? 'bg-gray-800 border border-gray-700' 
                      : 'bg-white border border-gray-200'
                  }`}>
                    <div className="w-12 h-12 bg-gradient-to-br from-blue-500 to-blue-600 rounded-xl flex items-center justify-center mx-auto mb-4">
                      <svg className="w-6 h-6 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
                      </svg>
                    </div>
                    <h4 className={`text-lg font-semibold mb-2 ${
                      isDarkMode ? 'text-white' : 'text-gray-900'
                    }`}>Chat Rooms</h4>
                    <p className={`text-sm ${
                      isDarkMode ? 'text-gray-300' : 'text-gray-600'
                    }`}>Organize conversations by topic</p>
                  </div>
                  
                  <div className={`rounded-xl p-6 shadow-sm ${
                    isDarkMode 
                      ? 'bg-gray-800 border border-gray-700' 
                      : 'bg-white border border-gray-200'
                  }`}>
                    <div className="w-12 h-12 bg-gradient-to-br from-purple-500 to-purple-600 rounded-xl flex items-center justify-center mx-auto mb-4">
                      <svg className="w-6 h-6 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                      </svg>
                    </div>
                    <h4 className={`text-lg font-semibold mb-2 ${
                      isDarkMode ? 'text-white' : 'text-gray-900'
                    }`}>Smart Context</h4>
                    <p className={`text-sm ${
                      isDarkMode ? 'text-gray-300' : 'text-gray-600'
                    }`}>Continue chats without losing history</p>
                  </div>
                </div>
              </div>
            </div>
          )}
        </main>
      </div>
    </div>
  );
};

export default ChatPage;
