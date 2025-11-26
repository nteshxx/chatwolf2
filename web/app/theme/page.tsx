'use client';
import { useThemeStore } from '@/store/theme.store';
import { themes } from '@/theme/themes';
import { Theme } from '@/types/theme.type';

export default function ThemesPage() {
  const { themeId, theme, setTheme } = useThemeStore();

  return (
    <div className={`min-h-screen bg-linear-to-br ${theme.bg} ${theme.textPrimary}`}>
      <div className="mx-auto max-w-7xl px-4 py-10">
        {/* Header */}
        <section className="mb-8">
          <h1 className="text-4xl font-bold">Theme Showcase</h1>
          <p className={`mt-2 ${theme.textSecondary}`}>
            Explore the complete UI kit for {theme.name}. Switch themes to see how each element adapts.
          </p>
        </section>

        {/* Theme Selector */}
        <section className="mb-8 flex flex-wrap gap-3">
          {(Object.keys(themes) as Theme[]).map(id => (
            <button
              key={id}
              onClick={() => setTheme(id)}
              className={`rounded-lg px-4 py-2 text-sm font-medium transition-all ${
                themeId === id
                  ? theme.button.primary
                  : theme.button.secondary
              }`}
            >
              {themes[id].name}
            </button>
          ))}
        </section>

        <div className="grid gap-6 lg:grid-cols-2">
          {/* Buttons Section */}
          <section className={`rounded-2xl ${theme.glass} p-6`}>
            <h2 className="mb-4 text-xl font-semibold">Buttons</h2>
            <div className="space-y-4">
              <div>
                <p className={`mb-2 text-sm ${theme.textSecondary}`}>Primary Button</p>
                <button className={`rounded-lg px-6 py-2.5 font-medium transition-all ${theme.button.primary}`}>
                  Start Howling
                </button>
              </div>
              <div>
                <p className={`mb-2 text-sm ${theme.textSecondary}`}>Secondary Button</p>
                <button className={`rounded-lg px-6 py-2.5 font-medium transition-all ${theme.button.secondary}`}>
                  Learn More
                </button>
              </div>
              <div>
                <p className={`mb-2 text-sm ${theme.textSecondary}`}>Ghost Button</p>
                <button className={`rounded-lg px-6 py-2.5 font-medium transition-all ${theme.button.ghost}`}>
                  View Details
                </button>
              </div>
              <div>
                <p className={`mb-2 text-sm ${theme.textSecondary}`}>Glass Button</p>
                <button className={`rounded-lg px-6 py-2.5 font-medium transition-all ${theme.button.glass}`}>
                  Glass Effect
                </button>
              </div>
            </div>
          </section>

          {/* Typography Section */}
          <section className={`rounded-2xl ${theme.glass} p-6`}>
            <h2 className="mb-4 text-xl font-semibold">Typography</h2>
            <div className="space-y-4">
              <div>
                <p className={`mb-2 text-sm ${theme.textSecondary}`}>Primary Text</p>
                <p className={theme.textPrimary}>
                  This is primary text with full contrast and readability.
                </p>
              </div>
              <div>
                <p className={`mb-2 text-sm ${theme.textSecondary}`}>Secondary Text</p>
                <p className={theme.textSecondary}>
                  Secondary text provides subtle emphasis for descriptions.
                </p>
              </div>
              <div>
                <p className={`mb-2 text-sm ${theme.textSecondary}`}>Muted Text</p>
                <p className={theme.textMuted}>
                  Muted text for minimal emphasis or disabled states.
                </p>
              </div>
              <div>
                <p className={`mb-2 text-sm ${theme.textSecondary}`}>Gradient Text</p>
                <p className={`bg-linear-to-r ${theme.primary} bg-clip-text text-2xl font-bold text-transparent`}>
                  {theme.name}
                </p>
              </div>
            </div>
          </section>

          {/* Tabs Section */}
          <section className={`rounded-2xl ${theme.glass} p-6`}>
            <h2 className="mb-4 text-xl font-semibold">Tabs</h2>
            <div className={`${theme.tabs.container}`}>
              <div className="flex gap-1">
                <button className={`rounded-t-lg px-4 py-2 text-sm font-medium transition-all ${theme.tabs.active}`}>
                  Active Tab
                </button>
                <button className={`rounded-t-lg px-4 py-2 text-sm font-medium transition-all ${theme.tabs.inactive}`}>
                  Inactive Tab
                </button>
                <button className={`rounded-t-lg px-4 py-2 text-sm font-medium transition-all ${theme.tabs.inactive}`}>
                  Another Tab
                </button>
              </div>
            </div>
            <div className={`mt-4 rounded-lg ${theme.bgCard} p-4`}>
              <p className={theme.textSecondary}>
                Tab content goes here. This demonstrates the active tab state.
              </p>
            </div>
          </section>

          {/* Input Fields Section */}
          <section className={`rounded-2xl ${theme.glass} p-6`}>
            <h2 className="mb-4 text-xl font-semibold">Input Fields</h2>
            <div className="space-y-4">
              <div>
                <label className={`mb-2 block text-sm font-medium ${theme.textPrimary}`}>
                  Username
                </label>
                <input
                  type="text"
                  placeholder="Enter your username"
                  className={`w-full rounded-lg px-4 py-2.5 outline-none transition-all ${theme.input}`}
                />
              </div>
              <div>
                <label className={`mb-2 block text-sm font-medium ${theme.textPrimary}`}>
                  Email
                </label>
                <input
                  type="email"
                  placeholder="your@email.com"
                  className={`w-full rounded-lg px-4 py-2.5 outline-none transition-all ${theme.input}`}
                />
              </div>
              <div>
                <label className={`mb-2 block text-sm font-medium ${theme.textPrimary}`}>
                  Message
                </label>
                <textarea
                  placeholder="Type your message..."
                  rows={3}
                  className={`w-full rounded-lg px-4 py-2.5 outline-none transition-all ${theme.input}`}
                />
              </div>
            </div>
          </section>

          {/* Cards Section */}
          <section className={`rounded-2xl ${theme.glass} p-6 lg:col-span-2`}>
            <h2 className="mb-4 text-xl font-semibold">Card Variations</h2>
            <div className="grid gap-4 md:grid-cols-3">
              <div className={`rounded-xl ${theme.bgCard} ${theme.border} border p-4`}>
                <h3 className={`mb-2 font-semibold ${theme.textPrimary}`}>Standard Card</h3>
                <p className={`text-sm ${theme.textSecondary}`}>
                  Basic card with background and border styling.
                </p>
              </div>
              <div className={`rounded-xl ${theme.glass} p-4 ${theme.glassHover} transition-all cursor-pointer`}>
                <h3 className={`mb-2 font-semibold ${theme.textPrimary}`}>Glass Card</h3>
                <p className={`text-sm ${theme.textSecondary}`}>
                  Glassmorphism effect with backdrop blur.
                </p>
              </div>
              <div className={`rounded-xl bg-linear-to-br ${theme.primary} p-4`}>
                <h3 className="mb-2 font-semibold text-white">Gradient Card</h3>
                <p className="text-sm text-white/90">
                  Card with primary gradient background.
                </p>
              </div>
            </div>
          </section>

          {/* Chat Preview Section */}
          <section className={`rounded-2xl ${theme.glass} p-6 lg:col-span-2`}>
            <h2 className="mb-4 text-xl font-semibold">Chat Interface Preview</h2>
            <div className={`rounded-xl ${theme.bgCard} ${theme.border} border p-4`}>
              <div className="space-y-3">
                <div className="flex items-start gap-3">
                  <div className={`h-8 w-8 rounded-full ${theme.primarySolid}`} />
                  <div className={`max-w-xs rounded-2xl ${theme.glass} px-4 py-2`}>
                    <p className={`text-sm ${theme.textPrimary}`}>
                      Hey! Check out this new theme system.
                    </p>
                  </div>
                </div>
                <div className="flex items-start justify-end gap-3">
                  <div className={`max-w-xs rounded-2xl bg-linear-to-r ${theme.primary} px-4 py-2`}>
                    <p className="text-sm text-white">
                      Looks amazing! The glassmorphism is perfect.
                    </p>
                  </div>
                  <div className={`h-8 w-8 rounded-full ${theme.bgSolid}`} />
                </div>
                <div className="flex items-start gap-3">
                  <div className={`h-8 w-8 rounded-full ${theme.primarySolid}`} />
                  <div className={`max-w-xs rounded-2xl ${theme.glass} px-4 py-2`}>
                    <p className={`text-sm ${theme.textPrimary}`}>
                      All components adapt to the theme automatically! 🎨
                    </p>
                  </div>
                </div>
              </div>
              <div className="mt-4 flex gap-2">
                <input
                  type="text"
                  placeholder="Type a message..."
                  className={`flex-1 rounded-full px-4 py-2 text-sm outline-none ${theme.input}`}
                />
                <button className={`rounded-full px-6 py-2 text-sm font-medium ${theme.button.primary}`}>
                  Send
                </button>
              </div>
            </div>
          </section>

          {/* Borders & Dividers */}
          <section className={`rounded-2xl ${theme.glass} p-6`}>
            <h2 className="mb-4 text-xl font-semibold">Borders & Dividers</h2>
            <div className="space-y-4">
              <div className={`rounded-lg border ${theme.border} p-3`}>
                <p className={`text-sm ${theme.textSecondary}`}>Standard border</p>
              </div>
              <div className={`h-px ${theme.border} border-t`} />
              <div className={`rounded-lg border ${theme.border} ${theme.borderHover} p-3 transition-all cursor-pointer`}>
                <p className={`text-sm ${theme.textSecondary}`}>Hover to see border change</p>
              </div>
            </div>
          </section>

          {/* States & Interactions */}
          <section className={`rounded-2xl ${theme.glass} p-6`}>
            <h2 className="mb-4 text-xl font-semibold">Interactive States</h2>
            <div className="space-y-3">
              <button className={`w-full rounded-lg px-4 py-2.5 text-left text-sm transition-all ${theme.button.ghost}`}>
                Hover me to see the effect
              </button>
              <button className={`w-full rounded-lg px-4 py-2.5 text-left text-sm transition-all ${theme.glassHover} ${theme.glass}`}>
                Glass hover effect
              </button>
              <div className={`rounded-lg ${theme.bgCard} p-3 transition-all hover:scale-[1.02]`}>
                <p className={`text-sm ${theme.textSecondary}`}>Hover to scale</p>
              </div>
            </div>
          </section>
        </div>

        {/* Theme Info Footer */}
        <section className={`mt-8 rounded-2xl ${theme.glass} p-6`}>
          <div className="grid gap-4 md:grid-cols-3">
            <div>
              <h3 className={`mb-2 font-semibold ${theme.textPrimary}`}>Current Theme</h3>
              <p className={`text-sm ${theme.textSecondary}`}>{theme.name}</p>
            </div>
            <div>
              <h3 className={`mb-2 font-semibold ${theme.textPrimary}`}>Description</h3>
              <p className={`text-sm ${theme.textSecondary}`}>{theme.description}</p>
            </div>
            <div>
              <h3 className={`mb-2 font-semibold ${theme.textPrimary}`}>Theme ID</h3>
              <code className={`rounded bg-linear-to-r ${theme.primary} px-2 py-1 text-xs text-white`}>
                {theme.id}
              </code>
            </div>
          </div>
        </section>
      </div>
    </div>
  );
}