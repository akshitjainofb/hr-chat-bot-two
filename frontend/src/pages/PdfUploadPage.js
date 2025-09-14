import React, { useState, useEffect, useRef } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { useDarkMode } from '../contexts/DarkModeContext';
import { useNavigate } from 'react-router-dom';
import api from '../config/axios';
import toast from 'react-hot-toast';
import DarkModeToggle from '../components/DarkModeToggle';

const PdfUploadPage = () => {
  const { user } = useAuth();
  const { isDarkMode } = useDarkMode();
  const navigate = useNavigate();
  const [documents, setDocuments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [uploading, setUploading] = useState(false);
  const [dragActive, setDragActive] = useState(false);
  const [deletingDocument, setDeletingDocument] = useState(null);
  const fileInputRef = useRef(null);

  useEffect(() => {
    if (user && user.role !== 'ADMIN') {
      toast.error('Admin access required for PDF upload');
      navigate('/admin');
      return;
    }
    if (user && user.role === 'ADMIN') {
      fetchDocuments();
    }
  }, [user, navigate]);

  const fetchDocuments = async () => {
    try {
      const user = JSON.parse(localStorage.getItem('user'));
      if (!user || !user.email) {
        toast.error('User not authenticated');
        return;
      }
      
      const response = await api.get(`/api/pdf/documents?userEmail=${encodeURIComponent(user.email)}`);
      setDocuments(response.data);
    } catch (error) {
      console.error('Error fetching documents:', error);
      toast.error('Failed to load documents');
    } finally {
      setLoading(false);
    }
  };

  const handleFileUpload = async (files) => {
    if (!files || files.length === 0) return;

    setUploading(true);
    const uploadPromises = Array.from(files).map(async (file) => {
      if (file.type !== 'application/pdf') {
        toast.error(`${file.name} is not a PDF file`);
        return null;
      }

      const user = JSON.parse(localStorage.getItem('user'));
      if (!user || !user.email) {
        toast.error('User not authenticated');
        return null;
      }
      
      const formData = new FormData();
      formData.append('file', file);
      formData.append('userEmail', user.email);

      try {
        const response = await api.post('/api/pdf/upload', formData, {
          headers: {
            'Content-Type': 'multipart/form-data',
          },
        });
        return response.data.document;
      } catch (error) {
        console.error(`Error uploading ${file.name}:`, error);
        toast.error(`Failed to upload ${file.name}`);
        return null;
      }
    });

    try {
      const results = await Promise.all(uploadPromises);
      const successfulUploads = results.filter(result => result !== null);
      
      if (successfulUploads.length > 0) {
        setDocuments(prev => [...successfulUploads, ...prev]);
        toast.success(`${successfulUploads.length} document(s) uploaded successfully`);
      }
    } catch (error) {
      console.error('Upload error:', error);
    } finally {
      setUploading(false);
    }
  };

  const handleDrag = (e) => {
    e.preventDefault();
    e.stopPropagation();
    if (e.type === 'dragenter' || e.type === 'dragover') {
      setDragActive(true);
    } else if (e.type === 'dragleave') {
      setDragActive(false);
    }
  };

  const handleDrop = (e) => {
    e.preventDefault();
    e.stopPropagation();
    setDragActive(false);
    
    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      handleFileUpload(e.dataTransfer.files);
    }
  };

  const handleFileInputChange = (e) => {
    if (e.target.files && e.target.files[0]) {
      handleFileUpload(e.target.files);
    }
  };

  const deleteDocument = async (documentId) => {
    if (!window.confirm('Are you sure you want to delete this document?')) return;

    setDeletingDocument(documentId);
    try {
      const user = JSON.parse(localStorage.getItem('user'));
      if (!user || !user.email) {
        toast.error('User not authenticated');
        return;
      }
      
      await api.delete(`/api/pdf/documents/${documentId}?userEmail=${encodeURIComponent(user.email)}`);
      setDocuments(prev => prev.filter(doc => doc.id !== documentId));
      toast.success('Document deleted successfully');
    } catch (error) {
      console.error('Error deleting document:', error);
      toast.error('Failed to delete document');
    } finally {
      setDeletingDocument(null);
    }
  };

  const formatFileSize = (bytes) => {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  };

  const formatDate = (dateString) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  // Show loading if user is not admin
  if (!user || user.role !== 'ADMIN') {
    return (
      <div className={`min-h-screen flex items-center justify-center ${
        isDarkMode 
          ? 'bg-gradient-to-br from-gray-900 via-gray-800 to-gray-700' 
          : 'bg-gradient-to-br from-slate-50 via-blue-50 to-indigo-100'
      }`}>
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-500 mx-auto mb-4"></div>
          <p className={`${isDarkMode ? 'text-gray-300' : 'text-gray-600'}`}>
            Redirecting to admin page...
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
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z" />
            </svg>
          </div>
          <h1 className={`text-3xl font-bold mb-2 ${
            isDarkMode 
              ? 'text-white' 
              : 'bg-gradient-to-r from-gray-900 to-gray-700 bg-clip-text text-transparent'
          }`}>PDF Document Management</h1>
          <p className={isDarkMode ? 'text-gray-300' : 'text-gray-600'}>
            Upload and manage your HR documents for AI-powered search and assistance
          </p>
        </div>

        {/* Upload Area */}
        <div className="mb-8">
          <div className={`rounded-2xl shadow-lg p-6 ${
            isDarkMode 
              ? 'bg-gray-800/90 backdrop-blur-xl border border-gray-700' 
              : 'bg-white/80 backdrop-blur-xl border border-white/20'
          }`}>
            <h2 className={`text-lg font-semibold mb-4 flex items-center ${
              isDarkMode ? 'text-white' : 'text-gray-900'
            }`}>
              <svg className="w-5 h-5 mr-2 text-blue-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12" />
              </svg>
              Upload Documents
            </h2>
            <div
              className={`relative border-2 border-dashed rounded-xl p-8 text-center transition-all duration-200 ${
                dragActive
                  ? 'border-blue-500 bg-blue-50/50'
                  : isDarkMode
                    ? 'border-gray-600 hover:border-gray-500 bg-gray-700/50'
                    : 'border-gray-300 hover:border-gray-400 bg-gray-50/50'
              }`}
              onDragEnter={handleDrag}
              onDragLeave={handleDrag}
              onDragOver={handleDrag}
              onDrop={handleDrop}
            >
            <input
              ref={fileInputRef}
              type="file"
              multiple
              accept=".pdf"
              onChange={handleFileInputChange}
              className="absolute inset-0 w-full h-full opacity-0 cursor-pointer"
              disabled={uploading}
            />
            
              <div className="space-y-4">
                <div className={`mx-auto w-16 h-16 rounded-full flex items-center justify-center ${
                  isDarkMode ? 'bg-blue-900/30' : 'bg-blue-100'
                }`}>
                  <svg className="w-8 h-8 text-blue-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12" />
                  </svg>
                </div>
                
                <div>
                  <p className={`text-lg font-medium ${
                    isDarkMode ? 'text-white' : 'text-gray-900'
                  }`}>
                    {uploading ? 'Uploading documents...' : 'Drop PDF files here or click to browse'}
                  </p>
                  <p className={`text-sm mt-1 ${
                    isDarkMode ? 'text-gray-400' : 'text-gray-500'
                  }`}>
                    Support for multiple files • Max 10MB per file
                  </p>
                </div>
                
                {uploading && (
                  <div className="flex items-center justify-center space-x-2">
                    <div className="w-4 h-4 border-2 border-blue-600 border-t-transparent rounded-full animate-spin"></div>
                    <span className="text-sm text-blue-600">Processing documents...</span>
                  </div>
                )}
              </div>
            </div>
          </div>
        </div>

        {/* Documents List */}
        <div>
          <div className={`rounded-2xl shadow-lg p-6 ${
            isDarkMode 
              ? 'bg-gray-800/90 backdrop-blur-xl border border-gray-700' 
              : 'bg-white/80 backdrop-blur-xl border border-white/20'
          }`}>
            <h2 className={`text-lg font-semibold mb-4 flex items-center ${
              isDarkMode ? 'text-white' : 'text-gray-900'
            }`}>
              <svg className="w-5 h-5 mr-2 text-blue-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
              </svg>
              Your Documents
            </h2>
            <p className={`text-sm mb-6 ${
              isDarkMode ? 'text-gray-300' : 'text-gray-600'
            }`}>
              {documents.length} document(s) uploaded • Manage your PDF files for AI-powered search
            </p>

            {loading ? (
              <div className="space-y-4">
                {[...Array(3)].map((_, i) => (
                  <div key={i} className="animate-pulse">
                    <div className={`flex items-center p-4 rounded-lg ${
                      isDarkMode ? 'bg-gray-700/50' : 'bg-gray-50'
                    }`}>
                      <div className={`w-12 h-12 rounded-lg ${
                        isDarkMode ? 'bg-gray-600' : 'bg-gray-200'
                      }`}></div>
                      <div className="flex-1 ml-4">
                        <div className={`h-4 rounded w-3/4 mb-2 ${
                          isDarkMode ? 'bg-gray-600' : 'bg-gray-200'
                        }`}></div>
                        <div className={`h-3 rounded w-1/2 ${
                          isDarkMode ? 'bg-gray-600' : 'bg-gray-200'
                        }`}></div>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            ) : documents.length === 0 ? (
              <div className="text-center py-12">
                <div className={`w-16 h-16 mx-auto mb-4 rounded-2xl flex items-center justify-center ${
                  isDarkMode ? 'bg-gray-700' : 'bg-gray-100'
                }`}>
                  <svg className="w-8 h-8 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                  </svg>
                </div>
                <h3 className={`text-lg font-medium mb-2 ${
                  isDarkMode ? 'text-white' : 'text-gray-900'
                }`}>No documents yet</h3>
                <p className={isDarkMode ? 'text-gray-400' : 'text-gray-500'}>Upload your first PDF to get started with AI-powered search</p>
              </div>
            ) : (
              <div className="space-y-3">
                {documents.map((document, index) => (
                  <div
                    key={document.id}
                    className={`flex items-center p-4 rounded-lg transition-all duration-200 ${
                      isDarkMode 
                        ? 'bg-gray-700/50 hover:bg-gray-700/80' 
                        : 'bg-gray-50 hover:bg-gray-100'
                    }`}
                  >
                    <div className="flex items-center space-x-4">
                      <div className={`w-12 h-12 rounded-lg flex items-center justify-center ${
                        isDarkMode ? 'bg-red-900/30' : 'bg-red-100'
                      }`}>
                        <svg className="w-6 h-6 text-red-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z" />
                        </svg>
                      </div>
                      <div className="flex-1 min-w-0">
                        <h3 className={`text-sm font-medium truncate ${
                          isDarkMode ? 'text-white' : 'text-gray-900'
                        }`}>
                          {document.fileName}
                        </h3>
                        <div className="flex items-center space-x-4 mt-1">
                          <p className={`text-xs ${
                            isDarkMode ? 'text-gray-400' : 'text-gray-500'
                          }`}>
                            {formatFileSize(document.fileSize)}
                          </p>
                          <p className={`text-xs ${
                            isDarkMode ? 'text-gray-400' : 'text-gray-500'
                          }`}>
                            {formatDate(document.createdAt)}
                          </p>
                          <div className="flex items-center space-x-1">
                            <div className={`w-2 h-2 rounded-full ${
                              document.status === 'INDEXED' ? 'bg-green-400' : 
                              document.status === 'FAILED' ? 'bg-red-400' : 'bg-yellow-400'
                            }`}></div>
                            <span className={`text-xs ${
                              isDarkMode ? 'text-gray-400' : 'text-gray-500'
                            }`}>
                              {document.status === 'INDEXED' ? 'Indexed' : 
                               document.status === 'FAILED' ? 'Failed' : 'Processing...'}
                            </span>
                          </div>
                        </div>
                        {document.summary && (
                          <p className={`text-xs mt-2 line-clamp-2 ${
                            isDarkMode ? 'text-gray-300' : 'text-gray-600'
                          }`}>
                            {document.summary}
                          </p>
                        )}
                      </div>
                    </div>
                    
                    <button
                      onClick={() => deleteDocument(document.id)}
                      disabled={document.status === 'PROCESSING' || deletingDocument === document.id}
                      className={`p-2 rounded-lg transition-colors duration-200 ${
                        document.status === 'PROCESSING' || deletingDocument === document.id
                          ? 'opacity-50 cursor-not-allowed'
                          : isDarkMode
                            ? 'text-gray-400 hover:text-red-400 hover:bg-red-900/20'
                            : 'text-gray-400 hover:text-red-600 hover:bg-red-50'
                      }`}
                      title={
                        document.status === 'PROCESSING' 
                          ? 'Cannot delete while processing' 
                          : deletingDocument === document.id 
                          ? 'Deleting...' 
                          : 'Delete document'
                      }
                    >
                      {deletingDocument === document.id ? (
                        <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-red-500"></div>
                      ) : (
                        <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                        </svg>
                      )}
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default PdfUploadPage;
