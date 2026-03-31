import { themes } from '@/features/theme/themes';
import { Theme } from '@/features/theme/theme.type';

export interface ThemeState {
  themeId: Theme;
  theme: (typeof themes)[Theme];
  setTheme: (id: Theme) => void;
}
