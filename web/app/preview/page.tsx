'use client';

import { themes, useTheme } from '@/theme/theme-provider';
import { Theme } from '@/types/theme.type';

export default function ThemesPage() {
  const { themeId, theme, setTheme } = useTheme();

  return (
    <div className={`min-h-screen bg-linear-to-br ${theme.bg} text-slate-100`}>
      <div className="mx-auto max-w-5xl px-4 py-10">
        <section className="mb-6">
          <h1 className="text-2xl font-semibold">Website theme preview</h1>
          <p className="mt-1 text-sm text-slate-300">
            Switch between visual packs to see how ChatWolf feels in different
            worlds.
          </p>
        </section>

        <section className="mb-6 flex gap-2">
          {(Object.keys(themes) as Theme[]).map(id => (
            <button
              key={id}
              onClick={() => setTheme(id)}
              className={`rounded-full border px-3 py-1 text-xs capitalize ${
                themeId === id
                  ? 'border-white/80 bg-white/10'
                  : 'border-white/20 bg-black/20 text-slate-300 hover:border-white/40'
              }`}
            >
              {themes[id].name}
            </button>
          ))}
        </section>

        <section className="overflow-hidden rounded-3xl border border-white/10 bg-black/40 shadow-2xl">
          <div className="border-b border-white/10 bg-black/50 px-5 py-3">
            <div className="flex items-center justify-between">
              <span className="text-sm font-medium">{theme.name}</span>
              <span className="text-xs text-slate-400">
                {theme.description}
              </span>
            </div>
          </div>
          <div className="grid gap-0 md:grid-cols-[1.2fr,1fr]">
            <div className="border-r border-white/10 bg-linear-to-br from-black/60 to-slate-900/80 p-5">
              <h2 className="text-lg font-semibold">Hero section</h2>
              <p className="mt-2 max-w-md text-sm text-slate-300">
                Build a socket‑powered chat in minutes with ChatWolf’s real‑time
                engine and UI kit.
              </p>
              <div className="mt-4 flex gap-3">
                <button
                  className={`rounded-full bg-linear-to-r ${theme.primary} px-4 py-1.5 text-xs font-medium text-slate-950 shadow-lg`}
                >
                  Start howling
                </button>
                <button className="rounded-full border border-white/20 bg-black/40 px-4 py-1.5 text-xs text-slate-200">
                  View docs
                </button>
              </div>
              <div className="mt-8 rounded-2xl border border-white/10 bg-black/60 p-3 text-xs text-slate-200">
                <div className="mb-2 flex items-center justify-between text-[11px] text-slate-400">
                  <span>Live preview</span>
                  <span>Socket latency: 38ms</span>
                </div>
                <div className="space-y-2">
                  <div className="flex gap-2">
                    <div className="h-6 w-6 rounded-full bg-slate-700" />
                    <div className="flex-1 rounded-2xl bg-slate-800/80 px-3 py-1">
                      <span className="text-[11px] text-slate-200">
                        The pack is online.
                      </span>
                    </div>
                  </div>
                  <div className="flex justify-end gap-2">
                    <div className="flex-1 max-w-[60%] rounded-2xl bg-linear-to-r from-emerald-400 to-cyan-400 px-3 py-1 text-right text-[11px] text-slate-950">
                      Your app, your theme, same socket power.
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <div className="hidden border-l border-white/5 bg-slate-950/60 p-5 md:block">
              <h3 className="text-sm font-medium">Marketing highlights</h3>
              <ul className="mt-3 space-y-2 text-xs text-slate-300">
                <li>• Clean hero with gradient primary CTA.</li>
                <li>• Glassmorphism card mirroring in‑app chat surface.</li>
                <li>• Theme‑aware gradients for on‑brand landing pages.</li>
              </ul>
            </div>
          </div>
        </section>
      </div>
    </div>
  );
}
