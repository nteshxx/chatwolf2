'use client';

import { FloatingLogo } from '@/components/home/FloatingLogo';
import ThemeToggle from '@/components/theme/ThemeToggle';
import { LoginButton } from '@/components/home/LoginButton';
import { Hero } from '@/components/home/Hero';
import { useThemeStore } from '@/features/theme/theme.store';

const Home = () => {
  const theme = useThemeStore(state => state.theme);

  return (
    <div
      className={`min-h-screen bg-linear-to-br ${theme['--color-background']}`}
    >
      <ThemeToggle />
      <div className="grid grid-cols-2 h-screen max-w-5xl:grid-cols-1">
        <div
          className="flex flex-col justify-center items-start pl-48
            max-w-lg:px-12 max-w-lg:items-center max-w-sm:px-4"
        >
          <Hero />
          <LoginButton />
        </div>
        <div
          className="flex justify-center items-center p-8 relative
         max-[992px]:col-start-1 max-[992px]:row-start-1
         max-[280px]:p-4"
        >
          <FloatingLogo width={300} height={300} />
        </div>
      </div>
    </div>
  );
};

export default Home;
