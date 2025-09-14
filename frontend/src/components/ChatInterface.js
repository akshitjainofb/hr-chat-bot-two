import React, { useState, useEffect, useRef } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { useDarkMode } from '../contexts/DarkModeContext';
import api from '../config/axios';
import axios from 'axios';
import toast from 'react-hot-toast';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';

const ChatInterface = ({ room, onRoomUpdate }) => {
  const { user } = useAuth();
  const { isDarkMode } = useDarkMode();
  const [messages, setMessages] = useState([]);
  const [inputMessage, setInputMessage] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [loadingStep, setLoadingStep] = useState('');
  const [showChatMenu, setShowChatMenu] = useState(false);
  const [retryCount, setRetryCount] = useState(0);
  const [currentRequest, setCurrentRequest] = useState(null);
  const [includeContext, setIncludeContext] = useState(room?.includeContext ?? true);
  const messagesEndRef = useRef(null);
  const menuRef = useRef(null);

  // Get user initials
  const getUserInitials = () => {
    const user = JSON.parse(localStorage.getItem('user'));
    if (user?.name) {
      const nameParts = user.name.trim().split(' ');
      if (nameParts.length >= 2) {
        return (nameParts[0][0] + nameParts[nameParts.length - 1][0]).toUpperCase();
      }
      return user.name[0].toUpperCase();
    }
    return 'U';
  };

  const loadingSteps = [
    '🤔 Thinking...',
    '📂 Fetching context...',
    '📑 Indexing documents...',
    '✨ Generating response...'
  ];

  useEffect(() => {
    if (room) {
      fetchMessages();
      setIncludeContext(room.includeContext ?? true);
    }
  }, [room]);

  // Close menu when clicking outside
  useEffect(() => {
    const handleClickOutside = (event) => {
      if (menuRef.current && !menuRef.current.contains(event.target)) {
        setShowChatMenu(false);
      }
    };

    if (showChatMenu) {
      document.addEventListener('mousedown', handleClickOutside);
    }

    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, [showChatMenu]);

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  // Cleanup pending requests on unmount
  useEffect(() => {
    return () => {
      if (currentRequest) {
        currentRequest.cancel('Component unmounted');
      }
    };
  }, [currentRequest]);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  const cancelRequest = () => {
    if (currentRequest) {
      currentRequest.cancel('Request cancelled by user');
      setCurrentRequest(null);
    }
    setIsLoading(false);
    setLoadingStep('');
    toast.info('Request cancelled');
  };

  const fetchMessages = async () => {
    try {
      const user = JSON.parse(localStorage.getItem('user'));
      if (!user || !user.email) {
        toast.error('User not authenticated');
        return;
      }
      
      const response = await api.get(`/api/chat/rooms/${room.id}?userEmail=${encodeURIComponent(user.email)}`);
      setMessages(response.data.messages || []);
    } catch (error) {
      console.error('Error fetching messages:', error);
      toast.error('Failed to load messages');
    }
  };

  const sendMessage = async (e, retry = false) => {
    e.preventDefault();
    if (!inputMessage.trim() || isLoading) return;

    const userMessage = {
      id: Date.now(),
      role: 'USER',
      message: inputMessage.trim(),
      createdAt: new Date().toISOString()
    };

    if (!retry) {
      setMessages(prev => [...prev, userMessage]);
      setInputMessage('');
    }
    setIsLoading(true);

    // Simulate loading steps
    let stepIndex = 0;
    const stepInterval = setInterval(() => {
      setLoadingStep(loadingSteps[stepIndex]);
      stepIndex = (stepIndex + 1) % loadingSteps.length;
    }, 1000);

    try {
      const user = JSON.parse(localStorage.getItem('user'));
      if (!user || !user.email) {
        toast.error('User not authenticated');
        clearInterval(stepInterval);
        setIsLoading(false);
        return;
      }
      
      // Create cancel token for this request
      const cancelToken = axios.CancelToken.source();
      setCurrentRequest(cancelToken);
      
      const response = await api.post('/api/chat/send', {
        chatRoomId: room.id,
        message: userMessage.message,
        userEmail: user.email,
        includeContext: includeContext
      }, {
        cancelToken: cancelToken.token
      });

      clearInterval(stepInterval);
      setIsLoading(false);
      setCurrentRequest(null);
      setRetryCount(0);

      if (response.data.success) {
        const assistantMessage = {
          id: Date.now() + 1,
          role: 'ASSISTANT',
          message: response.data.message,
          contextUsed: response.data.contextUsed,
          llmProviderUsed: response.data.llmProviderUsed,
          createdAt: new Date().toISOString()
        };

        setMessages(prev => [...prev, assistantMessage]);
        
        // Update the room with new message count
        if (onRoomUpdate) {
          onRoomUpdate(prev => ({
            ...prev,
            messageCount: prev.messageCount + 2 // +2 for user message and assistant response
          }));
        }
      } else {
        toast.error(response.data.error || 'Failed to send message');
      }
    } catch (error) {
      clearInterval(stepInterval);
      setIsLoading(false);
      setCurrentRequest(null);
      console.error('Error sending message:', error);
      
      // Handle different types of errors
      if (axios.isCancel(error)) {
        // Request was cancelled
        toast.info('Request cancelled');
        return;
      } else if (error.code === 'ECONNABORTED' || error.message.includes('timeout')) {
        if (retryCount < 2) {
          setRetryCount(prev => prev + 1);
          toast.error(`Request timed out. Retrying... (${retryCount + 1}/2)`);
          setTimeout(() => sendMessage(e, true), 2000);
        } else {
          toast.error('Request timed out after multiple attempts. Please try again.');
          setRetryCount(0);
        }
      } else if (error.response?.status === 408) {
        if (retryCount < 2) {
          setRetryCount(prev => prev + 1);
          toast.error(`Request timed out. Retrying... (${retryCount + 1}/2)`);
          setTimeout(() => sendMessage(e, true), 2000);
        } else {
          toast.error('Request timed out after multiple attempts. Please try again.');
          setRetryCount(0);
        }
      } else if (error.response?.status === 499) {
        toast.error('Request was cancelled. Please try again.');
      } else if (error.response?.status === 500) {
        toast.error('Server error. Please try again in a moment.');
      } else if (error.response?.data?.error) {
        toast.error(error.response.data.error);
      } else if (error.message === 'Network Error') {
        toast.error('Network error. Please check your connection and try again.');
      } else {
        toast.error('Failed to send message. Please try again.');
      }
    }
  };

  const formatTime = (timestamp) => {
    return new Date(timestamp).toLocaleTimeString([], { 
      hour: '2-digit', 
      minute: '2-digit' 
    });
  };

  const formatDate = (timestamp) => {
    const messageDate = new Date(timestamp);
    const today = new Date();
    const yesterday = new Date(today);
    yesterday.setDate(yesterday.getDate() - 1);
    
    // Reset time to compare only dates
    const messageDateOnly = new Date(messageDate.getFullYear(), messageDate.getMonth(), messageDate.getDate());
    const todayOnly = new Date(today.getFullYear(), today.getMonth(), today.getDate());
    const yesterdayOnly = new Date(yesterday.getFullYear(), yesterday.getMonth(), yesterday.getDate());
    
    if (messageDateOnly.getTime() === todayOnly.getTime()) {
      return 'Today';
    } else if (messageDateOnly.getTime() === yesterdayOnly.getTime()) {
      return 'Yesterday';
    } else {
      return messageDate.toLocaleDateString([], { 
        weekday: 'long',
        year: 'numeric',
        month: 'long',
        day: 'numeric'
      });
    }
  };

  const groupMessagesByDate = (messages) => {
    const grouped = [];
    let currentDate = null;
    
    messages.forEach((message, index) => {
      const messageDate = formatDate(message.createdAt);
      
      if (messageDate !== currentDate) {
        grouped.push({
          type: 'date-separator',
          date: messageDate,
          id: `date-${message.createdAt}`
        });
        currentDate = messageDate;
      }
      
      grouped.push({
        type: 'message',
        data: message,
        id: message.id
      });
    });
    
    return grouped;
  };

  const exportChat = () => {
    if (messages.length === 0) {
      toast.error('No messages to export');
      return;
    }

    const chatData = {
      roomName: room?.name || 'Chat',
      exportDate: new Date().toISOString(),
      messages: messages.map(msg => ({
        role: msg.role,
        message: msg.message,
        timestamp: msg.createdAt,
        contextUsed: msg.contextUsed,
        llmProviderUsed: msg.llmProviderUsed
      }))
    };

    const dataStr = JSON.stringify(chatData, null, 2);
    const dataBlob = new Blob([dataStr], { type: 'application/json' });
    const url = URL.createObjectURL(dataBlob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `${room?.name || 'chat'}_${new Date().toISOString().split('T')[0]}.json`;
    link.click();
    URL.revokeObjectURL(url);
    toast.success('Chat exported successfully');
  };

  const clearChat = async () => {
    if (!window.confirm('Are you sure you want to clear all messages in this chat?')) return;

    try {
      const user = JSON.parse(localStorage.getItem('user'));
      if (!user || !user.email) {
        toast.error('User not authenticated');
        return;
      }

      await api.delete(`/api/chat/rooms/${room.id}/messages?userEmail=${user.email}`);
      setMessages([]);
      
      // Update the room with cleared message count
      if (onRoomUpdate) {
        onRoomUpdate(prev => ({
          ...prev,
          messageCount: 0
        }));
      }
      
      toast.success('Chat cleared successfully');
    } catch (error) {
      console.error('Error clearing chat:', error);
      toast.error('Failed to clear chat');
    }
  };

  const DateSeparator = ({ date }) => {
    return (
      <div className="flex items-center justify-center my-6">
        <div className={`px-4 py-2 rounded-full text-sm font-medium ${
          isDarkMode
            ? 'bg-gray-700 text-gray-300 border border-gray-600'
            : 'bg-gray-100 text-gray-600 border border-gray-200'
        }`}>
          {date}
        </div>
      </div>
    );
  };

  const MessageBubble = ({ message, index }) => {
    const isUser = message.role === 'USER';
    
    return (
      <div className={`flex ${isUser ? 'justify-end' : 'justify-start'} mb-4`}>
        <div className={`flex max-w-md lg:max-w-lg xl:max-w-xl items-end ${isUser ? 'flex-row-reverse' : 'flex-row'}`}>
          {/* Avatar */}
          <div className={`w-10 h-10 flex-shrink-0 rounded-full flex items-center justify-center text-sm font-bold ${isUser ? 'ml-3' : 'mr-3'} ${
            isUser 
              ? 'bg-gradient-to-br from-blue-500 to-blue-600 text-white shadow-sm' 
              : isDarkMode
                ? 'bg-gray-600 text-gray-200 border border-gray-500'
                : 'bg-gray-200 text-gray-700 border border-gray-300'
          }`}>
            {isUser ? getUserInitials() : 'AI'}
          </div>
          
          {/* Message content */}
          <div className={`flex-1 px-3 py-2 rounded-2xl shadow-sm ${
            isUser 
              ? isDarkMode
                ? 'bg-gradient-to-br from-blue-600 to-blue-700 text-white rounded-br-lg border border-blue-500/20'
                : 'bg-gradient-to-br from-blue-500 to-blue-600 text-white rounded-br-lg border border-blue-400/20'
              : isDarkMode
                ? 'bg-gray-800 text-gray-100 border border-gray-600 rounded-bl-lg'
                : 'bg-white text-gray-900 border border-gray-200 rounded-bl-lg'
          }`}>
            <div className="text-sm leading-relaxed prose prose-sm max-w-none">
              <ReactMarkdown 
                remarkPlugins={[remarkGfm]}
                components={{
                  p: ({ children }) => <p className="mb-2 last:mb-0">{children}</p>,
                  strong: ({ children }) => <strong className="font-semibold text-current">{children}</strong>,
                  em: ({ children }) => <em className="italic text-current">{children}</em>,
                  ul: ({ children }) => <ul className="list-disc list-inside mb-2 space-y-1">{children}</ul>,
                  ol: ({ children }) => <ol className="list-decimal list-inside mb-2 space-y-1">{children}</ol>,
                  li: ({ children }) => <li className="text-current">{children}</li>,
                  code: ({ children }) => <code className="bg-gray-100 dark:bg-gray-700 px-1 py-0.5 rounded text-xs font-mono text-current">{children}</code>,
                  pre: ({ children }) => <pre className="bg-gray-100 dark:bg-gray-700 p-3 rounded-lg overflow-x-auto mb-2">{children}</pre>,
                  blockquote: ({ children }) => <blockquote className="border-l-4 border-gray-300 dark:border-gray-600 pl-4 italic mb-2">{children}</blockquote>,
                  h1: ({ children }) => <h1 className="text-lg font-bold mb-2 text-current">{children}</h1>,
                  h2: ({ children }) => <h2 className="text-base font-bold mb-2 text-current">{children}</h2>,
                  h3: ({ children }) => <h3 className="text-sm font-bold mb-2 text-current">{children}</h3>,
                }}
              >
                {message.message}
              </ReactMarkdown>
            </div>
            <div className="flex items-center justify-end mt-1.5">
              <p className={`text-xs ${
                isUser 
                  ? isDarkMode 
                    ? 'text-blue-200' 
                    : 'text-blue-100'
                  : isDarkMode
                    ? 'text-gray-400'
                    : 'text-gray-500'
              }`}>
                {formatTime(message.createdAt)}
              </p>
            </div>
          </div>
        </div>
      </div>
    );
  };

  const LoadingMessage = () => (
    <div className="flex justify-start mb-4">
      <div className="flex items-end">
        <div className={`w-10 h-10 rounded-full flex items-center justify-center text-sm font-bold border mr-3 ${
          isDarkMode
            ? 'bg-gray-600 text-gray-200 border-gray-500'
            : 'bg-gray-200 text-gray-700 border-gray-300'
        }`}>
          AI
        </div>
        <div className={`px-3 py-2 rounded-2xl rounded-bl-lg shadow-sm ${
          isDarkMode
            ? 'bg-gray-800 text-gray-100 border border-gray-600'
            : 'bg-white text-gray-900 border border-gray-200'
        }`}>
          <div className="flex items-center space-x-3">
            <div className="flex space-x-1">
              <div className="w-2 h-2 bg-blue-500 rounded-full animate-bounce"></div>
              <div className="w-2 h-2 bg-blue-500 rounded-full animate-bounce" style={{animationDelay: '0.1s'}}></div>
              <div className="w-2 h-2 bg-blue-500 rounded-full animate-bounce" style={{animationDelay: '0.2s'}}></div>
            </div>
            <span className="text-sm">{loadingStep}</span>
          </div>
        </div>
      </div>
    </div>
  );


  return (
    <div className={`flex flex-col h-full ${
      isDarkMode 
        ? 'bg-gradient-to-br from-gray-900 via-gray-800 to-gray-700' 
        : 'bg-gradient-to-br from-slate-50 via-blue-50 to-indigo-100'
    }`}>


      {/* Messages area */}
      <div className="flex-1 overflow-y-auto px-8 py-6 relative z-10">
        <div className="max-w-4xl mx-auto">
          {messages.length === 0 && !isLoading && (
            <div className="text-center py-16">
              <div className="w-20 h-20 mx-auto mb-6 bg-gradient-to-br from-blue-500 to-purple-600 rounded-2xl flex items-center justify-center shadow-lg">
                <svg className="w-10 h-10 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
                </svg>
              </div>
              <h3 className={`text-2xl font-bold mb-3 ${
                isDarkMode ? 'text-white' : 'text-gray-800'
              }`}>Start a conversation</h3>
              <p className={`text-lg ${
                isDarkMode ? 'text-gray-300' : 'text-gray-600'
              }`}>Ask me anything about your documents or general questions!</p>
            </div>
          )}
          
          {groupMessagesByDate(messages).map((item) => {
            if (item.type === 'date-separator') {
              return <DateSeparator key={item.id} date={item.date} />;
            } else {
              return <MessageBubble key={item.id} message={item.data} index={0} />;
            }
          })}
          
          {isLoading && <LoadingMessage />}
          <div ref={messagesEndRef} />
        </div>
      </div>

      {/* Input area */}
      <div className={`px-8 py-6 ${
        isDarkMode 
          ? 'bg-gray-800/50 border-t border-gray-700/50' 
          : 'bg-white/30 border-t border-gray-200/50'
      }`}>
        <div className="max-w-4xl mx-auto">
          <form onSubmit={sendMessage} className="flex items-center space-x-4">
            <div className="flex-1 relative">
              <input
                type="text"
                value={inputMessage}
                onChange={(e) => setInputMessage(e.target.value)}
                placeholder="Type your message..."
                className={`w-full px-6 py-4 rounded-2xl focus:ring-2 focus:ring-blue-500 focus:border-blue-500 text-sm shadow-sm ${
                  isDarkMode
                    ? 'bg-gray-700 border border-gray-600 text-white placeholder-gray-400'
                    : 'bg-white border border-gray-300 text-gray-900 placeholder-gray-500'
                }`}
                disabled={isLoading}
              />
              <div className="absolute right-4 top-1/2 transform -translate-y-1/2">
                <svg className="w-5 h-5 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z" />
                </svg>
              </div>
            </div>
            
            {/* Context Toggle Button */}
            <div className="relative">
              <button
                type="button"
                onClick={async () => {
                  const newContextSetting = !includeContext;
                  setIncludeContext(newContextSetting);
                  
                  // Update the context setting on the backend
                  try {
                    await api.put(`/api/chat/rooms/${room.id}/context`, {
                      userEmail: user.email,
                      includeContext: newContextSetting
                    });
                    
                    // Update the room in the parent component
                    if (onRoomUpdate) {
                      onRoomUpdate(prev => ({
                        ...prev,
                        includeContext: newContextSetting
                      }));
                    }
                  } catch (error) {
                    console.error('Error updating context setting:', error);
                    // Revert the state if the update failed
                    setIncludeContext(includeContext);
                    toast.error('Failed to update context setting');
                  }
                }}
                className={`h-12 w-12 rounded-2xl shadow-lg flex items-center justify-center transition-colors duration-200 ${
                  includeContext
                    ? isDarkMode
                      ? 'bg-blue-600 border border-blue-500 text-white hover:bg-blue-700'
                      : 'bg-blue-500 border border-blue-400 text-white hover:bg-blue-600'
                    : isDarkMode
                      ? 'bg-gray-700 border border-gray-600 text-gray-300 hover:bg-gray-600'
                      : 'bg-white border border-gray-200 text-gray-600 hover:bg-gray-50'
                }`}
                title={includeContext ? 'Context enabled - Click to disable' : 'Context disabled - Click to enable'}
              >
                <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                </svg>
              </button>
            </div>

            {/* Chat Menu Button */}
            {room && messages.length > 0 && (
              <div className="relative" ref={menuRef}>
                <button
                  type="button"
                  onClick={() => setShowChatMenu(!showChatMenu)}
                  className={`h-12 w-12 rounded-2xl shadow-lg flex items-center justify-center transition-colors duration-200 ${
                    isDarkMode
                      ? 'bg-gray-700 border border-gray-600 text-gray-300 hover:bg-gray-600'
                      : 'bg-white border border-gray-200 text-gray-600 hover:bg-gray-50'
                  }`}
                >
                  <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 5v.01M12 12v.01M12 19v.01M12 6a1 1 0 110-2 1 1 0 010 2zm0 7a1 1 0 110-2 1 1 0 010 2zm0 7a1 1 0 110-2 1 1 0 010 2z" />
                  </svg>
                </button>
                
                {showChatMenu && (
                  <div className={`absolute right-0 bottom-14 w-48 rounded-lg shadow-lg border py-2 z-30 ${
                    isDarkMode
                      ? 'bg-gray-800 border-gray-700'
                      : 'bg-white border-gray-200'
                  }`}>
                    <button
                      onClick={() => {
                        exportChat();
                        setShowChatMenu(false);
                      }}
                      className={`w-full px-4 py-2 text-left text-sm flex items-center ${
                        isDarkMode
                          ? 'text-gray-300 hover:bg-gray-700'
                          : 'text-gray-700 hover:bg-gray-100'
                      }`}
                    >
                      <svg className="w-4 h-4 mr-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                      </svg>
                      Export Chat
                    </button>
                    <button
                      onClick={() => {
                        clearChat();
                        setShowChatMenu(false);
                      }}
                      className={`w-full px-4 py-2 text-left text-sm flex items-center ${
                        isDarkMode
                          ? 'text-red-400 hover:bg-red-900/20'
                          : 'text-red-600 hover:bg-red-50'
                      }`}
                    >
                      <svg className="w-4 h-4 mr-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                      </svg>
                      Clear Chat
                    </button>
                  </div>
                )}
              </div>
            )}
            
            {isLoading ? (
              <button
                type="button"
                onClick={cancelRequest}
                className="h-12 px-6 bg-red-500 text-white rounded-2xl shadow-lg hover:shadow-xl hover:bg-red-600 font-medium flex items-center justify-center transition-colors"
              >
                <svg className="w-5 h-5 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                </svg>
                Cancel
              </button>
            ) : (
              <button
                type="submit"
                disabled={!inputMessage.trim()}
                className="h-12 px-6 bg-gradient-to-r from-blue-500 to-purple-600 text-white rounded-2xl shadow-lg hover:shadow-xl disabled:opacity-50 disabled:cursor-not-allowed font-medium flex items-center justify-center"
              >
                <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 7l5 5m0 0l-5 5m5-5H6" />
                </svg>
              </button>
            )}
          </form>
        </div>
      </div>
    </div>
  );
};

export default ChatInterface;
