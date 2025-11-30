import { useThemeStore } from '@/store/theme.store';
import Logo from '@/theme/logo';

export default function Footer() {
  const { theme } = useThemeStore();

  return (
    <div className="flex justify-left content-center p-1">
      <div className="my-auto mx-1">
        <Logo width={40} height={40} />
      </div>
      <div className="px-2">
        <div className={`text-base font-semibold ${theme.textPrimary}`}>
          ChatWolf
        </div>
        <div className={`text-xs ${theme.textPrimary}`}>
          Crafted with ♥️ in India
        </div>
      </div>
    </div>
  );
}
