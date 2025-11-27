'use client';

import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useAuthStore } from '@/store/auth.store';

export default function VerifyEmailForm() {
  const router = useRouter();
  const {
    verifyEmail,
    resendOTP,
    isLoading,
    error,
    clearError,
    pendingVerificationEmail,
  } = useAuthStore();

  const [otp, setOtp] = useState(['', '', '', '', '', '']);
  const [resendTimer, setResendTimer] = useState(60);
  const [canResend, setCanResend] = useState(false);

  useEffect(() => {
    if (!pendingVerificationEmail) {
      router.push('/auth/login');
      return;
    }

    const timer = setInterval(() => {
      setResendTimer(prev => {
        if (prev <= 1) {
          setCanResend(true);
          return 0;
        }
        return prev - 1;
      });
    }, 1000);

    return () => clearInterval(timer);
  }, [pendingVerificationEmail, router]);

  const handleOtpChange = (index: number, value: string) => {
    if (value.length > 1) return;

    const newOtp = [...otp];
    newOtp[index] = value;
    setOtp(newOtp);

    // Auto-focus next input
    if (value && index < 5) {
      const nextInput = document.getElementById(`otp-${index + 1}`);
      nextInput?.focus();
    }
  };

  const handleKeyDown = (index: number, e: React.KeyboardEvent) => {
    if (e.key === 'Backspace' && !otp[index] && index > 0) {
      const prevInput = document.getElementById(`otp-${index - 1}`);
      prevInput?.focus();
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    clearError();

    const otpString = otp.join('');
    if (otpString.length !== 6) {
      return alert('Please enter all 6 digits');
    }

    try {
      await verifyEmail(pendingVerificationEmail!, otpString);
      router.push('/chat');
    } catch (error) {
      console.error('Verification error:', error);
    }
  };

  const handleResend = async () => {
    if (!canResend) return;

    try {
      await resendOTP(pendingVerificationEmail!);
      setResendTimer(60);
      setCanResend(false);
      setOtp(['', '', '', '', '', '']);
    } catch (error) {
      console.error('Resend error:', error);
    }
  };

  return (
    <div className="max-w-md mx-auto p-6 bg-white rounded-lg shadow-md">
      <h2 className="text-2xl font-bold mb-2">Verify Your Email</h2>
      <p className="text-gray-600 mb-6">
        We sent a code to <strong>{pendingVerificationEmail}</strong>
      </p>

      {error && (
        <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded mb-4">
          {error}
        </div>
      )}

      <form onSubmit={handleSubmit} className="space-y-6">
        <div className="flex gap-2 justify-center">
          {otp.map((digit, index) => (
            <input
              key={index}
              id={`otp-${index}`}
              type="text"
              inputMode="numeric"
              maxLength={1}
              value={digit}
              onChange={e => handleOtpChange(index, e.target.value)}
              onKeyDown={e => handleKeyDown(index, e)}
              className="w-12 h-12 text-center text-xl border-2 rounded-md focus:border-blue-500"
              required
            />
          ))}
        </div>

        <button
          type="submit"
          disabled={isLoading}
          className="w-full bg-blue-600 text-white py-2 rounded-md hover:bg-blue-700 disabled:bg-gray-400"
        >
          {isLoading ? 'Verifying...' : 'Verify Email'}
        </button>
      </form>

      <div className="mt-4 text-center text-sm">
        {canResend ? (
          <button
            onClick={handleResend}
            disabled={isLoading}
            className="text-blue-600 hover:underline"
          >
            Resend Code
          </button>
        ) : (
          <span className="text-gray-500">Resend code in {resendTimer}s</span>
        )}
      </div>
    </div>
  );
}
