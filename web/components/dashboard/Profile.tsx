'use client';

import Image from 'next/image';
import { useAuthStore } from '@/store/auth.store';
import userImage from '@/public/avatars/male.svg';
import { ChangeEvent } from 'react';
import { useThemeStore } from '@/store/theme.store';

const Profile = () => {
  const { user, token } = useAuthStore();
  const { theme } = useThemeStore();

  const onUpdateAvatar = () => {
    const input = document.getElementById(
      'upload-avatar'
    ) as HTMLInputElement | null;
    input?.click();
  };

  const handleAvatarChange = (e: ChangeEvent<HTMLInputElement>) => {
    // TODO: Add upload logic here
    console.log('Avatar file:', e.target.files?.[0]);
  };

  return (
    <div className={`rounded-xl ${theme.border} border p-4`}>
      <div
        className="w-20 h-20 rounded-full m-auto overflow-hidden"
        onClick={onUpdateAvatar}
      >
        <Image
          className="object-fill h-full w-full"
          src={userImage}
          alt="User Avatar"
        />
        <input
          type="file"
          id="upload-avatar"
          name="file"
          accept="image/*"
          hidden
          onChange={handleAvatarChange}
        />
      </div>
      <div className="mt-4 text-center space-y-1">
        <div className={`text-sm font-medium ${theme.textPrimary}`}>
          Nitesh Yadav
        </div>
        <div
          className={`rounded text-xs px-2 py-0.5 inline-block transition-all ${theme.button.secondary}`}
        >
          nteshxx
        </div>
      </div>
    </div>
  );
};

export default Profile;
