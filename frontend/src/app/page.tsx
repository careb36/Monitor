'use client';

import { useMonitor } from '@/hooks/useMonitor';
import { Header } from '@/components/dashboard/Header';
import { Sidebar } from '@/components/dashboard/Sidebar';
import { LogViewer } from '@/components/dashboard/LogViewer';
import { InfraPanel } from '@/components/dashboard/InfraPanel';

export default function DashboardPage() {
  const { state, active, start, stop } = useMonitor();

  return (
    <>
      {/* Start overlay – required to initialise AudioContext from a user gesture */}
      {!active && (
        <div className="fixed inset-0 flex flex-col items-center justify-center bg-black/90 backdrop-blur-sm gap-8 z-50">
          <div className="text-center space-y-4">
            <h1 className="text-4xl md:text-6xl font-bold text-status-info tracking-widest uppercase shadow-status-info drop-shadow-[0_0_15px_rgba(64,196,255,0.5)]">
              Monitor Dashboard
            </h1>
            <p className="text-lg text-text-muted font-mono tracking-wide">
              Panel de monitorización de operaciones en tiempo real
            </p>
          </div>
          <button 
            className="px-8 py-4 border-2 border-status-ok text-status-ok text-lg font-bold tracking-widest uppercase rounded hover:bg-status-ok hover:text-black transition-all hover:shadow-[0_0_20px_var(--color-status-ok)]"
            onClick={start}
          >
            ▶ Iniciar Monitorización
          </button>
        </div>
      )}

      {/* Main Dashboard Layout */}
      <div className="flex flex-col h-screen overflow-hidden bg-bg-base text-text-primary selection:bg-status-info selection:text-black">
        <Header connected={state.connected} active={active} onStop={stop} />
        
        <div className="flex flex-1 overflow-hidden">
          <Sidebar state={state} />
          
          <main className="flex flex-1 overflow-hidden">
            <LogViewer logs={state.logs} />
            <InfraPanel infrastructure={state.infrastructure} />
          </main>
        </div>
      </div>
    </>
  );
}
