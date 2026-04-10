"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { motion } from "framer-motion";
import { CloudPanel } from "@/components/cloud-panel";
import { LoginCard } from "@/components/login-card";
import { GridBackground } from "@/components/grid-background";
import { Activity, Shield, Zap, Layers } from "lucide-react";

export default function LoginPage() {
  const [isAnimating, setIsAnimating] = useState(false);
  const router = useRouter();

  const handleSignIn = () => {
    setIsAnimating(true);
    // Navigate to dashboard after animation completes
    setTimeout(() => {
      router.push("/dashboard");
    }, 800);
  };

  return (
    <main className="relative min-h-screen bg-slate-950 overflow-hidden">
      {/* Deep dark background with gradient */}
      <div className="absolute inset-0 bg-gradient-to-br from-slate-950 via-slate-900 to-slate-950" />
      
      {/* Subtle noise texture */}
      <div 
        className="absolute inset-0 opacity-[0.015]"
        style={{
          backgroundImage: `url("data:image/svg+xml,%3Csvg viewBox='0 0 256 256' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='noiseFilter'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.9' numOctaves='4' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23noiseFilter)'/%3E%3C/svg%3E")`,
        }}
      />

      {/* Grid background (revealed on sign in) */}
      <GridBackground isVisible={isAnimating} />

      {/* Cloud panels container */}
      <div className="absolute inset-0 flex items-center justify-center">
        <div className="relative w-full max-w-6xl h-[500px] sm:h-[600px] px-4 sm:px-8 flex gap-3 sm:gap-4">
          {/* AWS Panel - Left */}
          <div className="flex-1 hidden sm:block">
            <CloudPanel provider="aws" isAnimating={isAnimating} direction="left" />
          </div>
          
          {/* Azure Panel - Center */}
          <div className="flex-1 hidden sm:block">
            <CloudPanel provider="azure" isAnimating={isAnimating} direction="center" />
          </div>
          
          {/* Azure Panel (Right) - Changed from GCP */}
          <div className="flex-1 hidden sm:block">
            <CloudPanel provider="gcp" isAnimating={isAnimating} direction="right" />
          </div>

          {/* Mobile: Single panel showing all providers */}
          <div className="flex-1 sm:hidden">
            <motion.div
              initial={{ opacity: 1 }}
              animate={{ opacity: isAnimating ? 0 : 1 }}
              transition={{ duration: 0.5 }}
              className="h-full rounded-2xl bg-gradient-to-b from-slate-800/30 to-slate-900/30 backdrop-blur-xl border border-slate-700/30 overflow-hidden"
            >
              <div className="p-6 h-full flex flex-col justify-center items-center gap-6">
                <div className="flex items-center gap-4">
                  <div className="p-2 rounded-lg bg-orange-500/10 border border-orange-500/20">
                    <Activity className="w-5 h-5 text-orange-400" />
                  </div>
                  <div className="p-2 rounded-lg bg-blue-500/10 border border-blue-500/20">
                    <Shield className="w-5 h-5 text-blue-400" />
                  </div>
                  <div className="p-2 rounded-lg bg-blue-500/10 border border-blue-500/20">
                    <Zap className="w-5 h-5 text-blue-400" />
                  </div>
                </div>
                <p className="text-sm text-slate-400 text-center">
                  Multi-cloud orchestration
                </p>
              </div>
            </motion.div>
          </div>
        </div>
      </div>

      {/* Login card overlay */}
      <div className="absolute inset-0 flex items-center justify-center px-4 z-20">
        <LoginCard onSignIn={handleSignIn} isAnimating={isAnimating} />
      </div>

      {/* Status bar */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.5 }}
        className="absolute bottom-6 left-1/2 -translate-x-1/2 flex items-center gap-6 text-xs text-slate-500"
      >
        <div className="flex items-center gap-2">
          <div className="w-1.5 h-1.5 rounded-full bg-emerald-500" />
          <span>All systems operational</span>
        </div>
        <span className="hidden sm:inline">|</span>
        <span className="hidden sm:inline">v2.4.1</span>
        <span className="hidden sm:inline">|</span>
        <span className="hidden sm:inline">us-east-1</span>
      </motion.div>

      {/* Top navigation hint */}
      <motion.nav
        initial={{ opacity: 0, y: -10 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.3 }}
        className="absolute top-0 left-0 right-0 px-6 py-4 flex items-center justify-between"
      >
        <div className="flex items-center gap-2">
          <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-slate-700 to-slate-800 border border-slate-600/50 flex items-center justify-center">
            <Layers className="w-4 h-4 text-slate-300" />
          </div>
          <span className="text-sm font-medium text-slate-300">MindOps</span>
        </div>
        
        <div className="flex items-center gap-4">
          <a href="#" className="text-sm text-slate-500 hover:text-slate-300 transition-colors">
            Docs
          </a>
          <a href="#" className="text-sm text-slate-500 hover:text-slate-300 transition-colors">
            Status
          </a>
          <a href="#" className="text-sm text-slate-500 hover:text-slate-300 transition-colors hidden sm:inline">
            Support
          </a>
        </div>
      </motion.nav>
    </main>
  );
}
