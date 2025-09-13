import React from 'react';

const LoadingSpinner = ({ size = 'large', text = 'Loading...' }) => {
  const sizeClasses = {
    small: 'w-4 h-4',
    medium: 'w-8 h-8',
    large: 'w-12 h-12'
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50">
      <div className="text-center">
        <div className={`${sizeClasses[size]} animate-spin rounded-full border-4 border-gray-300 border-t-primary-600 mx-auto mb-4`}></div>
        <p className="text-gray-600 text-lg">{text}</p>
      </div>
    </div>
  );
};

export default LoadingSpinner;
