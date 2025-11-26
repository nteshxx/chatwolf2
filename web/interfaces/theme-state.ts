import { themes } from "@/theme/themes";
import { Theme } from "@/types/theme.type"

export interface ThemeState {
  themeId: Theme;
  theme: (typeof themes)[Theme];
  setTheme: (id: Theme) => void;
}