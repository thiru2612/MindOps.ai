"use client";

import { motion } from "framer-motion";
import { Cloud, Server, Database, Globe } from "lucide-react";

interface CloudPanelProps {
  provider: "aws" | "azure" | "gcp";
  isAnimating: boolean;
  direction: "left" | "center" | "right";
}

const providerConfig = {
  aws: {
    name: "AWS",
    color: "from-orange-500/10 to-orange-600/5",
    borderColor: "border-orange-500/20",
    accentColor: "text-orange-400",
    glowColor: "shadow-orange-500/10",
    icon: Server,
    features: ["EC2", "Lambda", "S3", "RDS"],
  },
  azure: {
    name: "Azure",
    color: "from-blue-500/10 to-blue-600/5",
    borderColor: "border-blue-500/20",
    accentColor: "text-blue-400",
    glowColor: "shadow-blue-500/10",
    icon: Cloud,
    features: ["VMs", "Functions", "Blob", "SQL"],
  },
  gcp: {
    name: "Azure",
    color: "from-blue-500/10 to-blue-600/5",
    borderColor: "border-blue-500/20",
    accentColor: "text-blue-400",
    glowColor: "shadow-blue-500/10",
    icon: Globe,
    features: ["Virtual Machines", "App Service", "Blob Storage", "Azure SQL"],
  },
};

export function CloudPanel({ provider, isAnimating, direction }: CloudPanelProps) {
  const config = providerConfig[provider];
  const Icon = config.icon;

  const getExitX = () => {
    switch (direction) {
      case "left":
        return "-120%";
      case "right":
        return "120%";
      case "center":
        return "0%";
    }
  };

  return (
    <motion.div
      initial={{ x: "0%", opacity: 1 }}
      animate={{
        x: isAnimating ? getExitX() : "0%",
        opacity: isAnimating ? 0 : 1,
      }}
      transition={{
        duration: 0.8,
        ease: [0.22, 1, 0.36, 1],
        delay: direction === "center" ? 0.1 : 0,
      }}
      className={`relative h-full w-full rounded-2xl bg-gradient-to-b ${config.color} backdrop-blur-xl border ${config.borderColor} shadow-2xl ${config.glowColor} overflow-hidden`}
    >
      {/* Frosted glass effect overlay */}
      <div className="absolute inset-0 bg-slate-950/40 backdrop-blur-sm" />
      
      {/* Subtle grid pattern */}
      <div 
        className="absolute inset-0 opacity-[0.03]"
        style={{
          backgroundImage: `linear-gradient(rgba(255,255,255,0.1) 1px, transparent 1px), linear-gradient(90deg, rgba(255,255,255,0.1) 1px, transparent 1px)`,
          backgroundSize: '24px 24px',
        }}
      />

      {/* Content */}
      <div className="relative z-10 flex flex-col h-full p-6">
        {/* Header */}
        <div className="flex items-center gap-3 mb-6">
          <div className={`p-2 rounded-lg bg-slate-800/50 ${config.accentColor}`}>
            <Icon className="w-5 h-5" />
          </div>
          <span className={`text-sm font-medium tracking-wide ${config.accentColor}`}>
            {config.name}
          </span>
        </div>

        {/* Features list */}
        <div className="space-y-3 flex-1">
          {config.features.map((feature, index) => (
            <motion.div
              key={feature}
              initial={{ opacity: 0, x: -10 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ delay: 0.2 + index * 0.1 }}
              className="flex items-center gap-2"
            >
              <div className={`w-1.5 h-1.5 rounded-full ${config.accentColor} bg-current opacity-60`} />
              <span className="text-xs text-slate-400 font-mono">{feature}</span>
            </motion.div>
          ))}
        </div>

        {/* Status indicator */}
        <div className="flex items-center gap-2 pt-4 border-t border-slate-700/50">
          <div className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse" />
          <span className="text-xs text-slate-500">Connected</span>
        </div>
      </div>

      {/* Decorative elements */}
      <div className={`absolute -bottom-20 -right-20 w-40 h-40 rounded-full bg-gradient-to-br ${config.color} blur-3xl opacity-50`} />
    </motion.div>
  );
}
