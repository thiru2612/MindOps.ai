"use client";

import { motion } from "framer-motion";

interface GridBackgroundProps {
  isVisible: boolean;
}

export function GridBackground({ isVisible }: GridBackgroundProps) {
  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: isVisible ? 1 : 0 }}
      transition={{ duration: 1, delay: 0.3 }}
      className="absolute inset-0 overflow-hidden"
    >
      {/* Main grid */}
      <div
        className="absolute inset-0"
        style={{
          backgroundImage: `
            linear-gradient(rgba(148, 163, 184, 0.03) 1px, transparent 1px),
            linear-gradient(90deg, rgba(148, 163, 184, 0.03) 1px, transparent 1px)
          `,
          backgroundSize: "60px 60px",
        }}
      />

      {/* Smaller grid overlay */}
      <div
        className="absolute inset-0"
        style={{
          backgroundImage: `
            linear-gradient(rgba(148, 163, 184, 0.015) 1px, transparent 1px),
            linear-gradient(90deg, rgba(148, 163, 184, 0.015) 1px, transparent 1px)
          `,
          backgroundSize: "15px 15px",
        }}
      />

      {/* Radial gradient fade */}
      <div className="absolute inset-0 bg-gradient-radial from-transparent via-slate-950/50 to-slate-950" />

      {/* Animated glow orbs */}
      <motion.div
        animate={{
          scale: [1, 1.2, 1],
          opacity: [0.3, 0.5, 0.3],
        }}
        transition={{
          duration: 4,
          repeat: Infinity,
          ease: "easeInOut",
        }}
        className="absolute top-1/4 left-1/4 w-96 h-96 bg-blue-500/5 rounded-full blur-3xl"
      />
      
      <motion.div
        animate={{
          scale: [1.2, 1, 1.2],
          opacity: [0.2, 0.4, 0.2],
        }}
        transition={{
          duration: 5,
          repeat: Infinity,
          ease: "easeInOut",
        }}
        className="absolute bottom-1/4 right-1/4 w-96 h-96 bg-orange-500/5 rounded-full blur-3xl"
      />

      {/* Center console text */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: isVisible ? 1 : 0, y: isVisible ? 0 : 20 }}
        transition={{ duration: 0.6, delay: 0.8 }}
        className="absolute inset-0 flex items-center justify-center"
      >
        <div className="text-center">
          <h2 className="text-4xl sm:text-5xl font-bold text-slate-200 mb-4 tracking-tight">
            Welcome to MindOps
          </h2>
          <p className="text-lg text-slate-400 mb-8">
            Your unified cloud orchestration platform
          </p>
          <div className="flex items-center justify-center gap-2">
            <div className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse" />
            <span className="text-sm text-emerald-400 font-medium">
              Initializing console...
            </span>
          </div>
        </div>
      </motion.div>
    </motion.div>
  );
}
